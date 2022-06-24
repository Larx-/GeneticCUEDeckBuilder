package GameElements;

import Effects.EffectCollection;
import Setup.Album;
import Setup.Collection;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

public class Card {
    @Getter private final String id;
    @Getter private final String name;
    @Getter private final Album album;
    @Getter private final Collection collection;
    @Getter private final int baseCost;
    @Getter private final int basePower;
    @Getter private final EffectCollection effects;
    // Transient "in-play" variables
    @Getter @Setter private int currentCost;
    @Getter @Setter private int currentPower;
    @Getter @Setter private boolean isLocked;
    // TODO: isOnFire

    public Card(String id, String name, Album album, Collection collection,
                int energyCost, int basePower, EffectCollection effects){
        this.id = id;
        this.name = name;
        this.album = album;
        this.collection = collection;
        this.baseCost = energyCost;
        this.basePower = basePower;
        this.effects = effects;
        this.currentCost = this.baseCost;
        this.currentPower = this.basePower;
        this.isLocked = false;
    }

    public Card copyFresh(){
        return new Card(id,name, album, collection, baseCost, basePower, effects);
    }

    @Override
    public String toString(){
        return String.format("%s]  %s  E:%d (%d)   P:%d (%d)   %s  %s",
                StringUtils.leftPad(this.id,3),
                StringUtils.rightPad(StringUtils.abbreviate(this.name,30),30),
                this.currentCost, this.baseCost, this.currentPower, this.basePower,
                (this.isLocked ? "[\uD83D\uDD12]" : "   "),
                (this.effects == null ? "" : this.effects.getMiniDescription()));
    }
}
