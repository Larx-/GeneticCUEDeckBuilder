package PreProcessing;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class RegexPreProcessor {

    private static final String originalFile = "src/main/resources/Cards/PreParsing/cards_original.tsv";
    private static final String formattedFile = "src/main/resources/Cards/PreParsing/cards_formatted.tsv";
    public  static final String regexFile = "src/main/resources/Cards/PreParsing/cards_regex_effects.tsv";

    private static List<String> anomalies = new ArrayList<>();
    private static final int percToAnomalyFlag = 120; // Percent compared to average a replacement has to be, to be flagged as an anomaly

    public String processCardList() {
        // First fixing some general inconsistencies and oddities about the original .tsv
        String preFormatted = fixFormatting(readFullFile(originalFile));

        // Not done in cards.tsv but probably useful
        preFormatted = optionalFormatting(preFormatted);

        // Write formatted file
        writeFullFile(formattedFile, preFormatted);
        int charsBefore = countNotReplacedChars(preFormatted);

        // Regex EFFECT replacement
        preFormatted = replaceSpecialCases(preFormatted, false);
        preFormatted = cleanUp(preFormatted);
        preFormatted = replaceManualCases(preFormatted, false);
        preFormatted = cleanUp(preFormatted);
        preFormatted = replaceGeneratedCases(preFormatted, false);
        preFormatted = cleanUp(preFormatted);

        // Post-processing
        // TODO: Everything in brackets (ex. targets) need to be defined better

        preFormatted = cleanUp(preFormatted);

        // <DEBUG>

        // Count percentage of replaced
        int charsAfter = countNotReplacedChars(preFormatted);
        float percToReplace = (float) charsAfter * 100 / charsBefore;
        log.debug("");
        log.debug(String.format("Percentage left to replace: %.2f%%", percToReplace));
        log.debug("");

//        preFormatted = replaceAll(preFormatted, Pattern.compile("\\[.*?\\]"),"-", false);

//        printMissing(preFormatted, Pattern.compile("\\d+\\t\\d+\\t(.*?)\\["));
//        printMissing(preFormatted, Pattern.compile("]+(.+?)\\["));
//        printMissing(preFormatted, Pattern.compile("Target:(\\(.*?\\))"));

        // Output anomalies
        outputAnomalies(true);

        // </DEBUG>

        // Write regex replaced file
        writeFullFile(regexFile, preFormatted);
        return preFormatted;
    }

    private String replaceAll (String allRows, Pattern pattern, String replacement) {
        return replaceAll(allRows, pattern, replacement, true);
    }

    private String replaceAll (String allRows, Pattern pattern, String replacement, boolean doLog) {
        if (doLog) {
            log.debug("REGEX and REPLACEMENT");
            log.debug(pattern.pattern() + "\t -> \t " + replacement);
        }

        List<String> replacements = new ArrayList<>();
        int totalLength = 0;

        Matcher matcher = pattern.matcher(allRows);

        while(matcher.find()) {
            MatchResult matchResult = matcher.toMatchResult();
            int groupCount = matchResult.groupCount();

            String replaced = replacement;

            for (int i = 1; i <= groupCount; i++) {
                String replacedBy = matchResult.group(i) == null ? "" : matchResult.group(i);
                replaced = replaced.replace("$"+i, replacedBy);
            }

            replacements.add(replaced);
            totalLength += replaced.length();

            if (doLog) {
                log.debug(StringUtils.rightPad(matcher.group(), 60) + "\t -> \t " + StringUtils.rightPad(replaced, 60));
            }
        }

        float avgRepLen = (float) totalLength / replacements.size();
        for (String rep : replacements) {
            float repLenPerc = rep.length() * 100 / avgRepLen;
            if (repLenPerc >= percToAnomalyFlag) {
                anomalies.add(pattern.pattern() + "\t -> \t " + rep);
            }
        }

        if (doLog) {
            log.debug("");
        }
        return matcher.replaceAll(replacement);
    }

    public String readFullFile (String fileName) {
        StringBuilder fullFile = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                fullFile.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullFile.toString();
    }

    private void writeFullFile (String fileName, String content) {
        // Write formatted file to be processed using regex
        try (FileWriter fr = new FileWriter(fileName)) {
            fr.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String fixFormatting (String allRows) {
        // Fixing albums
        allRows = allRows.replace("Oceans and Seas","Oceans & Seas");
        allRows = allRows.replace("Oceans\t","Oceans & Seas\t");
        allRows = allRows.replace("Oceans cards","Oceans & Seas cards");
        allRows = allRows.replace("Arts and Culture","Arts & Culture");

        // Fixing collections
        allRows = allRows.replace("Angela Maxwell's Walking the World","Angela Maxwells Walking the World");
        allRows = allRows.replace("The Solar System","Solar System");
        allRows = allRows.replace("Stage and Screen","Stage & Screen");
        allRows = allRows.replace("Rites and Rituals","Rites & Rituals");
        allRows = allRows.replace("Hoaxes and Cons","Hoaxes & Cons");
        allRows = allRows.replace("The Roman Empire","Roman Empire");
        allRows = allRows.replace("Money, Money, Money","Money; Money; Money");
        allRows = allRows.replace("Molluscs, Worms and Water Bugs","Molluscs; Worms & Water Bugs");
        allRows = allRows.replace("Molluscs, Worms & Water Bugs","Molluscs; Worms & Water Bugs");
        allRows = allRows.replace("Donald R. Prothero's Story of Evolution","Donald R. Protheros Story of Evolution");
        allRows = allRows.replace("Sue Black's In The Bones","Sue Blacks In The Bones");

        // Fixing card names, could be done more clever
        allRows = allRows.replace("Bacchus, God of Wine","Bacchus; God of Wine");
        allRows = allRows.replace("Titan, Moon of Saturn","Titan; Moon of Saturn");
        allRows = allRows.replace("Titan,","Titan; Moon of Saturn,");
        allRows = allRows.replace("Pluto, Dwarf Planet","Pluto; Dwarf Planet");
        allRows = allRows.replace("Hellboy, the Regaliceratops","Hellboy the Regaliceratops");
        allRows = allRows.replace("Guggenheim Museum, Bilbao","Guggenheim Museum; Bilbao");
        allRows = allRows.replace("Rudolf II, Holy Roman Emperor","Rudolf II; Holy Roman Emperor");
        allRows = allRows.replace("William Thomson, 1st Baron Kelvin","William Thomson; 1st Baron Kelvin");
        allRows = allRows.replace("Neptune, God of Water","Neptune; God of Water");
        allRows = allRows.replace("Jupiter, King of the Gods","Jupiter; King of the Gods");
        allRows = allRows.replace("Parasauralophus","Parasaurolophus");
        allRows = allRows.replace("Romulus and Remus","Romulus & Remus");
        allRows = allRows.replace("Tanngrisnir and Tanngnjóstr","Tanngrisnir & Tanngnjóstr");
        allRows = allRows.replace("Huginn and Muninn","Huginn & Muninn");
        allRows = allRows.replace("Sol and Mani","Sol & Mani");
        allRows = allRows.replace("Vili and Vé","Vili & Vé");
        allRows = allRows.replace("Ask and Embla","Ask & Embla");
        allRows = allRows.replace("Geri and Freki","Geri & Freki");
        allRows = allRows.replace("Fe, Fi, Fo, Fum and Phooey","Fe; Fi; Fo; Fum & Phooey");
        allRows = allRows.replace("Ackee and Saltfish","Ackee & Saltfish");
        allRows = allRows.replace("Da Vinci, Master Artist","Da Vinci; Master Artist");
        allRows = allRows.replace("The Cowherd and the Weaver Girl","The Cowherd & the Weaver Girl");
        allRows = allRows.replace("Prince Rudolf and Saltfish","Prince Rudolf & Saltfish");
        allRows = allRows.replace("Ackee and Baroness Vetsera","Ackee & Baroness Vetsera");
        allRows = allRows.replace("Antony and Cleopatra","Antony & Cleopatra");
        allRows = allRows.replace("Queen Victoria and Prince Albert","Queen Victoria & Prince Albert");
        allRows = allRows.replace("Andrew Jackson and Rachel Donelson","Andrew Jackson & Rachel Donelson");
        allRows = allRows.replace("Ines de Castro and King Pedro","Ines de Castro & King Pedro");
        allRows = allRows.replace("Mary Godwin and Percy Shelley","Mary Godwin & Percy Shelley");
        allRows = allRows.replace("Napoleon and Josephine","Napoleon & Josephine");
        allRows = allRows.replace("Pocahontas and John Smith","Pocahontas & John Smith");
        allRows = allRows.replace("Heloise and Abelard","Heloise & Abelard");
        allRows = allRows.replace("Hadrian and Antinous","Hadrian & Antinous");
        allRows = allRows.replace("Harry Moore and Harriette Simms","Harry Moore & Harriette Simms");
        allRows = allRows.replace("Shah Jahan and Mumtaz Mahal","Shah Jahan & Mumtaz Mahal");
        allRows = allRows.replace("Bonnie and Clyde","Bonnie & Clyde");
        allRows = allRows.replace("Alexander I of Serbia and Draga Mašin","Alexander I of Serbia & Draga Mašin");
        allRows = allRows.replace("Caroline and George Norton","Caroline & George Norton");
        allRows = allRows.replace("Emperor Nero and Poppaea Sabina","Emperor Nero & Poppaea Sabina");
        allRows = allRows.replace("Lucrezia Borgia and Giovanni Sforza","Lucrezia Borgia & Giovanni Sforza");
        allRows = allRows.replace("Engelier and Gerin","Engelier & Gerin");
        allRows = allRows.replace("Prince Rudolf and Baroness Vetsera","Prince Rudolf & Baroness Vetsera");
        allRows = allRows.replace("Tezcatlipoca","Tetzcatlipoca");
        allRows = allRows.replace("Xipe Totec","XipeTotec");
        allRows = allRows.replace("Huītzilōpōchtli","Huitzilopochtli");
        // TODO: Automatic check for "and" and "," in names

        // Fixing miscellaneous
        allRows = allRows.replace("opponent","Opponent");
        allRows = allRows.replace(", your deck contains",", if your deck contains");
        allRows = allRows.replace("turn and next","turn & next");
        allRows = allRows.replace("in-hand","in hand");
        allRows = allRows.replace("When played, your [REDACTED] cards have [REDACTED] this turn.","When played, your Solar System cards have +50 Power this turn.");
        allRows = allRows.replace("When played, give all cards with a Base Power of 22 or less +22 Power until played.","When played, give all cards in hand with a Base Power of 22 or less +22 Power until played.");

        // Fixing burn to be written the same everywhere
        allRows = allRows.replaceAll(" P/T\\)",") ");
        allRows = allRows.replaceAll("[b|B]urns* *\\(-*(.*?)\\)"," BURN ($1)");
        allRows = allRows.replaceAll("BURN \\(-(.*?)\\)"," BURN ($1)");
        allRows = allRows.replaceAll(" [b|B]urn for (\\d)"," BURN ($1)");

        // Fixing all types of " in deck" -> there are around 15-20 different ways of writing this!
        allRows = allRows.replaceAll("\\(including those .*?\\)","");
        allRows = allRows.replaceAll("\\([e|E]ven .*?\\)", "");
        allRows = allRows.replaceAll("\\(wherever .*?\\)", "");

        // Removing unnecessary
        allRows = allRows.replaceAll("\\(coming soon.*?\\)","");

        // Fixing name contains "___" to '___'
        allRows = allRows.replaceAll("\"(.*?)\"", "'$1'");

        // Fixing whitespaces and end of line things, always last
        allRows = allRows.replace("\\n"," ");
        allRows = allRows.replaceAll("  +"," ");
        allRows = allRows.replace(" \n","\n");
        allRows = allRows.replace(",\n","\n");

        return allRows;
    }

    private String optionalFormatting (String allRows) {
        allRows = allRows.replace("Cmmn","Common");
        allRows = allRows.replace("Lgnd","Legendary");
        allRows = allRows.replace("Ult-Fusn","Ultra-Fusion");
        allRows = allRows.replace("Fusn","Fusion");

        allRows = allRows.replace("for this turn & next turn","this turn & next");
        allRows = allRows.replace("for this turn & next","this turn & next");
        allRows = allRows.replace("this turn & next turn","this turn & next");

        allRows = allRows.replace("\"","'");
        allRows = allRows.replaceAll("[eE]nergy [pP]er [tT]urn","Energy/Turn");
        allRows = allRows.replaceAll("[pP]ower [pP]er [tT]urn","Power/Turn");

        allRows = allRows.replace("On returned to your deck, if you won the turn, gain +5 energy next turn.","When returned to your deck, if you won the turn, gain +5 energy next turn.");
        allRows = allRows.replace("When played with Trigonometry, give it+30 Power this turn.","When played with Trigonometry, give it +30 Power this turn.");
        allRows = allRows.replace("When played with Green Lynx Spider, give it+50 Power this turn.","When played with Green Lynx Spider, give it +50 Power this turn.");
        allRows = allRows.replace("When played on the first turn of a round, gain +1 Energy/Turn for the rest of it. If you lose the turn, lose it and gain +15 Power next turn.","When played on the first turn of a round, gain +1 Energy/Turn for the rest of the round. If you lose this turn, lose it and gain +15 Power next turn.");
        allRows = allRows.replace("When drawn, gain +10 Power/Turn and 1 Energy/Turn until this card is played. If you have played Electricity, when drawn, gain an additional +10 Power/ Turn until this card is played.\n", "When drawn, gain +10 Power/Turn and 1 Energy/Turn until this card is played. When drawn, if you have played Electricity, gain an additional +10 Power/ Turn until this card is played.\n");
        allRows = allRows.replace("When played, give this card+40 Power this turn.","When played, give this card +40 Power this turn.");
        allRows = allRows.replace("When drawn, give a random card in your opponent's hand -5 Power and give this card +10 Power for this round.","When drawn, give a random card in your opponent's hand -5 Power and give this card +10 Power this round.");
        allRows = allRows.replace("When played, gain+3 Power/Turn for the rest of the game.","When played, gain +3 Power/Turn for the rest of the game.");
        allRows = allRows.replace("energy/Turn","Energy/Turn");
        allRows = allRows.replace("On return,","When returned to your deck,");
        allRows = allRows.replace("When drawn, give a random card in your Opponent's hand -5 Power and give this card +10 Power for this round.","When drawn, give a random card in your Opponent's hand -5 Power and give this card +10 Power this round.");

        return allRows;

//        allRows = allRows.replace("","");
    }

    private String replaceSpecialCases (String preFormatted, boolean doLogs) {
        preFormatted = replaceAll(preFormatted, Pattern.compile("BURN \\((\\d+?)\\) all cards - yours until next turn, your Opponent's until played\\."),
                "[Effect:BURN, Value:15] [Target:SELF, Where:CARDS_IN_HAND] [Duration:TIMER, Value:1] [Effect:BURN, Value:15] [Target:OTHER, Where:CARDS_IN_HAND] [Duration:UNTIL_PLAYED] ",  doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When drawn, gain \\+10 Power/Turn and 1 Energy/Turn until this card is played\\. If you have played Electricity, when drawn, gain an additional \\+10 Power/Turn until this card is played\\."),
                "[TriggerTime:DRAW] [Target:SELF] [Effect:POWER_PER_TURN, Value:+10] [Effect:ENERGY_PER_TURN, Value:+1] [Duration:WHILE_IN_HAND] [TriggerTime:DRAW] [Target:SELF] [Effect:POWER_PER_TURN, Value:+10] [Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Electricity]",  doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When played, if you have played Pika, Prairie Dog, Fennec Fox or Angora Rabbit at least once, give this card \\+20 for each card played this turn\\."),
                "[TriggerTime:PLAY] [Target:THIS] [Effect:POWER, Value:+20] [Duration:END_TURN] [Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Pika] " +
                        "[TriggerTime:PLAY] [Target:THIS] [Effect:POWER, Value:+20] [Duration:END_TURN] [Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Prairie Dog] " +
                        "[TriggerTime:PLAY] [Target:THIS] [Effect:POWER, Value:+20] [Duration:END_TURN] [Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Angora Rabbit] " +
                        "[TriggerTime:PLAY] [Target:THIS] [Effect:POWER, Value:+20] [Duration:END_TURN] [Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Fennec Fox] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When played, give yourself -2 Energy/Turn for the rest of the round\\. If you win the turn, gain \\+5 Energy/Turn for the rest of the round\\."),
                "[TriggerTime:PLAY] [Target:SELF] [Effect:ENERGY_PER_TURN, Value:-2] [Duration:END_ROUND] " +
                        "[TriggerTime:RETURN] [Target:SELF] [Effect:ENERGY_PER_TURN, Value:5] [Duration:END_ROUND] [Condition:TURN_STATE, Value:Win]", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When returned to your deck, give whoever won this turn \\+36 Power/Turn next turn\\."),
                "[TriggerTime:RETURN] [Target:SELF] [Effect:POWER_PER_TURN, Value:+36] [Duration:TIMER, Value:1] [Condition:TURN_STATE, Value:Win] " +
                        "[TriggerTime:RETURN] [Target:OTHER] [Effect:POWER_PER_TURN, Value:+36] [Duration:TIMER, Value:1] [Condition:TURN_STATE, Value:Loss]", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When played, give you and your Opponent \\+22 Power/Turn this turn\\. If you lose the turn, you keep the \\+22 power until the end of the round\\. If you win the turn, give your Opponent \\+22 power/turn until the end of the round\\."),
                "[TriggerTime:PLAY] [Target:BOTH] [Effect:POWER_PER_TURN, Value:+22] [Duration:END_TURN] " +
                        "[TriggerTime:RETURN] [Target:SELF] [Effect:POWER_PER_TURN, Value:+22] [Duration:END_ROUND] [Condition:TURN_STATE, Value:Win] " +
                        "[TriggerTime:RETURN] [Target:OTHER] [Effect:POWER_PER_TURN, Value:+22] [Duration:END_ROUND] [Condition:TURN_STATE, Value:Loss]", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When returned to your deck, give whoever won this turn \\+36 Power/Turn next turn\\."),
                "[TriggerTime:RETURN] [Target:SELF] [Effect:POWER_PER_TURN, Value:+36] [Duration:TIMER, Value:1] [Condition:TURN_STATE, Value:Win] " +
                        "[TriggerTime:RETURN] [Target:OTHER] [Effect:POWER_PER_TURN, Value:+22] [Duration:TIMER, Value:1] [Condition:TURN_STATE, Value:Loss]", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When played on a matching Arena, steal two Energy from your Opponent when it returns to your deck\\."),
                "[TriggerTime:RETURN] [Target:SELF] [Effect:ENERGY, Value:2] [Condition:ARENA_MATCHING] [TriggerTime:RETURN] [Target:OTHER] [Effect:ENERGY, Value:-2] [Condition:ARENA_MATCHING]", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When returned to your deck, give your Amphibians cards \\+14 Power this round \\(and if your deck contains Glass Frog or Axolotl, give them an extra \\+12 Power for each\\)\\."),
                "[TriggerTime:RETURN] [Target:(your Amphibians cards)] [Effect:POWER, Value:+14] [Duration:END_ROUND] " +
                        "[TriggerTime:RETURN] [Condition:DECK_CONTAINS, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Glass Frog] [Target:(your Amphibians cards)] [Effect:POWER, Value:+12]" +
                        "[TriggerTime:RETURN] [Condition:DECK_CONTAINS, Who:SELF, Where:CARDS_IN_DECK, CompareTo:Axolotl] [Target:(your Amphibians cards)] [Effect:POWER, Value:+12]", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("\\(and, if played on the Halloween Arena, give them an extra \\+40 Power\\)"), "", doLogs);    // Almost never happens, as it is only once a year
        preFormatted = replaceAll(preFormatted, Pattern.compile("Use Thunderstorms to awaken the monster"), "NULL", doLogs);                                        // Not sure what effect this has

        preFormatted = replaceAll(preFormatted, Pattern.compile("When drawn, Burn \\(23\\) 3 random cards in your Opponent's hand, and 1 random card in your hand, until played\\."),
                "", doLogs);

        return preFormatted;
    }

    private String replaceManualCases (String preFormatted, boolean doLogs) {
        // TriggerTime
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hile in your hand, at the start of (your|each) turn,"),"[TriggerTime:START] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen drawn,|[o|O]n draw,"),"[TriggerTime:DRAW] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played, if you are winning the round,"),"[TriggerTime:PLAY] [Condition:ROUND_STATE, Value:Win] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played on the first turn of a round,"),"[TriggerTime:PLAY] [Condition:TURN_IN_ROUND, Value:1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played,"),"[TriggerTime:PLAY] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([w|W]hen played with |When played alongside |[w|W]hen played adjacent to |[w|W]hen played next to |[i|I]f played next to |[i|I]f played alongside )(.*?),"),"[TriggerTime:PLAY] [Condition:PLAYED_WITH, Value:($2)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played in the middle slot and adjacent to (.*?),"),"[TriggerTime:PLAY] [Condition:PLAYED_WITH, Value:($1)] ", doLogs); // NOTE: Here should be another condition, that I skipped because I only use random agents
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played in the right slot and adjacent to (.*?),"),"[TriggerTime:PLAY] [Condition:PLAYED_WITH, Value:($1)] ", doLogs); // NOTE: Here should be another condition, that I skipped because I only use random agents
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played in the(.*?)slot"),"[TriggerTime:PLAY] ", doLogs); // NOTE: Here should be another condition, that I skipped because I only use random agents

        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen returned to your deck|[w|W]hen returned to the deck|[w|W]hen returned to deck|[w|W]hen this card returns to your deck"),"[TriggerTime:RETURN] ", doLogs);


        // Effect
        preFormatted = replaceAll(preFormatted, Pattern.compile("[t|T]his [c|C]ard has (.?\\d+?) [p|P]ower\\."),"[Effect:POWER, Value:$1] [Target:THIS] [Duration:END_TURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[a|A]ll your [c|C]ards have (.?\\d+?) [p|P]ower this turn\\."),"[Target:[Who:SELF, Where:CARDS_IN_HAND]] [Effect:POWER, Value:$1] [Duration:END_TURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[a|A]ll (.*?) [c|C]ards have (.?\\d+?) [p|P]ower this turn\\."),"[Target:[Who:BOTH, Where:CARDS_IN_HAND, CompareTo:($1)]] [Effect:POWER, Value:$2] [Duration:END_TURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[a|A]ll (.*?) [c|C]ards have (.?\\d+?) [p|P]ower"),"[Target:[Who:BOTH, Where:CARDS_IN_DECK, CompareTo:($1)]] [Effect:POWER, Value:$2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[r|R]educe the [e|E]nergy [c|C]ost of (.*?) by .?(\\d+?).?"),"[Effect:ENERGY, Value:-$2] [Target:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[f|F]or each (.*?) [c|C]ard in your deck.*maximum of (\\d+?)\\).*give this [c|C]ard (.?\\d+?) [p|P]ower this turn,"),"[Effect:POWER_FOR_EACH, Value:$3, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)], UpTo:$2] [Target:THIS] [Duration:END_TURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([g|G]ain|[g|G]ive yourself|[g|G]et|[y|Y]ou have|[g|G]ain an extra|[r|R]eceive) ([+-|]\\d+) [e|E]nergy/[t|T]urn"),"[Target:SELF] [Effect:ENERGY_PER_TURN, Value:$2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[r|R]educe the [e|E]nergy [c|C]ost of your (.*?) [c|C]ards by (.*?) "),"[Effect:ENERGY, Value:-$2] [Target:[Who:SELF, What:CARDS_IN_DECK, CompareTo:($1)]] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ive this [c|C]ard (.?\\d+?) [p|P]ower"),"[Target:THIS] [Effect:POWER, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ive your Opponent (.?\\d+?) [p|P]ower/[t|T]urn"),"[Target:OTHER] [Effect:POWER_PER_TURN, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ive your Opponent (.?\\d+?) [e|E]nergy\\/[t|T]urn"),"[Target:OTHER] [Effect:ENERGY_PER_TURN, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ive (.*?) (.?\\d+?) [p|P]ower(?!/)"),"[Target:($1)] [Effect:POWER, Value:$2] ", doLogs);


        // Conditions
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing the [r|R]ound by (\\d+).*?more.*?,"),"[Condition:ROUND_STATE, Value:Loss>=$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing the [r|R]ound by (\\d+).*?less.*?,"),"[Condition:ROUND_STATE, Value:Loss<=$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing the [r|R]ound,"),"[Condition:ROUND_STATE, Value:Loss] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning the [r|R]ound by (\\d+).*?more.*?,"),"[Condition:ROUND_STATE, Value:Win>=$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning the [r|R]ound by (\\d+).*?less.*?,"),"[Condition:ROUND_STATE, Value:Win<=$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning the [r|R]ound,"),"[Condition:ROUND_STATE, Value:Win] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you (\\w*) (this|the) turn"),"[Condition:TURN_STATE, Value:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f your deck contains (\\d+?) or more (.*?) and (\\d+?) or more (.*?),"),"[Condition:DECK_CONTAINS, Value:>=$1, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)]] [Condition:DECK_CONTAINS, Value:>=$3, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($4)]] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f your deck contains (\\d+?) or more (.*?)(( card,| cards,)*?),"),"[Condition:DECK_CONTAINS, Value:>=$1, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)]] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you have played (.*?) this game"),"[Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f (.*?) has been played this game"),"[Condition:PLAYED_BEFORE, Who:BOTH, Where:CARDS_IN_DECK, CompareTo:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f your deck contains (.*?) and you have already played (.*?),"),"[Condition:DECK_CONTAINS, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)] [Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f your deck contains (.*?)(,|\\[)"),"[Condition:DECK_CONTAINS, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)] $2", doLogs);

        // Duration
        preFormatted = replaceAll(preFormatted, Pattern.compile("[f|F]or the [r|R]est of the [g|G]ame"),"[Duration:PERMANENT] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[u|U]ntil the [e|E]nd of the [g|G]ame"),"[Duration:PERMANENT] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[u|U]ntil the [e|E]nd of (the|this) [r|R]ound"),"[Duration:END_ROUND] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("(([u|U]ntil next turn)|([t|T]his [t|T]urn & [n|N]ext))(\\.|)"),"[Duration:TIMER, Value:1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([u|U]ntil played|[u|U]ntil(.*?)played)"),"[Duration:UNTIL_PLAYED] ", doLogs);

        // FIXME: ARE THERE MORE PROBLEMS LIKE THIS ONE?!
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you lost this [t|T]urn"),"[Condition:TURN_STATE, Value:Loss] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("([t|T]his [t|T]urn)(\\.|)"),"[Duration:END_TURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("(for the rest of|) [t|T]his [r|R]ound(\\.|)"),"[Duration:END_ROUND] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[u|U]ntil [g|G]ame [e|E]nd(\\.|)"),"[Duration:PERMANENT] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[p|P]ermanently(\\.|)"),"[Duration:PERMANENT] ", doLogs);


        preFormatted = cleanUp(preFormatted);


        // REST OF THE THINGS, STARTING HERE TO AVOID SIDE EFFECTS FROM SORTING
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you have won one [r|R]ound this [g|G]ame, "),"[Condition:ROUNDS_WON, Value:1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hen played with (.*?) \\["),"[TriggerTime:PLAY] [Condition:PLAYED_WITH, Value:($1)] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("for the [r|R]est of the [r|R]ound"),"[Duration:END_ROUND] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[b|B]oth.*[g|G]ain (.?\\d+?) [e|E]nergy"),"[Target:BOTH] [Effect:ENERGY, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ain (.?\\d+?) [e|E]nergy"),"[Target:SELF] [Effect:ENERGY, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[r|R]educe its Energy cost by (.?)(\\d+?)"),"[Effect:ENERGY, Value:-$2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([g|G]ive yourself|[g|G]ain|[y|Y]ou have) (.?\\d+?) [p|P]ower/[t|T]urn"),"[Target:SELF] [Effect:POWER_PER_TURN, Value:$2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([g|G]ive both players|[g|G]ive each player|[g|G]ive yourself and your Opponent) (.?\\d+?) [p|P]ower/[t|T]urn"),"[Target:BOTH] [Effect:POWER_PER_TURN, Value:$2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([s|S]teal) (.?\\d+?) [p|P]ower/[t|T]urn from your Opponent"),"[Target:SELF] [Effect:POWER_PER_TURN, Value:$2] [Target:SELF] [Effect:POWER_PER_TURN, Value:-$2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ive this [c|C]ard (.?\\d+?) [p|P]ower"),"[Target:THIS] [Effect:POWER_PER_TURN, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[g|G]ive this [c|C]ard (.?\\d+?) \\["),"[Target:THIS] [Effect:POWER_PER_TURN, Value:$1] [", doLogs);

        // All other TriggerTimes
        preFormatted = replaceAll(preFormatted, Pattern.compile("When drawn and returned to your deck, "),"[TriggerTime:DRAW] [TriggerTime:RETURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When drawn or played, "),"[TriggerTime:DRAW] [TriggerTime:PLAY] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When drawn "),"[TriggerTime:DRAW] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When this returns to your deck, "),"[TriggerTime:RETURN] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When played\\["),"[TriggerTime:PLAY] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When played |When you play this,|When played\\."),"[TriggerTime:PLAY] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("If drawn "),"[TriggerTime:DRAW] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("On play "),"[TriggerTime:PLAY] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("first turn of the round"),"[Condition:TURN_IN_ROUND, Value:1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if you have played (.*?) at least once,"),"[Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if you have played Urðr, Verðandi and Skuld,"),"[Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:(Urðr, Verðandi and Skuld)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you have played(.*?), "),"[Condition:PLAYED_BEFORE, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("While in your hand, (.*?) have (.?\\d+?) Power"),"[TriggerTime:DRAW] [Target:($1)] [Effect:POWER, Value:$2] [Duration:WHILE_IN_HAND] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] your (.*?) cards have (.\\d*?) Power \\["),"[Target:($1)] [Effect:POWER, Value:$2] [", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("(RETURN.*) next turn"),"$1 [Duration:TIMER, Value:1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("(([a|A]t the start.*?)|)[w|W]hile(.*?)hand(.*?)[a|A]t the.*?(turn.)"),"[TriggerTime:START] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("At the start of each turn\\,"),"[TriggerTime:START] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("(at this |[a|A]t the )start.*?turn."),"[TriggerTime:START] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[w|W]hile(.*?)hand(.*?)."),"[TriggerTime:START] [Duration:END_TURN] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("[f|F]or (every|each) (.*?) in your deck.(.*?)([\\[|\\.])"),"[Effect:FOR_EACH_TODO, Values:($2) ($3)] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("for ([a-zA-Z\\d]*?) turn."),"[Duration:TIMER, Values:($1)] ", doLogs);

        return preFormatted;
    }

    // If I ever have some time I should make these less stupid and pretty close to one replacement per pattern...
    private String replaceGeneratedCases (String preFormatted, boolean doLogs) {
        preFormatted = replaceAll(preFormatted, Pattern.compile("BURN \\((\\d+)\\) (\\[)"),"[Effect:BURN, Value:$1] [Target:THIS] $2", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("BURN \\((\\d+)\\) (.*?) (\\[|and)"),"[Effect:BURN, Value:$1] [Target:($2)] $3", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("for (\\d+) [t|T]urns"),"[Duration:TIMER, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("\\].{0,10}gain (.?\\d+) Power \\["),"] [Effect:POWER_PER_TURN, Value:$1] [Target:[Who:SELF]] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("\\].{0,10}reduce (.*?) [e|E]nergy (.*?) by (.?\\d+) (\\[|and)"),"] [Effect:ENERGY, Value:-$3] [Target:($1)] $4", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("and (.?\\d+?) Energy/Turn"),"[Effect:ENERGY_PER_TURN, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[c|C]ost (.?\\d+?) [e|E]nergy less"),"[Effect:ENERGY, Value:-$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[c|C]ost (.?\\d+?) [e|E]nergy"),"[Effect:ENERGY, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("for the next (.*?) turns"),"[Duration:TIMER, Value:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if it contains (\\d*?) or more (.*?) cards."),"[Condition:DECK_CONTAINS, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2), Value:>=$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("increase the ([E|e]nergy |)cost of (.*?) card(s|) by (\\d+)"),"[Effect:ENERGY, Value:(+$4)] [Target:($2)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if you're winning the round"),"[Condition:ROUND_STATE, Value:Win] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if you're losing the round"),"[Condition:ROUND_STATE, Value:Loss] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("your cards have (.?\\d+) Power"),"[Effect:POWER, Value:$1] [Target:[Who:SELF, What:CARDS_IN_DECK]] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("[^(]cards left in your hand[^)]"),"[Target:[Who:SELF, What:CARDS_IN_HAND]] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("\\(with target preview\\)\\."),"", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([i|I]f it is|[i|I]f it’s|[o|O]n) the first [t|T]urn of a [r|R]ound"),"[Condition:TURN_IN_ROUND, Value:1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile(", and and ")," ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] ,"),"]", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] Energy \\["),"] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] [i|I]f it is the \\["),"] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f it is the (.*?) turn of (\\w*) round"),"[Condition:TURN_IN_ROUND, Value:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("If you (lost|lose)"),"[Condition:TURN_STATE, Value:Loss] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing"),"[Condition:ROUND_STATE, Value:Loss] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("reduce the [c|C]ost of (.*?) by (\\d+) [e|E]nergy"),"[Effect:ENERGY, Value:-$2] [Target:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("your Opponents cards burn in hand \\(10\\) until this card is returned your deck\\."),"[Effect:BURN, Value:10] [Target:[Who:OTHER, Where:CARDS_IN_HAND]] [Duration:WHILE_IN_HAND] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("BURN \\((\\d+)\\) (.*?)( again\\.| instead|\\[)"),"[Effect:BURN, Value:$1] [Target:($2)] $3", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f it is [r|R]ound 5, Turn 3,"),"[Condition:AFTER_ROUND, Value:4] [Condition:TURN_IN_ROUND, Value:3]", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f it is [r|R]ound 1,"),"[Condition:BEFORE_ROUND, Value:2] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f it is [r|R]ound 5,"),"[Condition:AFTER_ROUND, Value:4] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f it's after [r|R]ound (\\d),"),"[Condition:AFTER_ROUND, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f it's the"),"", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f played after [r|R]ound 3, Turn 2, "),"[Condition:AFTER_TURN, Value:8] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f played after turn 6"),"[Condition:AFTER_TURN, Value:6]", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f played with (.*?),"),"[Condition:PLAYED_WITH, Value:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f (you're|you are) winning the [r|R]ound and your deck contains (.*?),"),"[Condition:ROUND_STATE, Value:Win] [Condition:DECK_CONTAINS, Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning by (\\d+) (Power or more|or more Power)"),"[Condition:ROUND_STATE, Value:Win>=$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning,"),"[Condition:ROUND_STATE, Value:Win] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you have lost (.*) [r|R]ound(s this game,|,|s,|s of this game,)"),"[Condition:ROUNDS_LOST, Value:($1)] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you win( a turn this is played on,|, )"),"[Condition:TURN_STATE, Value:Win] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("( and |)[i|I]f you're losing the [r|R]ound.(too,|)"),"[Condition:ROUND_STATE, Value:Loss] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f (you're|you are) (.*) the round."),"[Condition:ROUND_STATE, Value:($2)] ", doLogs);

        preFormatted = replaceAll(preFormatted, Pattern.compile("(lock|Lock|LOCK) (.*?) (\\[|and|,|for|instead.)"),"[Effect:LOCK] [Target:($2)] $3", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("Reveal after scoring."),"", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[r|R]epeat(.*?)(\\.)"),"[Repeat:($1)] ", doLogs);      // TODO: Repeat
        preFormatted = replaceAll(preFormatted, Pattern.compile("[r|R]epeat( this| )"),"[Repeat:($1)] ", doLogs);       // TODO: Repeat
        preFormatted = replaceAll(preFormatted, Pattern.compile("] your cards in hand \\["),"] [Target:[Who:SELF, Where:CARDS_IN_HAND]] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] your Opponent's cards( in hand|) \\["),"] [Target:[Who:OTHER, Where:CARDS_IN_HAND]] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("your Opponent has (-\\d*) Power/Turn"),"[Target:OTHER] [Effect:POWER_PER_TURN, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("your Opponent has (.*?) Power "),"[Target:OTHER] [Effect:POWER_PER_TURN, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] *also\\.*"),"]", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[d|D]o the same (.*?)\\."),"[Repeat:($1)] ", doLogs);  // TODO: Repeat
        preFormatted = replaceAll(preFormatted, Pattern.compile("] instead."),"]", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("If are losing the round"),"[Condition:ROUND_STATE, Value:Loss] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("] [e|E]nergy \\["),"] [", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("(\\t.*?)(\\t)(.*?)(\\t.*?)(\\t.*?)(\\t.*?)(\\t.*?)(\\t.*?)([i|I]f played |[i|I]f you play this |)on a matching [a|A]rena."),"$1$2$3$4$5$6$7$8[Condition:MATCHING_ARENA, Value:$3] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("([^(even)]) if you have (\\d+) or more (.*) in your deck,"),"$1 [Condition:DECK_CONTAINS, Value:>=$2, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($3)]] ", doLogs); // FIXME: Leaves
        preFormatted = replaceAll(preFormatted, Pattern.compile("([^(even)]) if you have (.*) in your deck,"),"$1 [Condition:DECK_CONTAINS, Value:>=1, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)]] ", doLogs); // FIXME: Leaves
        preFormatted = replaceAll(preFormatted, Pattern.compile("all cards have (-\\d+) [p|P]ower "),"[Effect:POWER, Value:$1] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile(".ou have (.*?) .nergy"),"[Effect:ENERGY, Value:$1] ", doLogs);
        // TODO: When returned to your deck, you have -4 Energy next turn.  -->  Should not give back Energy...
        preFormatted = replaceAll(preFormatted, Pattern.compile("if your deck has 5 or more Space cards,"),"[Condition:DECK_CONTAINS, Value:>=5, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:(Space)]] ", doLogs);
        preFormatted = replaceAll(preFormatted, Pattern.compile("get (.\\d+) Power\\/Turn"),"[Target:SELF] [Effect:POWER_PER_TURN, Value:$1] ", doLogs);

//        preFormatted = replaceAll(preFormatted, Pattern.compile(""),"", doLogs);
        // For COPY & PASTE


        return preFormatted;
    }

    private String cleanUp(String allRows) {
        allRows = replaceAll(allRows, Pattern.compile("--+"),"-", false);
        allRows = replaceAll(allRows, Pattern.compile("  +")," ", false);
        allRows = replaceAll(allRows, Pattern.compile("\\] \\, \\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("\\] \\."),"]", false);
        allRows = replaceAll(allRows, Pattern.compile("\\] \\)\\."),"]", false);
        allRows = replaceAll(allRows, Pattern.compile("]\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] *, *\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] *and *\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] *, and *\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] *and, *\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] *\\(and *\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] *\\(and, *\\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("\\[\\\n"),"\n", false);
        allRows = replaceAll(allRows, Pattern.compile("\\[TriggerTime:PLAY\\["),"[TriggerTime:PLAY] [", false);
        allRows = replaceAll(allRows, Pattern.compile("] on the \\["),"] [", false);
        allRows = replaceAll(allRows, Pattern.compile("!\n"),"\n", false);
        return allRows;
    }

    private int countNotReplacedChars(String allRows) {
        int numChars = 0;
        String[] rows = allRows.split("\n");

        for (String row : rows) {
            row = row.substring(row.lastIndexOf("\t"));
            row = row.replaceAll("\\[.*\\]","");
            row = row.replaceAll("- ","");

            if (!row.matches("NULL")) {
                numChars += row.length();
            }
        }

        return numChars;
    }

    private void printMissing (String allRows, Pattern pattern) {
        log.debug("Not in [brackets] effect snippets based on '" + pattern.pattern() + "' Pattern: ");

        List<String> missing = new ArrayList<>();

        Matcher matcher = pattern.matcher(allRows);
        while(matcher.find()) {
            String group1 = matcher.group(1);

            if (!group1.equals("") && !group1.equals(" ") && !group1.equals("] ")) {
                String important = "     ";
                if (group1.contains("]") || group1.contains(" /Turn") || group1.contains(" /turn")) {
                    important = " !!! ";
                }
                String toAdd = "Missing:" + important + "'" + group1 + "'" + important;

                if (!missing.contains(toAdd)) {
                    missing.add(toAdd);
                }
            }
        }

        Collections.sort(missing);
        for (String m : missing) {
            log.debug(m);
        }

        log.debug(" -----> " + missing.size() + " <----- ");
        log.debug("");
    }

    private static void outputAnomalies (boolean onlyNumber) {
        log.debug("FOUND " + anomalies.size() + " ANOMALIES:");
        if (!onlyNumber) {
            for (String ano : anomalies) {
                log.debug(ano);
            }
        }
        log.debug("");
    }
}
