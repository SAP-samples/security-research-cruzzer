package HTTPClient;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Request {


    public static Connection.Response send(URL actionUrl, Map<String, String> params, Connection.Method method, CookieStore cookieStore) throws IOException {

        String url = actionUrl.toString();
        if (actionUrl.getQuery() != null) {
            url += "&XDEBUG_SESSION=1";
        } else {
            url += "?XDEBUG_SESSION";
        }

        //  long start = System.nanoTime();
        HashMap<String, String> cookies = new HashMap<>();
        for (HttpCookie cookie : cookieStore.getCookies()) {
            cookies.put(cookie.getName(), cookie.getValue());
        }
        Connection.Response r = Jsoup.connect(url)
                .data(params)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .ignoreContentType(true)
                .cookies(cookies)
                .timeout(36000)
                .validateTLSCertificates(false)
                .userAgent("Cruzzer")
                .header("Accept", "*")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .maxBodySize(1000000)
                .method(method)
                .execute();
        //  long diff = (System.nanoTime() - start) / 1000000;
        //  System.out.println(diff + " time needed for request " + method.name());

        return r;
    }
}
