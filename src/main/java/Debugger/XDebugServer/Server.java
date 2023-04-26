package Debugger.XDebugServer;

import Debugger.Debugger;
import Debugger.DebuggerResult;
import org.jsoup.Connection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static Debugger.XDebugServer.Result.parseMessage;
import static java.lang.Thread.sleep;

public class Server extends Debugger {
    private final Map<String, Set<Integer>> result = new HashMap<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 10, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>());
    private Boolean measure = false;

    public Server(String address, int port, String filter) {
        super(address, port, filter);
        executor.execute(task);
    }

    Runnable task = () -> {
        while (true) {
            if (!measure) {
                try {
                    sleep(1);
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            try (ServerSocket server = new ServerSocket(port, 10);
                 Socket client = server.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
                 server.setSoTimeout(360000);
                 client.setSoTimeout(360000);
                receive(in);
                Result data = null;
                while (data == null || data.running) {
                    step(out);
                    data = receive(in);
                    result.putIfAbsent(data.fileName, new HashSet<>());
                    result.get(data.fileName).add(data.lineNumber);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    public Connection.Response dump(Callable<Connection.Response> request) throws Exception {
        result.clear();
        setMeasure(true);
        Connection.Response r = request.call();
        setMeasure(false);
        return r;
    }

    @Override
    public DebuggerResult getResult() {
        return new XdebugReport(this.result);
    }

    @Override
    public void close() {
        executor.shutdown();
    }


    private void step(BufferedWriter out) throws IOException {
        String s = String.valueOf((int) (Math.random() * 20) + 1);
        out.write("step_into -i " + s + " -- " + "\0");
        out.flush();
    }

    private Result receive(BufferedReader in) throws IOException, InterruptedException {
        char[] chars = new char[512];
        in.read(chars);
        return parseMessage(new String(chars));
    }

    public void setMeasure(Boolean measure) {
        this.measure = measure;
    }

}
