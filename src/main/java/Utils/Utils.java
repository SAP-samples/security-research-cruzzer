package Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class Utils {
    public static URL getAbsoluteUrl(URL url, String actionUrl) throws MalformedURLException {
        if (actionUrl.startsWith("http")) {
            return new URL(actionUrl);
        } else if (actionUrl.startsWith("/")) {
            return new URL(url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/" + actionUrl);
        } else {
            return new URL(url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + url.getPath().substring(0, url.getPath().lastIndexOf("/")) + "/" + actionUrl);
        }
    }

    public static double calcSTD(Double[] array) {
        double mean = calcMean(array);

        double standardDeviation = 0.0;
        for (double num : array) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation / array.length);
    }

    public static double chao(HashMap<Double, Integer> coverage, double sum) {
        double estimate;
        double singletons = 0;
        double doubletons = 0;
        for (double num : coverage.values()) {
            if (num == 1) {
                singletons += 1;
            }
            if (num == 2) {
                doubletons += 1;
            }
        }
        if (doubletons <= 0) {
            estimate = sum + (singletons * (singletons - 1)) / 2;
        } else {
            estimate = sum + singletons * singletons / (2 * doubletons);
        }
        return 100 * sum / estimate;
    }

    public static double turing(HashMap<Double, Integer> diff, double sum) {

        return 100 * diff.keySet().size() / sum;
    }

    public static double calcMean(Double[] array) {
        double sum = 0.0;
        for (double i : array) {
            sum += i;
        }
        return sum / array.length;
    }

    public static HashMap<Double, Integer> getSetIntersect(HashMap<Double, Integer> a, HashMap<Double, Integer> b) {
        HashMap<Double, Integer> res = new HashMap<>();
        for (Double hash : b.keySet()) {
            if (a.containsKey(hash)) {
                res.put(hash, b.get(hash) - a.get(hash));
            } else {
                res.put(hash, b.get(hash));
            }
        }
        return res;
    }

    public static HashMap<Double, Integer> getSetDiff(HashMap<Double, Integer> a, HashMap<Double, Integer> b) {
        HashMap<Double, Integer> res = new HashMap<>();
        for (Double hash : b.keySet()) {
            if (!a.containsKey(hash)) {
                res.put(hash, b.get(hash));
            }
        }
        return res;
    }
}
