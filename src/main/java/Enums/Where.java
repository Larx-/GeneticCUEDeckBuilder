package Enums;

public enum Where {
    CARDS_IN_DECK,
    CARDS_IN_HAND,
    CARDS_REMAINING;

    public static Where fromString(String search) {
        for (Where nextElem : Where.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
