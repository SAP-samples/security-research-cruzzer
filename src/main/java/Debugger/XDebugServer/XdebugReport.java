package Debugger.XDebugServer;

import Debugger.DebuggerResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class XdebugReport extends DebuggerResult implements Cloneable {
    Map<String, Set<Integer>> merged = new HashMap<>();

    public XdebugReport(List<DebuggerResult> reports) {
        super();
        for (DebuggerResult report : reports) {
            this.merge(report);
        }
    }

    public XdebugReport() {
        super();
    }

    public XdebugReport(Map<String, Set<Integer>> result) {
        this.merged = result;
    }


    public String toString(int crashes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Coverage: %f , 500: %d \n".formatted(calculateCoverage(), crashes));
        for (Map.Entry<String, Set<Integer>> entry : merged.entrySet()) {
            sb.append(entry.getKey()).append(" ");
            sb.append(entry.getValue().stream().sorted().collect(Collectors.toList())).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void writeToFile(String fileName, int crashes) throws IOException {
        File file = new File(fileName);
        assert file.exists() : "File does not exist";
        assert file.isFile() : "File is not a file";
        assert file.canWrite() : "File is not writable";
        assert merged.size() > 0 : "No data to write";
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(this.toString(crashes));
        writer.close();
    }

    @Override
    public double calculateCoverage() {
        double coverage = 0;
        for (Map.Entry<String, Set<Integer>> data : this.merged.entrySet()) {
            coverage += data.getValue().size();
        }
        return coverage;
    }

    private void merge(Map<String, Set<Integer>> other) {
        for (Map.Entry<String, Set<Integer>> entry : other.entrySet()) {
            if (merged.containsKey(entry.getKey())) {
                merged.get(entry.getKey()).addAll(entry.getValue());
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void merge(XdebugReport other) {
        merge(other.merged);
    }

    public void merge(DebuggerResult other) {
        if (other instanceof XdebugReport) {
            merge((XdebugReport) other);
        }
    }

    @Override
    public long hash() {
        return merged.hashCode();
    }

    @Override
    public XdebugReport clone() {
        XdebugReport clone = (XdebugReport) super.clone();
        clone.merged = merged.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return clone;
    }
}
