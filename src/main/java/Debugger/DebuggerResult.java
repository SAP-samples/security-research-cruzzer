package Debugger;

import java.io.IOException;
import java.util.List;

public abstract class DebuggerResult implements Cloneable {

    public DebuggerResult(List<DebuggerResult> results) {
    }

    public DebuggerResult() {
    }

    public abstract void writeToFile(String path, int crashes) throws IOException;

    public abstract void merge(DebuggerResult other);

    public abstract long hash();

    public abstract double calculateCoverage();

    @Override
    public DebuggerResult clone() {
        try {
            return (DebuggerResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
