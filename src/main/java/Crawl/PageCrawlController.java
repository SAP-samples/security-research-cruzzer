package Crawl;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

import java.net.CookieStore;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PageCrawlController {
    public static ConcurrentLinkedQueue<Formular> crawl(String crawlStorageFolder, String url, int numberOfCrawlers, CookieStore cookieStore) throws Exception {
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setFollowRedirects(true);
        config.setIncludeBinaryContentInCrawling(false);
        config.setPolitenessDelay(1);
        config.setUserAgentString("cruzzer");
        config.setConnectionTimeout(10000);
        PageFetcher pageFetcher = new PageFetcherCookie(config, cookieStore);
        WebURL start = new WebURL();
        start.setURL(url);
        PageFetchResult f = pageFetcher.fetchPage(start);
        if (f.getMovedToUrl() != null) {
            start.setURL(f.getMovedToUrl());
            f = pageFetcher.fetchPage(start);
        }
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new PerformantCrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed(url);
        CrawlController.WebCrawlerFactory<PageCrawler> factory = PageCrawler::new;
        controller.start(factory, numberOfCrawlers);
        System.out.println("Crawling finished.");
        controller.shutdown();
        pageFetcher.shutDown();
        return PageCrawler.formulars;
    }
}
