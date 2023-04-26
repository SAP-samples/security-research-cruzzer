package Crawl;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class PageCrawler extends WebCrawler {

    public static final ConcurrentLinkedQueue<Formular> formulars = new ConcurrentLinkedQueue();

    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     * In this example, we are instructing the crawler to ignore urls that
     * have css, js, git, ... extensions and to only accept urls that start
     * with "https://www.ics.uci.edu/". In this case, we didn't need the
     * referringPage parameter to make the decision.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        return (href.startsWith("http://localhost") || href.startsWith("https://localhost"));
    }

    /**
     * This function is called when a page is fetched and ready
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        if (page.getParseData() instanceof HtmlParseData htmlParseData) {
            String html = htmlParseData.getHtml();
            try {
                formulars.addAll(parse(html, url));
                formulars.addAll(hasQuery(url));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Formular> hasQuery(String url) throws MalformedURLException {
        List<Formular> formulars = new ArrayList<>();
        if (url.contains("?")) {
            Formular formular = new Formular();
            formular.setMethod("GET");
            formular.setUrl(new URL(url));
            formular.setActionUrl(new URL(url.split("\\?")[0]));
            Arrays.stream(url.split("\\?")[1].split("&")).forEach(field -> {
                FormularField formularField = new FormularField();
                formularField.setFuzzable(true);
                formularField.setType("text");
                String[] splitted = field.split("=");
                formularField.setName(splitted[0]);
                if (splitted.length > 1) {
                    formularField.setValue(splitted[1]);
                }
                formular.getFields().add(formularField);
            });
            formulars.add(formular);
        }
        return formulars;
    }


    private Collection<Formular> parse(String html, String url) throws MalformedURLException {
        Document doc = Jsoup.parse(html);
        Elements form = doc.getElementsByTag("form");
        ArrayList<Formular> formulars = new ArrayList<>();
        for (Element element : form) {
            Formular formular = new Formular();
            formular.setUrl(new URL(url));
            String target = element.attr("action");
            if (target.isEmpty()) {
                target = url;
            }
            formular.setActionUrl(Utils.Utils.getAbsoluteUrl(formular.getUrl(), target));
            formular.setMethod(element.attr("method"));
            Elements inputs = element.getElementsByTag("input");
            inputs.addAll(element.getElementsByTag("textarea"));
            inputs.addAll(element.getElementsByTag("button"));
            inputs.addAll(element.getElementsByTag("submit"));
            inputs.addAll(element.getElementsByTag("select"));
            for (Element input : inputs) {
                FormularField field = new FormularField();
                field.setName(input.attr("name"));
                if (input.attr("type").equals("submit") || input.attr("type").equals("hidden")) {
                    field.setFuzzable(false);
                    field.setValue(input.attr("value"));
                } else if (input.attr("type").equals("checkbox")) {
                    field.setFuzzable(false);
                    field.addValue("on");
                    field.addValue("off");
                } else if (input.tag().toString().equals("select")) {
                    field.setFuzzable(false);
                    field.setValue(input.getElementsByTag("option")
                            .stream()
                            .map(Element::text)
                            .collect(Collectors.toList()));
                } else {
                    field.setValue(input.attr("value"));
                }
                field.setType(input.attr("type"));
                if (field.getName().length() > 0) {
                    formular.getFields().add(field);
                }
            }
            formulars.add(formular);
        }
        return formulars;
    }


}