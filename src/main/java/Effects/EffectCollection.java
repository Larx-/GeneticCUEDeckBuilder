package Effects;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EffectCollection {

    @Getter String miniDescription;
    @Getter boolean isFullyInitialized;
    @Getter List<Effect> effects;

    public EffectCollection(String miniDescription, boolean isFullyInitialized, Effect ... effects) {
        this.miniDescription = miniDescription;
        this.isFullyInitialized = isFullyInitialized;
        this.effects = Arrays.asList(effects);
    }

    public List<Effect> getEffectsWithTrigger(TriggerTime trigger) {
        List<Effect> rc = new ArrayList<>();

        for (Effect effectCont : this.effects) {
            if (effectCont.getTriggerTime() == trigger) {
                rc.add(effectCont);

//                if (effectCont.getExpiryEffect() != null) {
//                    rc.add(effectCont.getExpiryEffect());
//                }
            }
        }
        return rc;
    }
}
