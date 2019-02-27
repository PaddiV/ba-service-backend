package uhh_lt.webserver;

/**
 * Used for rating features of analyzed text.
 *
 */
public enum Rating {
    GOOD ("good"),
    BAD("bad"),
    OK("ok"),
    SHORT("short"),
    LONG("long");

    private final String value;

    private Rating(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}