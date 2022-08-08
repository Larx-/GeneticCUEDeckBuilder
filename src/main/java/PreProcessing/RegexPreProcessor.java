package PreProcessing;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class RegexPreProcessor {

    private static final String originalFile = "src/main/resources/Cards/PreParsing/cards_original.tsv";
    private static final String formattedFile = "src/main/resources/Cards/PreParsing/cards_formatted.tsv";
    private static final String regexFile = "src/main/resources/Cards/PreParsing/cards_regex_effects.tsv";

    private static List<String> anomalies = new ArrayList<>();
    private static final int percToAnomalyFlag = 120; // Percent compared to average a replacement has to be, to be flagged as an anomaly

    public static void main(String[] args) throws IOException {
        // First fixing some general inconsistencies and oddities about the original .tsv
        String preFormatted = fixFormatting(readFullFile(originalFile));

        // Not done in cards.tsv but probably useful
        preFormatted = optionalFormatting(preFormatted);

        // Write formatted file
        writeFullFile(formattedFile, preFormatted);
        int charsBefore = countNotReplacedChars(preFormatted);

        // Regex EFFECT replacement
        // TriggerTime
        preFormatted = replaceAll(preFormatted, Pattern.compile("While in your hand, at the start of (your|each) turn,"),"[TriggerTime:START] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When drawn,"),"[TriggerTime:DRAW] ", false);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When played, if you are winning the round,"),"[TriggerTime:PLAY] [Condition:ROUND_STATE, Value:Win] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When played on the first turn of a round,"),"[TriggerTime:PLAY] [Condition:TURN_IN_ROUND, Value:1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When played,"),"[TriggerTime:PLAY] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("When played with (.*?),|When played adjacent to (.*?),|When played next to (.*?),"),"[TriggerTime:PLAY] [Condition:PLAYED_WITH, Value:($1$2$3)] ", false);

        preFormatted = replaceAll(preFormatted, Pattern.compile("When returned to your deck|When returned to the deck|When returned to deck"),"[TriggerTime:RETURN] ", false);


        // Effect
        preFormatted = replaceAll(preFormatted, Pattern.compile("this card has ([+|-]\\d+?) Power\\."),"[Effect:POWER, Value:$1] [Target:THIS] [Duration:END_TURN] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("all your cards have ([+|-]\\d*) Power this turn\\."),"[Target:[Who:SELF, Where:CARDS_IN_HAND]] [Effect:POWER, Value:$1] [Duration:END_TURN] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("all (.*?) cards have ([+|-]\\d*) Power this turn\\."),"[Target:[Who:BOTH, Where:CARDS_IN_HAND, CompareTo:($1)]] [Effect:POWER, Value:$2] [Duration:END_TURN] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("all (.*?) cards have ([+|-]\\d*) Power"),"[Target:[Who:BOTH, Where:CARDS_IN_DECK, CompareTo:($1)]] [Effect:POWER, Value:$2] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("reduce the energy cost of (.*?) by (.*?)[,]* "),"[Effect:ENERGY, Value:-$2] [Target:($1)]] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("for each (.*?) card in your deck.*maximum of (\\d*?)\\).*give this card ([+|-]\\d*?) Power this turn, false\\."),"[Effect:POWER_FOR_EACH, Value:$3, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($1)], UpTo:$2] [Target:THIS] [Duration:END_TURN] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("(gain|give yourself|get|you have|gain an extra) ([+|-]\\d*) Energy/Turn"),"[Target:SELF] [Effect:ENERGY_PER_TURN, Value:$2] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("reduce the Energy [c|C]ost of your (.*?) [c|C]ards by (.*?) "),"[Effect:ENERGY, Value:-$2] [Target:[Who:SELF, What:CARDS_IN_DECK, CompareTo:($1)]]] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("give this card ([+|-]\\d*) Power"),"[Target:THIS] [Effect:POWER, Value:$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("give your Opponent ([+|-]\\d*) [p|P]ower\\/[t|T]urn"),"[Target:OTHER] [Effect:POWER_PER_TURN, Value:$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("give your Opponent ([+|-]\\d*) [e|E]nergy\\/[t|T]urn"),"[Target:OTHER] [Effect:ENERGY_PER_TURN, Value:$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("give (.*?) ([+|-]\\d*) [p|P]ower"),"[Target:($1)] [Effect:POWER, Value:$2] ", false);


        // Conditions
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing the [r|R]ound by (\\d+).*?more.*?,"),"[Condition:ROUND_STATE, Value:Loss>=$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing the [r|R]ound by (\\d+).*?less.*?,"),"[Condition:ROUND_STATE, Value:Loss<=$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are losing the [r|R]ound,"),"[Condition:ROUND_STATE, Value:Loss] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning the [r|R]ound by (\\d+).*?more.*?,"),"[Condition:ROUND_STATE, Value:Win>=$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning the [r|R]ound by (\\d+).*?less.*?,"),"[Condition:ROUND_STATE, Value:Win<=$1] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("[i|I]f you are winning the [r|R]ound,"),"[Condition:ROUND_STATE, Value:Win] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if you (\\w*) this turn"),"[Condition:TURN_STATE, Value:($1)] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if your deck contains (\\d*) or more (.*?) and (\\d*) or more (.*?),"),"[Condition:DECK_CONTAINS, Value:>=$1, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)]] [Condition:DECK_CONTAINS, Value:>=$3, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($4)]] ", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("if your deck contains (\\d*) or more (.*?) ([ card,| cards,]*?),"),"[Condition:DECK_CONTAINS, Value:>=$1, CountEach:[Who:SELF, Where:CARDS_IN_DECK, CompareTo:($2)]] ", false);


        // Duration
        preFormatted = replaceAll(preFormatted, Pattern.compile("for the rest of the game."),"[Duration:PERMANENT] ", false);

        // For COPY & PASTE
//        preFormatted = replaceAll(preFormatted, Pattern.compile(""),"");

        // Post-processing
        // TODO: Everything in brackets (ex. targets) need to be defined better

        // Clean up
        preFormatted = replaceAll(preFormatted, Pattern.compile("--+"),"-", false);
        preFormatted = replaceAll(preFormatted, Pattern.compile("  +")," ", false);

        // Count percentage of replaced
        int charsAfter = countNotReplacedChars(preFormatted);
        float percToReplace = (float) charsAfter * 100 / charsBefore;
        log.debug(String.format("Percentage left to replace: %.2f%%", percToReplace));
        log.debug("");

        // FIXME: REMOVAL OF EVERYTHING IN BRACKETS ONLY FOR DEBUGGING
        preFormatted = replaceAll(preFormatted, Pattern.compile("\\[.*?\\]"),"-", false);

        // Output anomalies
        outputAnomalies(true);

        // Write regex replaced file
        writeFullFile(regexFile, preFormatted);
    }

    private static String replaceAll (String allRows, Pattern pattern, String replacement) {
        return replaceAll(allRows, pattern, replacement, true);
    }

    private static String replaceAll (String allRows, Pattern pattern, String replacement, boolean doLog) {
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

    private static String readFullFile (String fileName) {
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

    private static void writeFullFile (String fileName, String content) {
        // Write formatted file to be processed using regex
        try (FileWriter fr = new FileWriter(fileName)) {
            fr.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fixFormatting (String allRows) {
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
        allRows = allRows.replaceAll("burns* *\\(-*(.*?)\\)","BURN ($1)");
        allRows = allRows.replaceAll("BURN \\(-(.*?)\\)"," BURN ($1)");
        allRows = allRows.replaceAll(" burn for (\\d)","BURN ($1)");

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

    private static String optionalFormatting (String allRows) {
        allRows = allRows.replaceAll("Cmmn","Common");
        allRows = allRows.replaceAll("Lgnd","Legendary");
        allRows = allRows.replaceAll("Ult-Fusn","Ultra-Fusion");
        allRows = allRows.replaceAll("Fusn","Fusion");

        return allRows;
    }

    private static int countNotReplacedChars(String allRows) {
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
