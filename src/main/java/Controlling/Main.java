package Controlling;

import Agents.*;
import GameElements.*;
import Setup.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.log4j.Log4j2;

import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Log4j2
public class Main {

    public static SecureRandom random;
    public static final String cardsFile = "src/main/resources/Cards/cards.csv";
    public static final String rulesFile = "src/main/resources/Rules/rules_1.json";
    public static final String residentsFile = "src/main/resources/Cards/residents.csv";
    public static final String candidatesFile = null;

    public static void main(String[] args) {
        Main.random = new SecureRandom();
        GenAlg genAlg = new GenAlg(cardsFile, rulesFile, residentsFile, candidatesFile);
        genAlg.runGeneticAlgorithm();
    }
}