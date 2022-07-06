package Setup;

import Controlling.Main;
import Enums.Album;
import Enums.Collection;
import GameElements.RoundBonus;
import GameElements.Rules;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.IOUtils;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Log4j2
public class RulesInitializer {

    public Rules getRulesFromFile (String filename) {
        try {
            Reader reader = new FileReader(filename);
            String data = IOUtils.toString(reader);
            JSONObject jsonRules = new JSONObject(data);

            int start = jsonRules.getInt(  "EnergyStarting");
            int min =   jsonRules.getInt(  "EnergyMin");
            int max =   jsonRules.getInt(  "EnergyMax");
            int ept =   jsonRules.getInt(  "EnergyPerTurn");

            JSONObject guaranteedBoni = jsonRules.getJSONObject("GuaranteedBoni");
            JSONObject additionalBoni = jsonRules.getJSONObject("AdditionalBoni");

            return new Rules(start, min, max, ept, readRoundBoni(guaranteedBoni), readRoundBoni(additionalBoni));

        } catch (IOException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        return null;
    }

    private List<RoundBonus> readRoundBoni (JSONObject boni) {
        Set<String> boniKeys = boni.keySet();
        List<RoundBonus> roundBoni = new ArrayList<>();

        for (String boniKey : boniKeys) {

            Collection collection = Collection.fromString(boniKey);
            if (collection != null) {
                roundBoni.add(new RoundBonus(collection, boni.getInt(boniKey)));

            } else {
                Album album = Album.fromString(boniKey);
                if (album != null) {
                    roundBoni.add(new RoundBonus(album, boni.getInt(boniKey)));

                } else {
                    log.error("Could not find corresponding Album / Collection to apply boni to!");
                }
            }
        }
        return roundBoni;
    }

    public Rules getTestRules() {
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

    public RoundBonus getRandomRoundBonus() {
        return getRandomRoundBonus(10, (Main.random.nextInt(5)+1) * 10);
    }

    public RoundBonus getRandomRoundBonus(boolean noCollectionBonus){
        Album album = DeckInitializer.getRandomEnum(Album.class);
        return new RoundBonus(album, null, (Main.random.nextInt(4)+1) * 10, 0);
    }

    public RoundBonus getRandomRoundBonus(int albumBonus, int collectionBonus) {
        Collection coll = DeckInitializer.getRandomEnum(Collection.class);
        Album album = coll.getAffiliatedAlbum();

        return new RoundBonus(album, coll, albumBonus, collectionBonus);
    }
}
