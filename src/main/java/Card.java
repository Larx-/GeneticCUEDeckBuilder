import lombok.Getter;
import lombok.Setter;

public class Card {
    @Getter private final String id;
    @Getter private final String name;
    @Getter private final String set;
    @Getter private final int baseCost;
    @Getter private final int basePower;
    @Getter @Setter private int currentCost;
    @Getter @Setter private int currentPower;
    @Getter @Setter private boolean isLocked;
    // TODO: isOnFire

    public Card(String id, String name, String set, int energyCost, int basePower){
        this.id = id;
        this.name = name;
        this.set = set;
        this.baseCost = energyCost;
        this.basePower = basePower;
        this.currentCost = this.baseCost;
        this.currentPower = this.basePower;
        this.isLocked = false;
    }

    public Card copy(){
        return new Card(id,name,set, baseCost,basePower);
    }

    @Override
    public String toString(){
        return "Name = " + this.name;
    }
}
