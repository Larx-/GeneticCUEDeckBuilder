package Enums;

public enum Album {
    ARTS_AND_CULTURE ("Arts & Culture"),
    OCEANS_AND_SEAS  ("Oceans & Seas"),
    SPACE            ("Space"),
    LIFE_ON_LAND     ("Life on Land"),
    HISTORY          ("History"),
    PALEONTOLOGY     ("Paleontology"),
    SCIENCE          ("Science"),
    NAN              ("NAN"),
    NAN_2            ("NAN_2"),
    NAN_3            ("NAN_3");

    private final String name;

    Album(String s) {
        this.name = s;
    }

    public static Album fromString(String search) {
        for (Album nextElem : Album.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
