package GameElements;

import Enums.Album;
import Enums.Collection;
import lombok.Getter;

public class RoundBonus {
    @Getter private final Album album;
    @Getter private final Collection collection;
    @Getter private final int albumBonus;
    @Getter private final int collectionBonus;

    public RoundBonus(Album album, Collection collection, int albumBonus, int collectionBonus) {
        this.album = album;
        this.collection = collection;
        this.albumBonus = albumBonus;
        this.collectionBonus = collectionBonus;
    }
}
