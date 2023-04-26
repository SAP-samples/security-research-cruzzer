package Debugger.JacocoClient.ProbeAnalyzer;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageVisitor;
import org.jacoco.core.analysis.IMethodCoverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoverageVisitor implements ICoverageVisitor {
    private final String filter;

    public Map<String, List<Integer>> getCoverageMap() {
        return coverageMap;
    }

    private final Map<String, List<Integer>> coverageMap = new HashMap<>();

    public CoverageVisitor(String filter) {
        this.filter = filter;
    }

    @Override
    public void visitCoverage(IClassCoverage coverage) {
        if (!coverage.getPackageName().contains(filter) || coverage.isNoMatch())
            return;

        for (IMethodCoverage method : coverage.getMethods()) {
            ArrayList<Integer> lineCoverage = new ArrayList<>();
            for (int i = method.getFirstLine(); i <= method.getLastLine(); i++) {
                if (coverage.getLine(i).getStatus() != ICounter.NOT_COVERED) {
                    lineCoverage.add(i - method.getFirstLine());
                }
            }
            mergeCoverageMap(coverage.getName() + "::" + method.getName(), lineCoverage);
        }
    }

    private void mergeCoverageMap(String name, ArrayList<Integer> lineCoverage) {
        if (coverageMap.containsKey(name)) {
            if (!name.contains(filter)) {
                return;
            }
            coverageMap.get(name).addAll(lineCoverage);
        } else {
            coverageMap.put(name, lineCoverage);
        }
    }
}
