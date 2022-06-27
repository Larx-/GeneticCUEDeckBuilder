package Enums;

public enum What {
    NAME,
    NAME_INCLUDES,
    COLLECTION,
    ALBUM;

    public static What fromString(String search) {
        for (What nextElem : What.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
