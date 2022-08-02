package Setup;

import Effects.Effect;
import Enums.Album;
import Enums.TriggerTime;
import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import lombok.extern.log4j.Log4j2;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Log4j2
public class TSVtoCSVPreProcessor {

    public void processTSVtoCSV() {
        List<String[]> cardsFromTSV = new ArrayList<>();
        String defaultFilenameTSV = "src/main/resources/Cards/cards.tsv";
        String defaultFilenameCSV = "src/main/resources/Cards/cards.csv";

        CSVParser tsvParser = new CSVParserBuilder().withSeparator('\t').build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(defaultFilenameTSV)).withCSVParser(tsvParser).build()) {
            cardsFromTSV = reader.readAll();
        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        // Printing all collections to add to enum
        // printCollections(cardsFromTSV);

        // Collect all names to feed to the effect parser
        Set<String> allCardNames = new HashSet<>();
        for (String[] tsvCard : cardsFromTSV) {
            allCardNames.add(tsvCard[1].toLowerCase());
        }

        EffectParser effectParser = new EffectParser(allCardNames);
        List<String[]> processedCards = new ArrayList<>();
        processedCards.add(new String[]{"Id","IdString","Name","Lim","Rarity","Collection","Energy","Power","EffectDescription","EffectJSON","CombosWith"});

        // Translate all effects and write new file in expected format
//        List<String[]> subList = cardsFromTSV.subList(0, 10);
        int intId = 1;
        for (int i = 0; i < cardsFromTSV.size(); i++) {
            String[] tsvCard    = cardsFromTSV.get(i);
            String idString     = tsvCard[0];
            String cardName     = tsvCard[1];
            String collection   = tsvCard[2];
            String album        = tsvCard[3];
            String rarity       = tsvCard[4];
            String energy       = tsvCard[5];
            String power        = tsvCard[6];
            String effects      = tsvCard[7];

            cardName = cardName.replace("'","\"");

            String limited = rarity.contains("Ltd") ? "1" : "0";
            rarity = rarity.replace("Ltd ", "");
            rarity = rarity.replace("Lvl ", "");
            rarity = rarity.replace("Cft ", "");

            String jsonEffect = effectParser.translateEffects(effects.trim(), cardName);
            String combosWith = effectParser.getCombos(jsonEffect);
            if (jsonEffect != null) {
                // Make sure EffectParser can handle all added cases
                Map<TriggerTime, List<Effect>> effectsParsed = effectParser.parseEffects(jsonEffect);

                String[] cardCSV = new String[]{String.valueOf(intId),idString,cardName,limited,rarity,collection,energy,power,effects,jsonEffect,combosWith};
                processedCards.add(cardCSV);
                intId++;
            }
        }

        int missingPatterns = effectParser.parser.getNumEffectsWithoutPattern();
        int numPatterns = effectParser.parser.getNumPatterns();
        int effMatched = 2700 - missingPatterns;
        float avgEffPerPattern = (float) effMatched / numPatterns;
        float avgMissingPattern = (float) missingPatterns / avgEffPerPattern;
        log.debug("ATTENTION: " + missingPatterns + " effects without matching pattern found!");
        log.debug(effMatched + " / 2700 effects have been matched using " + numPatterns + " patterns, resulting in " + avgEffPerPattern + " per pattern.");
        log.debug("This means on average there are " + avgMissingPattern + " more patterns to write.\n\n\n");

        try (CSVWriter writer = new CSVWriter(new FileWriter(defaultFilenameCSV))) {
            writer.writeAll(processedCards);
        } catch (IOException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    public static void printCollections (List<String[]> cardsFromTSV) {
        Set<String> collections = new HashSet<>();
        for (String[] tsvCard : cardsFromTSV) {
            if (!collections.contains(tsvCard[2])) {
                collections.add(tsvCard[2]);

                String shortName = tsvCard[0].substring(0,tsvCard[0].indexOf("0"));
                String albumString = "Album.";

                Album album = Album.fromString(tsvCard[3]);
                switch (Objects.requireNonNull(album)) {
                    case ARTS_AND_CULTURE:  albumString += "ARTS_AND_CULTURE";  break;
                    case OCEANS_AND_SEAS:            albumString += "OCEANS";            break;
                    case SPACE:             albumString += "SPACE";             break;
                    case LIFE_ON_LAND:      albumString += "LIFE_ON_LAND";      break;
                    case HISTORY:           albumString += "HISTORY";           break;
                    case PALEONTOLOGY:      albumString += "PALEONTOLOGY";      break;
                    case SCIENCE:           albumString += "SCIENCE";           break;
                }

                // expected output:   AMAS ("Amazing Astronauts", Album.SPACE),
                System.out.println(shortName + " (\"" + tsvCard[2] + "\", " + albumString + "),");
            }
        }
    }
}
