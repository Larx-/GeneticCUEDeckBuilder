package Effects;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EffectCollection {

    @Getter String miniDescription;
    @Getter boolean isFullyInitialized;
    @Getter List<EffectContainer> effects;

    public EffectCollection(String miniDescription, boolean isFullyInitialized, EffectContainer ... effects) {
        this.miniDescription = miniDescription;
        this.isFullyInitialized = isFullyInitialized;
        this.effects = Arrays.asList(effects);
    }

    public List<EffectContainer> getEffectsWithTrigger(TriggerTime trigger) {
        List<EffectContainer> rc = new ArrayList<>();

        for (EffectContainer effectCont : this.effects) {
            if (effectCont.getTriggerTime() == trigger) {
                rc.add(effectCont);

                if (effectCont.getExpiryEffect() != null) {
                    rc.add(effectCont.getExpiryEffect());
                }
            }
        }
        return rc;
    }
}
