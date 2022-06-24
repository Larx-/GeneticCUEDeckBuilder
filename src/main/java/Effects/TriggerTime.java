package Effects;

public enum TriggerTime {
    ON_START_GAME,
    ON_START_ROUND,
    ON_START_TURN, // Same as draw, but used for START
    ON_DRAW,
    ON_PLAY,
    ON_RETURN,
    ON_END_TURN, // Same as return, but only ever used as "counter effect"
    ON_END_ROUND,
    ON_TIMER
}
