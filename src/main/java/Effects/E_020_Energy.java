package Effects;

import Controlling.Main;
import GameElements.Card;
import GameElements.Game;
import GameElements.PlayerManager;
import Setup.Album;
import Setup.Collection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class E_020_Energy extends Effect { // TODO: Deeper inheritance for player / card specific Effects

    @Setter List<Card> effectedCards;
    @Getter @Setter String initializationString = null;
    int changeBy;

    public E_020_Energy(List<Card> effectedCards, int changeBy, List<ConditionInterface> conditions) {
        this.effectedCards = effectedCards;
        this.changeBy = changeBy;

        super.conditions = conditions;
    }

    @Override
    public void applyEffect() {
        if (this.effectedCards == null) {
            log.error("Initialization went wrong, no cards effected...");
        }
        if (super.checkConditions()) {
            for (Card card : this.effectedCards) {
                int newEnergy = card.getCurrentCost() + this.changeBy;
                card.setCurrentCost(newEnergy);
            }
        }
    }
}
