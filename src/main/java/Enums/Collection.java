package Enums;

public enum Collection {
    AMAS ("Amazing Astronauts", Album.SPACE),
    AMFO ("American Folklore", Album.HISTORY),
    TARO ("Tarot", Album.ARTS_AND_CULTURE),
    NOMY ("Norse Mythology", Album.ARTS_AND_CULTURE),
    EXST ("Exploring the Stars", Album.SPACE);

    private final String name;
    private final Album album;

    Collection (String s, Album album) {
        this.name = s;
        this.album = album;
    }

    public static Collection fromString(String search) {
        for (Collection nextElem : Collection.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }

    public Album getAffiliatedAlbum() {
        return this.album;
    }

    public boolean equalsName(String otherName) {
        return this.name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
