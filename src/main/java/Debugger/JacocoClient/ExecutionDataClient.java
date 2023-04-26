package Debugger.JacocoClient;

import Debugger.Debugger;
import Debugger.DebuggerResult;
import HTTPClient.Request;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.jsoup.Connection;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.Callable;

public final class ExecutionDataClient extends Debugger {

    private Socket socket = null;
    private ExecutionDataVisitor localWriter;
    private RemoteControlWriter writer = null;
    private RemoteControlReader reader = null;
    private String pathToClassFiles = null;

    public ExecutionDataClient(String host, int port, String filter) {
        super(host, port, filter);
        connect();
    }

    public ExecutionDataClient(String host, int port, String filter, String pathToClassfiles) {
        super(host, port, filter);
        this.pathToClassFiles = pathToClassfiles;
        connect();
    }

    public void connect() {
        System.out.println(" Open a socket to the coverage agent..");
        try {
            socket = new Socket(InetAddress.getByName(host), port);
        } catch (IOException e) {
            e.printStackTrace();
        }


        localWriter = new ExecutionDataVisitor(filter);

        try {
            writer = new RemoteControlWriter(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reader = new RemoteControlReader(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        reader.setSessionInfoVisitor(localWriter);
        reader.setExecutionDataVisitor(localWriter);
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Connection.Response dump(Callable<Connection.Response> request) throws Exception {
        localWriter.resetStore();
        Connection.Response r = request.call();
        writer.visitDumpCommand(true, true);
        writer.flush();
        reader.read();

        return r;
    }

    @Override
    public DebuggerResult getResult() {
        return new JacocoReport(localWriter.store, pathToClassFiles, filter);
    }

    public static void main(String[] args) throws IOException {
        ExecutionDataClient client = new ExecutionDataClient("localhost", 6300, "cspf", "/Users/i534627/Projects/JavaVulnerableLab/test2.war");
        //ExecutionDataClient client = new ExecutionDataClient("localhost", 6300, "owasp", "/Users/i534627/Projects/BenchmarkJava/target/benchmark2.war");
        client.localWriter.resetStore();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        if (Objects.equals(reader.readLine(), "stop")) {
            client.writer.visitDumpCommand(true, true);
            client.writer.flush();
            client.reader.read();
            DebuggerResult result = client.getResult();
            System.out.println(result.calculateCoverage());
            result.writeToFile("owasp_result.txt", 0);
            client.close();
        }

    }
}
