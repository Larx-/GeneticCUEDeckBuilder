package Setup;

import Controlling.Main;
import Effects.E_Energy;
import Effects.E_Power;
import Effects.E_PowerForEach;
import Effects.Effect;
import Enums.Album;
import Enums.Collection;
import Enums.Who;
import GameElements.Target;
import PreProcessing.EffectChunkParser;
import PreProcessing.RegexPreProcessor;
import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import lombok.extern.log4j.Log4j2;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Log4j2
public class TSVtoCSVPreProcessor {

    Map<String, String> cardIdToNatLangEffectMap = new HashMap<>();
    Set<String> allCardNames = new HashSet<>();

    EffectChunkParser effectChunkParser;

    public void processTSVtoCSV() {
        this.mapAllEffects(RegexPreProcessor.formattedFile);
        this.effectChunkParser = new EffectChunkParser(this.allCardNames);

        List<String[]> cardsFromTSV = new ArrayList<>();
        CSVParser tsvParser = new CSVParserBuilder().withSeparator('\t').build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(RegexPreProcessor.regexFile)).withCSVParser(tsvParser).build()) {
            cardsFromTSV = reader.readAll();
        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        // Printing all collections to add to enum
        // printCollections(cardsFromTSV);

        List<String[]> processedCards = new ArrayList<>();
        processedCards.add(new String[]{"Id","IdString","Name","Lim","Rarity","Collection","Energy","Power","EffectDescription","Effect","CombosWithTODO"});

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

            // TODO: Maybe test no effects and combos to isolate influence of different rules better

            String effectsDesc = cardIdToNatLangEffectMap.get(idString);
            String combosWith = determineSimpleCombos(cardName, effects);

            String[] cardCSV = new String[]{String.valueOf(intId),idString,cardName,limited,rarity,collection,energy,power,effectsDesc,effects,combosWith};
            processedCards.add(cardCSV);
            intId++;
        }

//        int missingPatterns = effectParser.parser.getNumEffectsWithoutPattern();
//        int numPatterns = effectParser.parser.getNumPatterns();
//        int effMatched = 2700 - missingPatterns;
//        float avgEffPerPattern = (float) effMatched / numPatterns;
//        float avgMissingPattern = (float) missingPatterns / avgEffPerPattern;
//        log.debug("ATTENTION: " + missingPatterns + " effects without matching pattern found!");
//        log.debug(effMatched + " / 2700 effects have been matched using " + numPatterns + " patterns, resulting in " + avgEffPerPattern + " per pattern.");
//        log.debug("This means on average there are " + avgMissingPattern + " more patterns to write.\n\n\n");

        try (CSVWriter writer = new CSVWriter(new FileWriter(Main.cardsFile))) {
            writer.writeAll(processedCards);
        } catch (IOException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    private String determineSimpleCombos(String cardName, String effects) {
        List<Effect> parsedEffects = this.effectChunkParser.parseEffect(cardName, effects);
        String combos = "";

        if (parsedEffects != null) {
            for (Effect effect : parsedEffects) {
                if (effect.getTarget().getWho() == Who.SELF) {
                    if (effect instanceof E_Power) {
                        if (((E_Power) effect).getChangeBy() > 0) {
                            combos = this.appendCombo(combos, findTargetsAsString(effect.getTarget()), cardName);
                        }

                    } else if (effect instanceof E_PowerForEach) {
                        if (((E_PowerForEach) effect).getChangeBy() > 0) {
                            combos = this.appendCombo(combos, findTargetsAsString(effect.getTarget()), cardName);
                            combos = this.appendCombo(combos, findTargetsAsString(((E_PowerForEach) effect).getCountEach()), cardName);
                        }

                    } else if (effect instanceof E_Energy) {
                        if (((E_Energy) effect).getChangeBy() < 0) {
                            combos = this.appendCombo(combos, findTargetsAsString(effect.getTarget()), cardName);
                        }
                    }
                }
                // TODO: Also look through the conditions
            }
        }
        if (combos.length() == 0) {
            return "[]";
        } else {
            return "[" + combos.substring(0, combos.length()-1) + "]";
        }
    }

    private String appendCombo(String comboFull, String combo, String cardName) {
        if (!combo.equals("") && !combo.contains(cardName) && !comboFull.contains(combo)) {
            comboFull += combo;
        }
        return comboFull;
    }

    private String findTargetsAsString (Target target) {
        String combosWith = "";

        Collection collection = target.getCollection();
        if (collection != null && collection != Collection.NAN && collection != Collection.NAN_WS) {
            combosWith += collection + ",";
        }

        Album album = target.getAlbum();
        if (album != null && album != Album.NAN && album != Album.NAN_2 && album != Album.NAN_3) {
            combosWith += album + ",";
        }

        String name = target.getName();
        if (name != null && !name.equals("NULL") && !name.equals("null")) {
            combosWith += name + ",";
        }

        return combosWith;
    }

    private void mapAllEffects(String formattedFile) {
        List<String[]> cardsFromTSV = null;

        CSVParser tsvParser = new CSVParserBuilder().withSeparator('\t').build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(formattedFile)).withCSVParser(tsvParser).build()) {
            cardsFromTSV = reader.readAll();
        } catch (IOException | CsvException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }

        for (String[] cardTSV : cardsFromTSV) {
            String indexStr = cardTSV[0];
            String natLangEff = cardTSV[7];
            this.cardIdToNatLangEffectMap.put(indexStr, natLangEff);
            this.allCardNames.add(cardTSV[1].toLowerCase());
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
