import GameElements.Card;
import Enums.Album;
import Enums.Collection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CardCreationTest {

    private final Card card = new Card("SST029", "Fireworks Galaxy",
            Album.SPACE, Collection.EXST, 2, 17, null);

    @Test
    void cardSetCorrectly() {
        assertEquals("SST029", card.getId());
        assertEquals("Fireworks Galaxy", card.getName());
        assertEquals("Exploring the Stars", card.getAlbum());
        assertEquals(2, card.getBaseCost());
        assertEquals(17, card.getBasePower());
    }
}
