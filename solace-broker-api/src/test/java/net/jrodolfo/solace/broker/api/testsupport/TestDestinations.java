package net.jrodolfo.solace.broker.api.testsupport;

/**
 * Canonical sample destinations used by broker API tests.
 *
 * <p>These values mirror {@code docs/reference/sample-destinations.md} and are
 * intentionally not production broker configuration.
 */
public final class TestDestinations {

    public static final String SYSTEM_01 = "solace/java/direct/system-01";
    public static final String SYSTEM_02 = "solace/java/direct/system-02";
    public static final String SYSTEM_03 = "solace/java/direct/system-03";
    public static final String SYSTEM_04 = "solace/java/direct/system-04";

    private TestDestinations() {
    }
}
