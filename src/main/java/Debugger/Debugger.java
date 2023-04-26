package Debugger;

import org.jsoup.Connection;

import java.util.concurrent.Callable;

public abstract class Debugger {
    public String host;
    public int port;
    public String filter;

    public Debugger(String host, int port, String filter) {
        this.host = host;
        this.port = port;
        this.filter = filter;
    }

    public abstract Connection.Response dump(Callable<Connection.Response> request) throws Exception;

    public abstract DebuggerResult getResult();

    public abstract void close();
}
