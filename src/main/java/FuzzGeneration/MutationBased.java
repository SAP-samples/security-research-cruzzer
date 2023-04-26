package FuzzGeneration;

import java.util.List;

public class MutationBased implements FuzzStrategy {
    List<String> seed;
    int minMutations;
    int maxMutations;
    int seedIndex = 0;
    public List<String> population;
    public static final String listOfChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"§$%&/()=?`´'*+#-_.:,;<>|\\~@^°{}[] ß";

    private String lastSeed = "";

    public MutationBased(List<String> seed, int minMutations, int maxMutations) {
        this.seed = seed;
        this.minMutations = minMutations;
        this.maxMutations = maxMutations;
        this.reset();
    }

    void reset() {
        population = this.seed;
        seedIndex = 0;
    }

    /**
     * get random entry from population list
     *
     * @return
     */
    String getRandomEntry() {
        int index = (int) (Math.random() * population.size());
        return population.get(index);
    }

    @Override
    public String fuzz() {
        String candidate;
        if (seedIndex < seed.size()) {
            candidate = seed.get(seedIndex);
            seedIndex++;
        } else {
            candidate = createCandidate();
        }
        lastSeed = candidate;
        return candidate;
    }

    @Override
    public void addToPopulation(String s) {
        if (!population.contains(s))
            population.add(s);
    }

    @Override
    public String lastSeed() {
        return lastSeed;
    }

    /**
     * Delete Random Character from candidate
     *
     * @return
     */
    String deleteRandomCharacter(String candidate) {
        try {
            int index = (int) (Math.random() * candidate.length());
            return candidate.substring(0, index) + candidate.substring(index + 1);
        } catch (StringIndexOutOfBoundsException e) {
            return insertRandomCharacter(candidate);
        }
    }

    /**
     * Insert Random Character from candidate
     *
     * @return
     */
    String insertRandomCharacter(String candidate) {
        int index = (int) (Math.random() * candidate.length());
        //StringBuilder sb = new StringBuilder(candidate);
        //sb.insert(index, (char) (Math.random() * (127 - 32)) + 32);
        return candidate.substring(0, index) + listOfChars.charAt((int) (Math.random() * listOfChars.length())) + candidate.substring(index);
    }

    /**
     * flip random character from candidate
     *
     * @return
     */
    String flipRandomCharacter(String candidate) {
        try {
            int index = (int) (Math.random() * candidate.length());
            int flip = 1 << ((int) (Math.random() * 6));
            return candidate.substring(0, index) + (candidate.charAt(index) ^ flip) + candidate.substring(index + 1);
        } catch (StringIndexOutOfBoundsException e) {
            return insertRandomCharacter(candidate);
        }
    }

    String createCandidate() {
        String candidate = getRandomEntry();
        int trials = (int) (Math.random() * (maxMutations - minMutations)) + minMutations;
        for (int i = 0; i < trials; i++) {
            candidate = this.mutate(candidate);
        }

        return candidate;
    }

    private String mutate(String candidate) {
        int mutation = (int) (Math.random() * 3);
        return switch (mutation) {
            case 0 -> deleteRandomCharacter(candidate);
            case 1 -> insertRandomCharacter(candidate);
            default -> flipRandomCharacter(candidate);
        };
    }
}
