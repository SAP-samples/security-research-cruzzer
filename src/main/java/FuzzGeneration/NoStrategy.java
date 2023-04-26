package FuzzGeneration;

public class NoStrategy implements FuzzStrategy {
    String seed;

    public NoStrategy(String seed) {
        this.seed = seed;
    }

    public String fuzz() {
        return seed;
    }

    @Override
    public void addToPopulation(String s) {
        
    }

    @Override
    public String lastSeed() {
        return seed;
    }

}
