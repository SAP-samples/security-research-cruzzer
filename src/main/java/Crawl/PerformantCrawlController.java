package Crawl;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;

public class PerformantCrawlController extends CrawlController {
    static final Logger logger = LoggerFactory.getLogger(PerformantCrawlController.class);

    public PerformantCrawlController(CrawlConfig config, PageFetcher pageFetcher, RobotstxtServer robotstxtServer) throws Exception {
        super(config, pageFetcher, robotstxtServer);
    }


    protected <T extends WebCrawler> void start(final CrawlController.WebCrawlerFactory<T> crawlerFactory, int numberOfCrawlers, boolean isBlocking) {
        try {
            this.finished = false;
            this.crawlersLocalData.clear();
            final ArrayList threads = new ArrayList();
            final ArrayList crawlers = new ArrayList();

            for (int i = 1; i <= numberOfCrawlers; ++i) {
                T crawler = crawlerFactory.newInstance();
                Thread thread = new Thread(crawler, "Crawler " + i);
                crawler.setThread(thread);
                crawler.init(i, this);
                thread.start();
                crawlers.add(crawler);
                threads.add(thread);
                logger.info("Crawler {} started", i);
            }

            Thread monitorThread = new Thread(() -> {
                try {
                    synchronized (waitingLock) {
                        WebCrawler crawler;
                        while (true) {
                            boolean someoneIsWorking;
                            do {
                                boolean shut_on_empty;
                                do {
                                    do {
                                        CrawlController.sleep(1);
                                        someoneIsWorking = false;

                                        for (int i = 0; i < threads.size(); ++i) {
                                            Thread threadx = (Thread) threads.get(i);
                                            if (!threadx.isAlive()) {
                                                if (shuttingDown) {
                                                    logger.info("Thread {} was dead, I'll recreate it", i);
                                                    crawler = crawlerFactory.newInstance();
                                                    threadx = new Thread(crawler, "Crawler " + (i + 1));
                                                    threads.remove(i);
                                                    threads.add(i, threadx);
                                                    crawler.setThread(threadx);
                                                    crawler.init(i + 1, PerformantCrawlController.this);
                                                    threadx.start();
                                                    crawlers.remove(i);
                                                    crawlers.add(i, crawler);
                                                }
                                            } else if (((WebCrawler) crawlers.get(i)).isNotWaitingForNewURLs()) {
                                                someoneIsWorking = true;
                                            }
                                        }

                                        shut_on_empty = config.isShutdownOnEmptyQueue();
                                    } while (someoneIsWorking);
                                } while (!shut_on_empty);

                                logger.info("It looks like no thread is working, waiting for 1 seconds to make sure...");
                                CrawlController.sleep(1);
                                someoneIsWorking = false;

                                for (int ix = 0; ix < threads.size(); ++ix) {
                                    Thread thread = (Thread) threads.get(ix);
                                    if (thread.isAlive() && ((WebCrawler) crawlers.get(ix)).isNotWaitingForNewURLs()) {
                                        someoneIsWorking = true;
                                    }
                                }
                            } while (someoneIsWorking);

                            if (shuttingDown) {
                                break;
                            }

                            long queueLength = frontier.getQueueLength();
                            if (queueLength <= 0L) {
                                logger.info("No thread is working and no more URLs are in queue waiting for another 10 seconds to make sure...");
                                CrawlController.sleep(1);
                                queueLength = frontier.getQueueLength();
                                if (queueLength > 0L) {
                                    continue;
                                }
                                break;
                            }
                        }

                        logger.info("All of the crawlers are stopped. Finishing the process...");
                        frontier.finish();
                        Iterator var13 = crawlers.iterator();

                        while (var13.hasNext()) {
                            crawler = (WebCrawler) var13.next();
                            crawler.onBeforeExit();
                            crawlersLocalData.add(crawler.getMyLocalData());
                        }

                        logger.info("Waiting for 1 seconds before final clean up...");
                        CrawlController.sleep(1);
                        frontier.close();
                        docIdServer.close();
                        pageFetcher.shutDown();
                        finished = true;
                        waitingLock.notifyAll();
                        env.close();
                    }
                } catch (Exception var8) {
                    logger.error("Unexpected Error", var8);
                }
            });
            monitorThread.start();
            if (isBlocking) {
                this.waitUntilFinish();
            }
        } catch (Exception var9) {
            logger.error("Error happened", var9);
        }

    }

}
