package Enums;

import lombok.Getter;

public enum Target {
    CARD_THIS,
    CARDS_IN_DECK,
    CARDS_IN_HAND,
    CARDS_IN_REMAINING,
    PLAYER;

    @Getter private Who who;
    @Getter private What what;
    @Getter private String name;

    public Target who(Who who) {
        this.who = who;
        return this;
    }

    public Target what(What what) {
        this.what = what;
        return this;
    }

    public Target name(String name) {
        this.name = name;
        return this;
    }
}
