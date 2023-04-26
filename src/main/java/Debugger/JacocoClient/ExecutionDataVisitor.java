package Debugger.JacocoClient;

import org.jacoco.core.data.*;

public class ExecutionDataVisitor implements ISessionInfoVisitor, IExecutionDataVisitor {
    public ExecutionDataStore store = new ExecutionDataStore();
    private String filter = "";

    public ExecutionDataVisitor(String filter) {
        this.filter = filter;
    }

    public void visitSessionInfo(final SessionInfo info) {
        //System.out.println(info.getId());
        //System.out.println(info.getStartTimeStamp());
        //System.out.println(info.getDumpTimeStamp());
    }

    public void visitClassExecution(final ExecutionData data) {
        if (data.getName().contains(filter)) {
            store.put(data);
        }
    }

    public void resetStore() {
        store = new ExecutionDataStore();
    }
}
