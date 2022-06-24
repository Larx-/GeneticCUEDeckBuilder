package Effects;

public enum TargetCards {
    OWN,
    OWN_HAND,
    OWN_REMAINING,
    OTHER,
    OTHER_HAND,
    OTHER_REMAINING,
    BOTH,
    BOTH_HAND,
    BOTH_REMAINING,

    COMPLEX,
    INIT_FINISHED,
    INVALID_STATE;

    public static TargetCards fromString(String search) {
        for (TargetCards nextElem : TargetCards.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
