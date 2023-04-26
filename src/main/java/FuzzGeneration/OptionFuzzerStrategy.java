package FuzzGeneration;

import java.util.ArrayList;
import java.util.List;

public class OptionFuzzerStrategy implements FuzzStrategy {
    String seed = "";
    public ArrayList<String> population = new ArrayList<>();

    public OptionFuzzerStrategy(List<String> options) {
        population.addAll(options);
    }

    public String fuzz() {
        int index = (int) (Math.random() * population.size());
        seed = population.get(index);
        return seed;
    }

    @Override
    public void addToPopulation(String s) {
        population.add(s);
    }

    @Override
    public String lastSeed() {
        return seed;
    }

}
