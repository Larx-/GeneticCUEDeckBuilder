public class Game {

    Rules rules;
    PlayerState resident;
    PlayerState opponent;

    public Game(Rules rules, AgentInterface resident, AgentInterface opponent) {
        this.rules = rules;

        this.resident = new PlayerState(resident, this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);
        this.opponent = new PlayerState(opponent, this.rules.getEnergyStarting(), this.rules.getEnergyPerTurn(), 0);


        this.resident.getDeck().printDeck();
        this.opponent.getDeck().printDeck();
        System.out.println();

        this.resident.getDeck().shuffleDeck();
        this.opponent.getDeck().shuffleDeck();

        this.resident.getDeck().printDeck();
        this.opponent.getDeck().printDeck();
        System.out.println();
    }

    public void playGame(){
        int rWin = 0;
        int oWin = 0;

        while(rWin < 3 && oWin <3){
            boolean resWon = playRound();
            if (resWon) {
                rWin++;
                System.out.println("\t\tResident won round "+(rWin+oWin));
            } else {
                oWin++;
                System.out.println("\t\tOpponent won round "+(rWin+oWin));
            }
            System.out.println("\t----------------------------\t\n\n");
        }

        if (rWin > oWin) {
            System.out.println("Resident won game");
        } else {
            System.out.println("Opponent won game");
        }
    }

    private boolean playRound(){
        int powerBalance = 0;

        for (int i = 0; i < 3; i++){
            powerBalance += playTurn(powerBalance);
        }

        while (powerBalance == 0) {
            powerBalance += playTurn(powerBalance);
        }

        return powerBalance > 0;
    }

    private int playTurn(int oldPowerBalance){
        this.resident.getDeck().drawCards();
        this.opponent.getDeck().drawCards();

        this.printHands();

        this.resident.setEnergyAvailable(this.resident.getAgent().getNextTurn(this.resident.getEnergyAvailable()));
        this.opponent.setEnergyAvailable(this.opponent.getAgent().getNextTurn(this.opponent.getEnergyAvailable()));

        int rPow = this.resident.getDeck().executePlay();
        int oPow = this.opponent.getDeck().executePlay();
        int diff = rPow - oPow;

        this.printPlay(diff + oldPowerBalance);

        this.resident.getDeck().returnPlayedCards();
        this.opponent.getDeck().returnPlayedCards();

        System.out.println("\t----------------------------\t");

        this.resident.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());
        this.opponent.updateEnergyAvailable(this.rules.getEnergyMin(), this.rules.getEnergyMax());

        return diff;
    }

    private void printHands(){
        printCards(this.resident.getDeck().getCardsInHand(),"(Energy: "+this.resident.getEnergyAvailable()+") Resident's Hand: ", true);
        printCards(this.opponent.getDeck().getCardsInHand(),"(Energy: "+this.opponent.getEnergyAvailable()+") Opponent's Hand: ", true);
    }

    private void printPlay(int powerBalance){
        System.out.println();
        printCards(this.resident.getDeck().getCardsInHand(),"(Energy: "+this.resident.getEnergyAvailable()+") Resident's Hand: ", true);
        printCards(this.resident.getDeck().getCardsPlayed(),"     Resident's Play: ", false);
        System.out.println(powerBalance);
        printCards(this.opponent.getDeck().getCardsPlayed(),"     Opponent's Play: ", false);
        printCards(this.opponent.getDeck().getCardsInHand(),"(Energy: "+this.opponent.getEnergyAvailable()+") Opponent's Hand: ", true);
        System.out.println();
    }

    private void printCards(Card[] toPrint, String desc, boolean printIndex){
        System.out.print(desc);
        for (int i = 0; i < toPrint.length; i++) {
            if (printIndex) {
                System.out.print("(" + i + ")");
            }
            if (toPrint[i] == null) {
                System.out.print(" -- \t");
            } else {
                System.out.print(" " + toPrint[i].getName() + "\t");
            }
        }
        System.out.println();
    }

}
