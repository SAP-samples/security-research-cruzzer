package Debugger.JacocoClient.ProbeAnalyzer;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.data.ExecutionDataStore;

import java.io.IOException;
import java.io.InputStream;

public record ProbeAnalyzer(ExecutionDataStore probes, CoverageVisitor coverageVisitor) {
    public void analyze(final InputStream input) throws IOException {
        Analyzer analyzer = new Analyzer(probes, coverageVisitor);
        analyzer.analyzeAll(input, "");
    }
}
