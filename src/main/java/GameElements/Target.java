package GameElements;

import Enums.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Target {

    @Getter @Setter private Who who;
    @Getter @Setter private Where where;
    @Getter @Setter private What what;
    @Getter @Setter private String name;
    @Getter @Setter private Collection collection;
    @Getter @Setter private Album album;
    @Getter @Setter private List<String> rarity;
    @Getter @Setter private List<Card> targetCards; // For expiry effects

    public Target(Who who) {
        this.who = who;
    }

    public Target(Who who, Where where) {
        this.who = who;
        this.where = where;
    }

    public Target(Who who, Where where, Album album) {
        this.who = who;
        this.where = where;

        this.album = album;
        this.what = What.ALBUM;
    }

    public Target(Who who, Where where, Collection collection) {
        this.who = who;
        this.where = where;

        this.collection = collection;
        this.what = What.COLLECTION;
    }

    public Target(Who who, Where where, String name, boolean exactMatch) {
        this.who = who;
        this.where = where;

        this.name = name;
        this.what = exactMatch ? What.NAME : What.NAME_CONTAINS;
    }

    public Target(Who who, Where where, What what, String compareTo) {
        this.who = who;
        this.where = where;
        this.what = what;
        this.name = compareTo;
    }

    public Target(List<Card> targetCards) {
        this.targetCards = targetCards;
    }

    public boolean hasPresetCards() {
        return this.targetCards != null;
    }
}
