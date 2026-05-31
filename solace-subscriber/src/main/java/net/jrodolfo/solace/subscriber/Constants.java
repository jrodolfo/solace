package net.jrodolfo.solace.subscriber;

/**
 * Shared constants for the Solace subscriber module.
 */
public class Constants {

    /**
     * The Solace topic name that this subscriber listens to.
     * <p>Uses a wildcard {@code *} to match sub-topics under {@code solace/java/direct/system-0}.
     */
    static final String TOPIC_NAME = "solace/java/direct/system-0*";
}
