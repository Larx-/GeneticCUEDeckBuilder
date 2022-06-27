package Enums;

public enum TriggerTime {
    START_GAME,
    START_ROUND,
    START, // Same as draw, but used for START
    DRAW,
    PLAY,
    RETURN,
    END_TURN, // Same as return, but only ever used as "counter effect"
    END_ROUND,
    TIMER;    // Only ever in expiry

    public static TriggerTime fromString(String search) {
        for (TriggerTime nextElem : TriggerTime.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
