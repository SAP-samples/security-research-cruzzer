package Debugger.XDebugServer;

public class Result {
    public boolean running;
    public String fileName;
    public int lineNumber;

    public Result(boolean running, String fileName, int lineNumber) {
        this.running = running;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }


    public static Result parseMessage(String message) {
        boolean running = !message.contains("status=\"stopping\"");
        if (!(message.contains("filename=") && message.contains("lineno="))){
            return new Result(running, "", 0);
        }
        try {
            String fileName = message.split("xdebug:message filename=\"")[1].split("\"")[0];
            String lineNumber = message.split(" lineno=\"")[1].split("\"")[0];
            return new Result(running, fileName, Integer.parseInt(lineNumber));
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(running, "", 0);
        }
    }

    @Override
    public String toString() {
        return "File: " + fileName + " Line: " + lineNumber;
    }
}
