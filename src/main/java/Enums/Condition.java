package Enums;

import lombok.Getter;

public enum Condition {
    PLAYED;

    @Getter private Who who;
    @Getter private What what;
    @Getter private String name;

    public Condition who(Who who) {
        this.who = who;
        return this;
    }

    public Condition what(What what) {
        this.what = what;
        return this;
    }

    public Condition name(String name) {
        this.name = name;
        return this;
    }
}
