package Crawl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormularField {
    private String name;
    private List<String> value;
    private Boolean fuzzable = true;
    private String type = "text";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * generate hashcode
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public List<String> getValue() {
        if (value == null) {
            return Collections.singletonList("");
        }
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }

    public void addValue(String value) {
        if (this.value == null) {
            this.value = new ArrayList<>();
        }
        this.value.add(value);
    }

    public void setValue(String value) {
        this.value = Collections.singletonList(value);
    }

    public Boolean getFuzzable() {
        return fuzzable;
    }

    public void setFuzzable(Boolean fuzzable) {
        this.fuzzable = fuzzable;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}