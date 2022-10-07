package Controlling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/*
    Comparing the 1_StandardRules, these occurred suspiciously often (1500 is max possible):

        ACFU001		270				243		273				 -> 786
        EES008		267		85		253		19		127		 -> 751
        EMM034		33		276		54				275		 -> 638
        EWW028		25		262		209		282		274		 -> 1052
        LPR012		267		258		249		265		277		 -> 1316
        LVE032		274						256		237		 -> 767
        OCE009		214		166		199		253		265		 -> 1097
        OCP002		274		96		256		252		1		 -> 879
        SBB012		277		283		285		281		289		 -> 1415
        SMA025		278		285		282		284		283		 -> 1412
        SOD016		281		271		266		275		267		 -> 1360
        STE019		280		275		271		266		275		 -> 1367

    12 cards occurred in at least 3 files and at least 500 times in total! (Out of 1889 unique cards)
*/

/*
    Comparing the 4_NoEffect, these occurred suspiciously often (200 is max possible):

        ACFU001		93		93		 -> 186
        ACRR004		92		27		 -> 119
        EEE009		66		83		 -> 149
        EWW032		22		78		 -> 100
        ORE004		85		57		 -> 142
        PCA022		32		85		 -> 117
        SDD028		68		87		 -> 155
        SFN025		85		46		 -> 131

    8 cards occurred in at least 2 files and at least 100 times in total! (Out of 419 unique cards)
*/

/*
    Comparing the 7_StandardComboZero, these occurred suspiciously often (300 is max possible):

        ACFU001		260		 -> 260
        ELT008		220		 -> 220
        EWW028		283		 -> 283
        LPL018		276		 -> 276
        LPR012		200		 -> 200
        SBB012		284		 -> 284
        SDD026		217		 -> 217
        SMA025		274		 -> 274
        SOD016		281		 -> 281

    9 cards occurred in at least 1 files and at least 200 times in total! (Out of 540 unique cards)
*/

public class CountingCommonlyOccurringCards {

    public static String[] fileNames_Standard = {
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_Rules1/currentCandidates - Kopie.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_Rules2/currentCandidates - Kopie.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_Rules3/currentCandidates - Kopie.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_ContinuedCanFrom1_Rules3/currentCandidates.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_ContinuedCanFrom1_3_Rules2/currentCandidates.csv"
    };

    public static String[] fileNames_NoEffects = {
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/4_NoEffects/From_Null_Rules_1/currentCandidates.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/4_NoEffects/From_1_Rules_3/currentCandidates.csv"
    };    
    
    public static String[] fileNames = {
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_Rules1/currentCandidates - Kopie.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_Rules2/currentCandidates - Kopie.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/1_StandardRules/RepeatResidents_Rules3/currentCandidates - Kopie.csv",
            "C:/Users/kabra/Desktop/Uni/Bachelor 2/GeneticCUEDeckBuilder/src/main/resources/Results/7_StandardComboZero/From_Null_Rules_1/currentCandidates.csv"
    };

    public static int minTotalOccurrences = 200;
    public static int minOccurredInFiles  = 3;
    
    public static void main(String[] args) {
        List<Map<String, Integer>> listOfCounts = new ArrayList<>();
        List<String> allCards = new ArrayList<>();

        for (String file : fileNames) {
            Map<String, Integer> mapOfUsedCards = new HashMap<>();
            String[] arrayOfCards = readFullFile(file);

            for (String card : arrayOfCards) {
                if (mapOfUsedCards.containsKey(card)) {
                    mapOfUsedCards.put(card, mapOfUsedCards.get(card)+1);
                } else {
                    mapOfUsedCards.put(card, 1);

                    if (!allCards.contains(card)) {
                        allCards.add(card);
                    }
                }
            }

            listOfCounts.add(mapOfUsedCards);
        }

        Collections.sort(allCards);
        
        int countCommonCards    = 0;
        for (String card : allCards) {
            int totalOccurrences = 0;
            int occurredInFiles  = 0;

            StringBuilder line = new StringBuilder(card + "\t\t");

            for (Map<String, Integer> mapOfUsedCards : listOfCounts) {
                if (mapOfUsedCards.containsKey(card)) {
                    Integer occurrences = mapOfUsedCards.get(card);
                    totalOccurrences += occurrences;
                    occurredInFiles++;

                    line.append(occurrences);
                }
                line.append("\t\t");
            }
            line.append(" -> ").append(totalOccurrences);

            if (totalOccurrences >= minTotalOccurrences && occurredInFiles >= minOccurredInFiles) {
                System.out.println(line);
                countCommonCards++;
            }
        }
        System.out.println("\n" + countCommonCards + " cards occurred in at least "
                + minOccurredInFiles + " files and at least "
                + minTotalOccurrences + " times in total! (Out of " + allCards.size() + " unique cards)");
    }

    public static String[] readFullFile (String fileName) {
        StringBuilder fullFile = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                fullFile.append(line).append(",");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullFile.toString().trim().split(",");
    }
}
