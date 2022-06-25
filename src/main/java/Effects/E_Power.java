package Effects;

import Enums.TriggerTime;
import GameElements.Card;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class E_Power extends Effect { // TODO: Deeper inheritance for player / card specific Effects

    @Setter List<Card> effectedCards;
    @Getter @Setter String initializationString = null;
    int changeBy;

    public E_Power(List<Card> effectedCards, int changeBy, List<ConditionInterface> conditions, TriggerTime triggerTime) {
        super(triggerTime);
        super.conditions = conditions;

        this.effectedCards = effectedCards;
        this.changeBy = changeBy;
    }

    @Override
    public Effect applyEffect() {
        if (this.effectedCards == null) {
            log.error("Initialization went wrong, no cards effected...");
        }
        if (super.conditionsFulfilled()) {
            for (Card card : this.effectedCards) {
                int newPower = card.getModifierPower() + this.changeBy;
                card.setModifierPower(newPower);
            }
            return super.expiryEffect;
        }
        return null;
    }
}
