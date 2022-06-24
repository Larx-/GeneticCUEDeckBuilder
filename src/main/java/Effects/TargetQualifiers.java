package Effects;

public enum TargetQualifiers {
    BASE_ENERGY_UNDER,
    BASE_ENERGY_ABOVE,
    FROM_ALBUM,
    FROM_COLLECTION,
    FROM_NAME,
    FROM_NAME_CONTAINS,

    INIT_FINISHED,
    INVALID_STATE;

    public static TargetQualifiers fromString(String search) {
        for (TargetQualifiers nextElem : TargetQualifiers.values()) {
            if (nextElem.toString().equalsIgnoreCase(search)) {
                return nextElem;
            }
        }
        return null;
    }
}
