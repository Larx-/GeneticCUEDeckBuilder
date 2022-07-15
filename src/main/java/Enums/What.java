package Enums;

public enum What {
    NAME,
    NAME_INCLUDES,
    COLLECTION,
    ALBUM,
    BASE_ENERGY,
    BASE_POWER,
    RARITY,
    RANDOM;

    public static What fromString(String search) {
        for (What nextElem : What.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
