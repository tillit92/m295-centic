package stegmueller.til.centic.model;

import lombok.Data;

@Data
public class Hello {

    private String name;

    public Hello(String name) {
        this.name = name;
    }

    public Hello() {}
}
