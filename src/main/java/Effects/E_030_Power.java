package Effects;

import GameElements.Card;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class E_030_Power extends Effect { // TODO: Deeper inheritance for player / card specific Effects

    @Setter List<Card> effectedCards;
    @Getter @Setter String initializationString = null;
    int changeBy;

    public E_030_Power(List<Card> effectedCards, int changeBy, List<ConditionInterface> conditions) {
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
                int newPower = card.getCurrentPower() + this.changeBy;
                card.setCurrentPower(newPower);
            }
        }
    }
}
