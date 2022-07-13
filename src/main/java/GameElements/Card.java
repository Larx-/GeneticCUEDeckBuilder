package GameElements;

import Effects.Effect;
import Enums.TriggerTime;
import Enums.Album;
import Enums.Collection;
import Enums.Who;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Card {
    @Getter private final int id;
    @Getter private final String idString;
    @Getter private final String name;
    @Getter private final String rarity;
    @Getter private final String effectString;
    @Getter private final Album album;
    @Getter private final Collection collection;
    @Getter private final int baseEnergy;
    @Getter private final int basePower;
    @Getter private final Map<TriggerTime,List<Effect>> effects;
    @Getter private final String[] combosWith; // TODO: use when mutating, do not forget to explode Albums and Collections


    // Transient "in-play" variables
    @Getter @Setter private int modifierEnergy;
    @Getter @Setter private int modifierPower;
    @Getter @Setter private boolean isLocked;
    @Getter @Setter private int burntPower;
    @Getter @Setter private int burnAmount;
    @Getter @Setter private List<Effect> expiryEffectsAfterPlayed;

    public Card(int id, String idString, String name, String rarity, String effectString, Album album, Collection collection,
                int baseEnergy, int basePower, Map<TriggerTime,List<Effect>> effects, String[] combosWith){
        this.id = id;
        this.idString = idString;
        this.name = name;
        this.rarity = rarity;
        this.effectString = effectString;
        this.album = album;
        this.collection = collection;
        this.baseEnergy = baseEnergy;
        this.basePower = basePower;
        this.effects = effects;
        this.combosWith = combosWith;

        this.resetCard();
    }

    public int getModifiedEnergy() {
        return Math.max(0, (this.baseEnergy + this.modifierEnergy));
    }

    public int getModifiedPower() {
        return Math.max(0, (this.burntPower + this.modifierPower));
    }

    public void applyExpiryEffects(Game game, Who selfPlayer) {
        for (Effect effect : this.expiryEffectsAfterPlayed) {
            List<Card> thisCard = new ArrayList<>();
            thisCard.add(this);
            effect.setTarget(new Target(thisCard));
            effect.applyEffect(game, selfPlayer);
        }
        this.expiryEffectsAfterPlayed.clear();
    }

    public List<Effect> getEffectsByTriggerTime(TriggerTime triggerTime) {
        if (this.effects == null) {
            return null;
        }
        return this.effects.get(triggerTime);
    }

    public Card copyFresh(){
        // TODO: Deep copy effects maybe
        return new Card(id, idString, name, rarity, effectString, album, collection, baseEnergy, basePower, effects, combosWith);
    }

    public void resetCard() {
        this.modifierEnergy = 0;
        this.modifierPower = 0;
        this.isLocked = false;
        this.burntPower = this.basePower;
        this.burnAmount = 0;
        this.expiryEffectsAfterPlayed = new ArrayList<>();
    }

    @Override
    public String toString(){
        return String.format("%s]  %s  E:%d (%d)   P:%d (%d)  %s  %s  %s",
                StringUtils.leftPad(this.idString,5),
                StringUtils.rightPad(StringUtils.abbreviate(this.name,30),30),
                this.getModifiedEnergy(), this.baseEnergy, this.getModifiedPower(), this.basePower,
                (this.isLocked ? " \uD83D\uDD12 " : "   "),
                (this.burnAmount > 0 ? " \uD83D\uDD25 " : "   "),
                (this.effects == null ? "" : "*"));
    }
}
