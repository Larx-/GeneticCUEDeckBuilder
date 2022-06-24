package Effects;

import lombok.Getter;
import lombok.Setter;

public class EffectContainer {

    @Getter Effect effect;  // Includes targets and conditions
    @Getter TriggerTime triggerTime;
    @Setter @Getter int afterXTurns;
    @Setter @Getter EffectContainer expiryEffect;

    public EffectContainer(Effect effect, TriggerTime triggerTime) {
        this.effect = effect;
        this.triggerTime = triggerTime;
        this.expiryEffect = null;
    }
}

