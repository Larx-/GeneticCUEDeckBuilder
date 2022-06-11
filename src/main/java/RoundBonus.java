import lombok.Getter;

public class RoundBonus {
    @Getter private final String album;
    @Getter private final String collection;
    @Getter private final int bonus;

    public RoundBonus(String album, String collection, int bonus) {
        this.album = album;
        this.collection = collection;
        this.bonus = bonus;
    }
}
