package Enums;

import lombok.Getter;

public enum TriggerTime {
    START_GAME,
    START_ROUND,
    START_TURN, // Same as draw, but used for START
    DRAW,
    PLAY,
    RETURN,
    END_TURN, // Same as return, but only ever used as "counter effect"
    END_ROUND,
    TIMER;

    @Getter private Integer turns;

    public TriggerTime turns(int turns) {
        this.turns = turns;
        return this;
    }
}
