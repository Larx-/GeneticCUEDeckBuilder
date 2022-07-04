import GameElements.Card;
import Enums.Album;
import Enums.Collection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CardCreationTest {

    private final Card card = new Card(0,"SST029", "Fireworks Galaxy",
            Album.SPACE, Collection.SST, 2, 17, null);

    @Test
    void cardSetCorrectly() {
        assertEquals("SST029", card.getIdString());
        assertEquals("Fireworks Galaxy", card.getName());
        assertEquals(Album.SPACE, card.getAlbum());
        assertEquals(2, card.getBaseEnergy());
        assertEquals(17, card.getBasePower());
    }
}
