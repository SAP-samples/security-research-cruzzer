package FuzzGeneration;

import Crawl.Formular;
import Crawl.FormularField;
import Debugger.Debugger;
import Debugger.DebuggerResult;
import HTTPClient.Request;
import HTTPClient.ResultType;
import Utils.Match;
import org.jsoup.Connection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.CookieStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FormularFuzzer {
    public static final int MIN_MUTATIONS = 1;
    public static final int MAX_MUTATIONS = 5;
    public Formular formular;
    private final List<Match> matches;
    public DebuggerResult bestCoverage = null;
    public HashMap<Double, Integer> reachedCoverages = new HashMap<>();
    private final String path;
    List<FuzzStrategy> fieldMutators = new ArrayList<>();
    final boolean[] fuzzed;


    public FormularFuzzer(Formular formular, List<String> seeds, String path, List<Match> matches) {
        this.formular = formular;
        this.path = path;
        this.matches = matches;
        this.fuzzed = new boolean[formular.getFields().size()];
        for (FormularField field : formular.getFields()) {
            if (field.getFuzzable()) {
                fieldMutators.add(new MutationBased(
                        Stream.concat(seeds.stream(), field.getValue().stream())
                                .collect(Collectors.toList()), MIN_MUTATIONS, MAX_MUTATIONS));
            } else {
                if (field.getValue().size() > 1) {
                    fieldMutators.add(new OptionFuzzerStrategy(field.getValue()));
                } else {
                    fieldMutators.add(new NoStrategy(field.getValue().get(0)));
                }
            }
        }
    }


    public ResultType fuzz(Debugger client, CookieStore cookieStore) throws Exception {
        final HashMap<String, String> params = new HashMap<>();
        final List<String> seeds = new ArrayList<>();
        ResultType r = ResultType.OK;
        // random number between 0 and formular.getFields().size() - 1
        int index = (int) (Math.random() * formular.getFields().size());
        for (int i = 0; i < formular.getFields().size(); i++) {
            String s;
            if (i == index) {
                s = fieldMutators.get(i).fuzz();
                fuzzed[i] = true;
            } else {
                s = fieldMutators.get(i).lastSeed();
                fuzzed[i] = false;
            }
            params.put(formular.getFields().get(i).getName(), s);
            seeds.add(s);
        }
        Connection.Response result = client.dump(() -> Request.send(formular.getActionUrl(), params, formular.getMethod(), cookieStore));

        DebuggerResult debuggerResult = client.getResult();
        double hash = debuggerResult.hash();
        if (!reachedCoverages.containsKey(hash)) {
            //System.out.println("New Branch discovered Formular: " + formular.getActionUrl());
            for (int i = 0; i < formular.getFields().size(); i++) {
                if (fuzzed[i]) {
                    fieldMutators.get(i).addToPopulation(seeds.get(i));
                }
            }


            if (result.statusCode() >= 400 && result.statusCode() < 500) {
                System.out.println("Triggered Client error");
                r = ResultType.CLIENTERROR;
                writeCrash(params, String.valueOf(result.statusCode()));
            } else if (result.statusCode() >= 500) {
                System.out.println("Triggered Server error");
                r = ResultType.SERVERERROR;
                writeCrash(params, String.valueOf(result.statusCode()));
            } else {
                for (Match match : matches) {
                    if (match.matches(result.body())) {
                        System.out.printf("Triggered %s\n", match.name);
                        r = ResultType.TRIGGERED;
                        writeCrash(params, match.name);
                        break;
                    }
                }
            }
            if (bestCoverage == null) {
                bestCoverage = debuggerResult.clone();
            } else {
                bestCoverage.merge(debuggerResult.clone());
            }
        }
        reachedCoverages.put(hash, reachedCoverages.getOrDefault(hash, 0) + 1);
        return r;
    }

    private void writeCrash(HashMap<String, String> params, String result) throws IOException {
        try (FileWriter fw = new FileWriter(path + "/reproduction.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("%s %s: %s %s\n_______________"
                    .formatted(formular.getMethod(), formular.getActionUrl(), result, params));
        }
    }

}
