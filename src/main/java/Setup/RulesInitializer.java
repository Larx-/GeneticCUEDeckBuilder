package Setup;

import Controlling.Main;
import GameElements.RoundBonus;
import GameElements.Rules;

import java.util.ArrayList;
import java.util.List;

public class RulesInitializer {

    // TODO: Read rules from file or something

    public static Rules getTestRules() {
        Collection coll = DeckInitializer.getRandomEnum(Collection.class);
        Album album = coll.getAffiliatedAlbum();

        List<RoundBonus> guaranteedRoundBoni = new ArrayList<>();
        guaranteedRoundBoni.add(getRandomRoundBonus());
        guaranteedRoundBoni.add(getRandomRoundBonus());
        guaranteedRoundBoni.add(getRandomRoundBonus());

        List<RoundBonus> additionalRoundBoni = new ArrayList<>();
        additionalRoundBoni.add(getRandomRoundBonus());
        additionalRoundBoni.add(getRandomRoundBonus());
        additionalRoundBoni.add(getRandomRoundBonus(true));

        return new Rules(30, 0, 45, 15, guaranteedRoundBoni, additionalRoundBoni);
    }

    public static RoundBonus getRandomRoundBonus() {
        return getRandomRoundBonus(10, (Main.random.nextInt(6)+1) * 10);
    }

    public static RoundBonus getRandomRoundBonus(boolean noCollectionBonus){
        Album album = DeckInitializer.getRandomEnum(Album.class);
        return new RoundBonus(album, null, (Main.random.nextInt(6)+1) * 10, 0);
    }

    public static RoundBonus getRandomRoundBonus(int albumBonus, int collectionBonus) {
        Collection coll = DeckInitializer.getRandomEnum(Collection.class);
        Album album = coll.getAffiliatedAlbum();

        return new RoundBonus(album, coll, albumBonus, collectionBonus);
    }
}
