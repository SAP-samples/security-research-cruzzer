package Utils;

import org.apache.tika.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Match {
    public final String name;
    private final String regex;

    public Match(String name, String regex) {
        this.name = name;
        this.regex = regex;
    }

    public boolean matches(String line) {
        return line.matches(regex);
    }

    public static List<Match> parse(String path) {
        ArrayList<Match> matches = new ArrayList<Match>();
        try {
            IOUtils.readLines(new FileInputStream(path), "UTF-8").stream().forEach(line -> {
                String[] split = line.split(":");
                matches.add(new Match(split[0], split[1]));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matches;
    }
}
