package Enums;

public enum TriggerTime {
    START_GAME,
    START_ROUND,
    START, // Same as draw, but used for START
    DRAW,
    PLAY,
    RETURN,

    // Only ever in expiry effect
    END_TURN,
    END_ROUND,
    TIMER,
    PERMANENT,
    UNTIL_PLAYED;

    public static TriggerTime fromString(String search) {
        for (TriggerTime nextElem : TriggerTime.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
