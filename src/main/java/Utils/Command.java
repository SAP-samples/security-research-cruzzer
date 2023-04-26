package Utils;


import picocli.CommandLine;

import java.io.File;

public class Command {
    @CommandLine.Option(names = {"-t", "--startpage"}, description = "Startpage for Crawling", required = true)
    public String startPage = "Java";

    @CommandLine.Option(names = {"-l", "--language"}, description = "PHP or Java?", required = true)
    public String programmingLanguage = "Java";

    @CommandLine.Option(names = {"-i", "--fuzz"}, description = "Maximal Fuzzes")
    public Integer fuzzes = 1000;

    @CommandLine.Option(names = {"-f", "--filter"}, description = "White-List Filter for Classes that should be included in the coverage analysis")
    public String filter = "";

    @CommandLine.Option(names = {"-a", "--hostname"}, description = "Hostname")
    public String host = "localhost";

    @CommandLine.Option(names = {"-c", "--classes"}, description = "Path to Jar, War or Zip in case of Java. Needed to recover Jacoco Probes")
    public String classes = null;

    @CommandLine.Option(names = {"-p", "--port"}, description = "Port. Xdebug: 9003, Jacoco: 6300")
    public Integer port = 6300;

    @CommandLine.Option(names = {"-s", "--seeds"}, paramLabel = "SEEDS", description = "A Textfile with seeds. Newline Seperated", required = true)
    public File seedFile = null;

    @CommandLine.Option(names = {"-m", "--match"}, paramLabel = "matches", description = "A Textfile with new line seperated Name:Regex tuples. Will be matched against HTTP Response.")
    public File matchFile = null;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    public boolean helpRequested = false;

    public static String printHelp(){
        return "-t --startpage  <URL>  Startpage of the Webapplication.\n" +
                "-l --language  <LANGUAGE>  Language of the Web Backend (PHP or Java).\n" +
                "-i --fuzz <Number> Number of fuzzing iterations.\n" +
                "-f --filter <Filter> Filter Classes or Files (optional).\n" +
                "-a --hostname <HOSTNAME> Hostname of the Debugger if Jacoco is used.\n" +
                "-c --classes <Path> Compiled War/Jar to recover Jacoco Traces.\n" +
                "-p --port <PORT> Port of the Debugger.\n" +
                "-s --seeds <Path> Path to the seeds. Every seed should be new line seperated\n" +
                "-m --matches <Path> A Textfile with new line seperated Name:Regex tuples. Will be matched against HTTP Response.\n" +
                "-h --help Show this help.";
    }
}
