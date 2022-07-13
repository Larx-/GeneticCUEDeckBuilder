package Setup;

import Controlling.Main;
import Effects.Effect;
import Enums.TriggerTime;
import GameElements.Card;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.log4j.Log4j2;

import javax.sql.rowset.CachedRowSet;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Log4j2
public class CardReader {

    List<String[]> cardsFromCSV;
    Map<Integer,Card> cardsInMemory;
    Map<String,Integer> idStringIndex;
    Map<String,Integer> nameIndex;
    private int numberOfCards;
    EffectParser effectParser;

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
        EffectJSON,
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

            this.numberOfCards = this.cardsFromCSV.size();

            for (int i = 1; i < this.numberOfCards; i++) {
                String[] cardCSV = this.cardsFromCSV.get(i);
                this.idStringIndex.put(cardCSV[header.IdString.ordinal()],i);
                this.nameIndex.put(cardCSV[header.Name.ordinal()].toLowerCase(),i);
            }

        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        this.effectParser = new EffectParser(this.nameIndex.keySet());
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

        Enums.Collection collection = Enums.Collection.fromString(cardCSV[header.Collection.ordinal()]);
        Enums.Album album = collection.getAffiliatedAlbum();
        int baseEnergy = Integer.parseInt(cardCSV[header.Energy.ordinal()]);
        int basePower = Integer.parseInt(cardCSV[header.Power.ordinal()]);

        String effectString = cardCSV[header.EffectDescription.ordinal()];
        String effectJSON = cardCSV[header.EffectJSON.ordinal()];
        Map<TriggerTime,List<Effect>> effectMap = this.effectParser.parseEffects(effectJSON);
        String comboString = cardCSV[header.CombosWith.ordinal()];
        String[] combosWith = comboString.equals("[]") ? new String[0] : comboString.replace("[","").replace("]","").split(",");

        Card card = new Card(id, idString, name, rarity, effectString, album, collection, baseEnergy, basePower, effectMap, combosWith);

        // Caching
        this.cardsInMemory.put(index,card);

        return card;
    }

    public Card getCardByStringIndex (String stringIndex) {
        return this.getCard(this.idStringIndex.get(stringIndex));
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