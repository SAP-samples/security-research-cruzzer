package FuzzGeneration;

public interface FuzzStrategy {
    String fuzz();

    void addToPopulation(String s);
    String lastSeed();
}
