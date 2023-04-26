package Debugger.JacocoClient;

import Debugger.DebuggerResult;
import Debugger.JacocoClient.ProbeAnalyzer.CoverageVisitor;
import Debugger.JacocoClient.ProbeAnalyzer.ProbeAnalyzer;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JacocoReport extends DebuggerResult implements Cloneable {
    private final String pathToClassFiles;
    private String filter = "";
    ExecutionDataStore merged = new ExecutionDataStore();

    public JacocoReport(List<DebuggerResult> reports) {
        super();
        for (DebuggerResult report : reports) {
            this.merge(report);
        }
        pathToClassFiles = null;
    }

    public JacocoReport() {
        super();
        pathToClassFiles = null;
    }

    public JacocoReport(ExecutionDataStore sessions) {
        this.merged = sessions;
        pathToClassFiles = null;
    }

    public JacocoReport(ExecutionDataStore sessions, String pathToClassFiles, String filter) {
        this.merged = sessions;
        this.filter = filter;
        this.pathToClassFiles = pathToClassFiles;
    }

    private String probesToString(boolean[] probes) {
        StringBuilder sb = new StringBuilder();
        for (boolean b : probes) {
            sb.append(b ? "1" : "0");
        }
        return sb.toString();
    }

    public String toStringResolved(int crashes) {
        CoverageVisitor visitor = new CoverageVisitor(this.filter);
        ProbeAnalyzer analyzer = new ProbeAnalyzer(merged, visitor);
        try {
            analyzer.analyze(new FileInputStream(pathToClassFiles));
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Coverage: %f , Crashes: %d \n".formatted(calculateCoverage(), crashes));
        for (Map.Entry<String, List<Integer>> entry : visitor.getCoverageMap().entrySet()) {
            if (entry.getValue().size() <= 0)
                continue;

            sb.append(entry.getKey())
                    .append(entry.getValue().stream().map(Object::toString).reduce("", (a, b) -> a + "," + b))
                    .append("\n");
        }
        return sb.toString();
    }

    public String toString(int crashes) {
        if (pathToClassFiles != null) {
            return toStringResolved(crashes);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Coverage: %f , 500: %d \n".formatted(calculateCoverage(), crashes));
        for (ExecutionData entry : merged.getContents()) {
            sb.append(entry.getName()).append(" ")
                    .append(probesToString(entry.getProbes()))
                    .append("\n");
        }
        return sb.toString();
    }

    @Override
    public void writeToFile(String fileName, int crashes) throws IOException {
        File file = new File(fileName);
        assert file.exists() : "File does not exist";
        assert file.isFile() : "File is not a file";
        assert file.canWrite() : "File is not writable";
        assert merged.getContents().size() > 0 : "No data to write";
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(this.toString(crashes));
        writer.close();
    }

    @Override
    public double calculateCoverage() {
        double coverage = 0;
        int executionCount = 0;
        for (ExecutionData data : this.merged.getContents()) {
            executionCount++;
            double countHits = 0;
            for (int i = 0; i < data.getProbes().length; i++) {
                if (data.getProbes()[i]) {
                    countHits++;
                }
                if (countHits == Double.MAX_VALUE-1){
                    System.out.println("overflow in jacocoreport calculate coverage");
                }
            }
            coverage += countHits;
        }
        if (executionCount == 0)
            return 0d;
        return coverage;
    }

    private void merge(ExecutionDataStore other) {
        for (ExecutionData data : other.getContents()) {
            this.merged.put(data);
        }
    }

    private void merge(JacocoReport other) {
        merge(other.merged);
    }

    public void merge(DebuggerResult other) {
        if (other instanceof JacocoReport) {
            merge((JacocoReport) other);
        }
    }

    @Override
    public long hash() {
        long num = 0;
        for (ExecutionData data : this.merged.getContents()) {
            for (int i = 0; i < data.getProbes().length; i++) {
                num += Arrays.hashCode(data.getProbes());
            }
        }
        return num;
    }

    @Override
    public JacocoReport clone() {
        JacocoReport clone = (JacocoReport) super.clone();
        merged.getContents().stream().forEach((k) -> {
            ExecutionData tmpExecutionData = new ExecutionData(k.getId(), k.getName(), k.getProbes().clone());
            clone.merged.put(tmpExecutionData);
        });
        return clone;
    }
}
