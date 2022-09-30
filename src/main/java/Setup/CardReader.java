package Setup;

import Controlling.Main;
import Effects.Effect;
import Enums.Album;
import Enums.Collection;
import Enums.TriggerTime;
import GameElements.Card;
import PreProcessing.EffectChunkParser;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.log4j.Log4j2;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Log4j2
public class CardReader {

    List<String[]> cardsFromCSV;
    Map<Integer,Card> cardsInMemory;
    Map<String,Integer> idStringIndex;
    Map<String,Integer> nameIndex;
    Map<String,String> nameToStringIdIndex;
    private int numberOfCards;
    EffectChunkParser effectParser;

    Map<String,String[]> stringIndexToCombosMap;
    Map<Collection,List<String>> collectionToCardsMap;
    Map<Album,List<String>> albumToCardsMap;

    enum header {
        Id,
        IdString,
        Name,
        Lim,
        Rarity,
        Collection,
        Energy,
        Power,
        EffectDescription,
        Effect,
        CombosWith
    }

    public CardReader(String filename) {
        this.initCardReader(filename);
    }

    private void initCardReader (String filename) {
        try (CSVReader reader = new CSVReader(new FileReader(filename))) {
            this.cardsFromCSV = reader.readAll();
            this.cardsInMemory = new HashMap<>();
            this.idStringIndex = new HashMap<>();
            this.nameIndex = new HashMap<>();
            this.nameToStringIdIndex = new HashMap<>();

            this.stringIndexToCombosMap = new HashMap<>();
            this.collectionToCardsMap = new HashMap<>();
            this.albumToCardsMap = new HashMap<>();
            Arrays.stream(Collection.values()).forEach(collection -> this.collectionToCardsMap.put(collection, new ArrayList<>()));
            Arrays.stream(Album.values()).forEach(album -> this.albumToCardsMap.put(album, new ArrayList<>()));

            this.numberOfCards = this.cardsFromCSV.size();

            for (int i = 1; i < this.numberOfCards; i++) {
                String[] cardCSV = this.cardsFromCSV.get(i);
                this.idStringIndex.put(cardCSV[header.IdString.ordinal()],i);
                this.nameIndex.put(cardCSV[header.Name.ordinal()].toLowerCase(),i);
                this.nameToStringIdIndex.put(cardCSV[header.Name.ordinal()].toLowerCase(),cardCSV[header.IdString.ordinal()]);

                Collection collection = Collection.fromString(cardCSV[header.Collection.ordinal()]);
                Album album = collection.getAffiliatedAlbum();
                this.collectionToCardsMap.get(collection).add(cardCSV[header.IdString.ordinal()]);
                this.albumToCardsMap.get(album).add(cardCSV[header.IdString.ordinal()]);
            }

        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        this.effectParser = new EffectChunkParser(this.nameIndex.keySet());
    }

    public Card getRandomCard () {
        return getCard(Main.random.nextInt(this.numberOfCards));
    }

    public String getRandomCardStr () {
        String[] cardCsv = this.getCardCSV(Main.random.nextInt(this.numberOfCards-1)+1);
        if (cardCsv == null){
            return null;
        } else {
            return cardCsv[header.IdString.ordinal()];
        }
    }

    public Set<String> getSetOfAllCards() {
        return this.idStringIndex.keySet();
    }

    public List<String> getPotentialCombos (String comboWith) {
        List<String> comboList = new ArrayList<>();
        String[] combosUnexpanded = this.stringIndexToCombosMap.get(comboWith);

        for (String potCombo : combosUnexpanded) {
            if (Album.fromString(potCombo) != null) {
                comboList.addAll(this.albumToCardsMap.get(Album.fromString(potCombo)));

            } else if (Collection.fromString(potCombo) != null) {
                comboList.addAll(this.collectionToCardsMap.get(Collection.fromString(potCombo)));

            } else if (this.nameIndex.containsKey(potCombo.toLowerCase())) {
                comboList.add(this.nameToStringIdIndex.get(potCombo.toLowerCase()));
            }
        }

        return comboList;
    }

    public Card getCard (int index) {
        // Caching
        if (this.cardsInMemory.containsKey(index)) {
            return this.cardsInMemory.get(index);
        }

        String[] cardCSV = this.getCardCSV(index);
        if (cardCSV == null){
            return null;
        }

        int id = Integer.parseInt(cardCSV[header.Id.ordinal()]);
        String idString = cardCSV[header.IdString.ordinal()];
        String name = cardCSV[header.Name.ordinal()];
        String rarity = cardCSV[header.Rarity.ordinal()];
        boolean limited = cardCSV[header.Lim.ordinal()].equals("1");

        Collection collection = Collection.fromString(cardCSV[header.Collection.ordinal()]);
        Album album = collection.getAffiliatedAlbum();
        int baseEnergy = Integer.parseInt(cardCSV[header.Energy.ordinal()]);
        int basePower = Integer.parseInt(cardCSV[header.Power.ordinal()]);

        String effectString = cardCSV[header.EffectDescription.ordinal()];
        String effect = cardCSV[header.Effect.ordinal()];
        Map<TriggerTime,List<Effect>> effectMap = this.effectParser.parseEffects(name, effect);

        // Save combos here, instead of in the cards
        String comboString = cardCSV[header.CombosWith.ordinal()];
        String[] combosWith = comboString.equals("[]") ? new String[0] : comboString.replace("[","").replace("]","").split(",");
        this.stringIndexToCombosMap.put(idString, combosWith);

        Card card = new Card(id, idString, name, rarity, limited, effectString, album, collection, baseEnergy, basePower, effectMap);

        // Caching
        this.cardsInMemory.put(index,card);

        return card;
    }

    public Card getCardByStringIndex (String stringIndex) {
        return this.getCard(this.idStringIndex.get(stringIndex));
    }

    // TODO: use when mutating, do not forget to explode Albums and Collections (by saving a index of all albums to cards and collections to cards)
    public String[] getCombosOf (String stringIndex) {
        return this.stringIndexToCombosMap.get(stringIndex);
    }

    public Card getCardByName (String name) {
        return this.getCard(this.nameIndex.get(name));
    }

    private String[] getCardCSV (int index) {
        if (index > 0 && index < this.numberOfCards) {
            return this.cardsFromCSV.get(index);
        }
        return null;
    }

    public int getNumberOfCards() {
        // Accounting for headers
        return this.numberOfCards-1;
    }
}
