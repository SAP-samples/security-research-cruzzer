import Crawl.Formular;
import Crawl.PageCrawlController;
import Debugger.Debugger;
import Debugger.DebuggerResult;
import Debugger.JacocoClient.ExecutionDataClient;
import Debugger.XDebugServer.Server;
import FuzzGeneration.FormularFuzzer;
import HTTPClient.ResultType;
import Utils.Command;
import Utils.Match;
import Utils.Utils;
import me.tongfei.progressbar.ProgressBar;
import org.apache.tika.io.IOUtils;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    static HashMap<Formular, FormularFuzzer> fuzzers = new HashMap<>();
    static HashSet<Formular> removedFuzzers = new HashSet<Formular>();
    public static String path = "./results/";
    static final CookieManager cookieManager = new CookieManager();
    static final CookieStore cookieStore = cookieManager.getCookieStore();


    public static void createDirectory(String path) {
        new java.io.File(path).mkdirs();
    }

    public static void main(String[] args) throws Exception {
        Command command = new Command();
        new CommandLine(command).parseArgs(args);
        Debugger client = null;
        List<String> seeds = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        if (command.helpRequested) {
            System.out.println(Command.printHelp());
            System.exit(0);
        }
        if (command.seedFile.isFile()) {
            seeds.addAll(IOUtils.readLines(new FileInputStream(command.seedFile), "UTF-8"));
        }
        if (command.matchFile != null && command.matchFile.isFile()) {
            matches = Match.parse(command.matchFile.getAbsolutePath());
        }
        if (command.programmingLanguage.equalsIgnoreCase("PHP")) {
            client = new Server(command.host, command.port, command.filter);
        } else if (command.programmingLanguage.equalsIgnoreCase("Java")) {
            if (command.classes != null) {
                client = new ExecutionDataClient(command.host, command.port, command.filter, command.classes);
            } else {
                client = new ExecutionDataClient(command.host, command.port, command.filter);
            }
        } else {
            System.out.println("Unknown programming language: " + command.programmingLanguage);
            System.exit(1);
        }
        path += command.startPage
                .replace("http://", "")
                .replace("/", "_")
                .replace(".", "")
                .replace(":", "")
                + command.fuzzes.toString();

        createDirectory(path);
        crawl(command, seeds, matches);
        int crashes = 0;
        ArrayList<FormularFuzzer> fuzcache = new ArrayList<>(fuzzers.values());
        try (ProgressBar pb = new ProgressBar("Fuzzing " + command.startPage, command.fuzzes);
        ) {
            for (int i = 0; i < command.fuzzes; i++) {
                int stepby = 0;
                if (fuzzers.size() <= 0) {
                    System.out.println("no more stuff to fuzz");
                    break;
                }
                int index = i % fuzzers.size();
                FormularFuzzer fuzzer = fuzcache.get(index);

                HashMap<Double, Integer> lastcoverages = new HashMap<>(fuzzer.reachedCoverages);

                int maxk = (int) (Math.random() * 120) + 50;
                for (int k = 0; k < maxk; k++) {
                    try {
                        if (fuzzer.fuzz(client, cookieStore) == ResultType.SERVERERROR) {
                            crashes++;
                        }


                    } catch (Exception e) {
                        //   System.out.println("Failed to fuzz: " + e.getMessage());
                    }
                }
                i += maxk;
                stepby += maxk;


                if (Math.random() > 0.995) {
                    crawl(command, seeds, matches);
                    fuzcache = new ArrayList<>(fuzzers.values());
                }

                if ((Math.round((double) i / 100) * 100) % 1000 == 0) {
                    writeResults(crashes, i);
                }
                //double stopcriteria = Utils.chao(Utils.getSetIntersect(lastcoverages, fuzzer.reachedCoverages), maxk);
                double stopcriteria = Utils.turing(Utils.getSetDiff(lastcoverages, fuzzer.reachedCoverages), maxk);

                //System.out.println(stopcriteria + " " + fuzzer.reachedCoverages.values().stream().reduce(0, Integer::sum));
                if (fuzzer.reachedCoverages.values().stream().reduce(0, Integer::sum) > 5000 && (stopcriteria < 0.1)) {
                    System.out.println("removed a fuzzer with size " + fuzzer.reachedCoverages.values().stream().reduce(0, Integer::sum) + " and its probability " + stopcriteria);
                    removedFuzzers.add(fuzzer.formular);
                    fuzzers.remove(fuzzer.formular);
                    fuzcache = new ArrayList<>(fuzzers.values());
                }

                pb.stepBy(stepby);
            }
        }
        writeResults(crashes, command.fuzzes);
        client.close();
    }

    private static void writeResults(int crashes, int i) throws IOException {
        DebuggerResult r = fuzzers.values().stream().filter(x -> x.bestCoverage != null).collect(Collectors.toList()).get(0).bestCoverage;
        for (FormularFuzzer f : fuzzers.values()) {
            r.merge(f.bestCoverage);
        }
        r.writeToFile(path + "/report_" + i + ".txt", crashes);
    }

    private static void crawl(Command command, List<String> seeds, List<Match> matches) throws Exception {
        List<Formular> formulars = PageCrawlController.
                crawl("./tmp/crawl/", command.startPage, 30, cookieStore).stream().toList();
        initializeMutationFuzzers(seeds, formulars, matches);
    }


    private static void initializeMutationFuzzers(List<String> seeds, List<Formular> formulars, List<Match> matches) {
        for (Formular formular : formulars) {
            if (!fuzzers.containsKey(formular) && !removedFuzzers.contains(formular)) {
                fuzzers.put(formular, new FormularFuzzer(formular, seeds, path, matches));
            }
        }
    }

}
