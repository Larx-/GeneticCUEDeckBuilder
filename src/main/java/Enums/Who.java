package Enums;

public enum Who {
    SELF,
    OTHER,
    BOTH,
    RESIDENT,
    OPPONENT;

    public static Who fromString(String search) {
        for (Who nextElem : Who.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
