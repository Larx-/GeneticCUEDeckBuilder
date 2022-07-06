package GameElements;

import Enums.Album;
import Enums.Collection;
import lombok.Getter;

public class RoundBonus {
    @Getter private final Album album;
    @Getter private final Collection collection;
    @Getter private final int albumBonus;
    @Getter private final int collectionBonus;
    private final int defaultAlbumBonus = 10;

    public RoundBonus(Collection collection, int collectionBonus) {
        this.album = collection.getAffiliatedAlbum();
        this.albumBonus = this.defaultAlbumBonus;

        this.collection = collection;
        this.collectionBonus = collectionBonus;
    }

    public RoundBonus(Album album, int albumBonus) {
        this.album = album;
        this.albumBonus = albumBonus;

        this.collection = Collection.NAN;
        this.collectionBonus = 0;
    }

    public RoundBonus(Album album, Collection collection, int albumBonus, int collectionBonus) {
        this.album = album;
        this.albumBonus = albumBonus;

        this.collection = collection;
        this.collectionBonus = collectionBonus;
    }
}
