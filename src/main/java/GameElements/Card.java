package GameElements;

import Effects.Effect;
import Enums.TriggerTime;
import Enums.Album;
import Enums.Collection;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class Card {
    @Getter private final String id;
    @Getter private final String name;
    @Getter private final Album album;
    @Getter private final Collection collection;
    @Getter private final int baseEnergy;
    @Getter private final int basePower;
    @Getter private final Map<TriggerTime,List<Effect>> effects;

    // Transient "in-play" variables
    @Getter @Setter private int modifierEnergy;
    @Getter @Setter private int modifierPower;
    @Getter @Setter private boolean isLocked;
    @Getter @Setter private int burntPower;
    @Getter @Setter private int burnAmount;

    public Card(String id, String name, Album album, Collection collection,
                int baseEnergy, int basePower, Map<TriggerTime,List<Effect>> effects){
        this.id = id;
        this.name = name;
        this.album = album;
        this.collection = collection;
        this.baseEnergy = baseEnergy;
        this.basePower = basePower;
        this.effects = effects;

        this.modifierEnergy = 0;
        this.modifierPower = 0;
        this.isLocked = false;
        this.burntPower = this.basePower;
        this.burnAmount = 0;
    }

    public int getModifiedEnergy() {
        return Math.max(0, (this.baseEnergy + this.modifierEnergy));
    }

    public int getModifiedPower() {
        return Math.max(0, (this.burntPower + this.modifierPower));
    }

    public List<Effect> getEffectsByTriggerTime(TriggerTime triggerTime) {
        if (this.effects == null) {
            return null;
        }
        return this.effects.get(triggerTime);
    }

    public Card copyFresh(){
        // TODO: Deep copy effects
        return new Card(id,name, album, collection, baseEnergy, basePower, effects);
    }

    @Override
    public String toString(){
        return String.format("%s]  %s  E:%d (%d)   P:%d (%d)  %s  %s  %s",
                StringUtils.leftPad(this.id,3),
                StringUtils.rightPad(StringUtils.abbreviate(this.name,30),30),
                this.getModifiedEnergy(), this.baseEnergy, this.getModifiedPower(), this.basePower,
                (this.isLocked ? " \uD83D\uDD12 " : "   "),
                (this.burnAmount > 0 ? " \uD83D\uDD25 " : "   "),
                (this.effects == null ? "" : "*"));
    }
}
