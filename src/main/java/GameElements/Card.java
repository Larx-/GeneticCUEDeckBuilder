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
    @Getter private final int baseCost;
    @Getter private final int basePower;
    @Getter private final Map<TriggerTime,List<Effect>> effects;

    // Transient "in-play" variables
    @Getter @Setter private int modifierCost;
    @Getter @Setter private int modifierPower;
    @Getter @Setter private boolean isLocked;
    // TODO: On Fire

    public Card(String id, String name, Album album, Collection collection,
                int energyCost, int basePower, Map<TriggerTime,List<Effect>> effects){
        this.id = id;
        this.name = name;
        this.album = album;
        this.collection = collection;
        this.baseCost = energyCost;
        this.basePower = basePower;
        this.effects = effects;

        this.modifierCost = 0;
        this.modifierPower = 0;
        this.isLocked = false;
    }

    public int getModifiedCost() {
        return Math.max(0, (this.baseCost + this.modifierCost));
    }

    public int getModifiedPower() {
        return Math.max(0, (this.basePower + this.modifierPower));
    }

    public Card copyFresh(){
        // TODO: Deep copy effects
        return new Card(id,name, album, collection, baseCost, basePower, effects);
    }

    @Override
    public String toString(){
        return String.format("%s]  %s  E:%d (%d)   P:%d (%d)   %s  %s",
                StringUtils.leftPad(this.id,3),
                StringUtils.rightPad(StringUtils.abbreviate(this.name,30),30),
                this.modifierCost, this.baseCost, this.modifierPower, this.basePower,
                (this.isLocked ? "[\uD83D\uDD12]" : "   "),
                (this.effects == null ? "" : "*"));
    }
}
