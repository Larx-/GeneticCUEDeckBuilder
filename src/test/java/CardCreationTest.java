import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CardCreationTest {

    private final Card card = new Card("SST029",
            "Fireworks Galaxy", "Exploring the Stars", 2, 17);

    @Test
    void cardSetCorrectly() {
        assertEquals("SST029", card.getId());
        assertEquals("Fireworks Galaxy", card.getName());
        assertEquals("Exploring the Stars", card.getSet());
        assertEquals(2, card.getBaseCost());
        assertEquals(17, card.getBasePower());
    }
}
