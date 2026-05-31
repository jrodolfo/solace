package net.jrodolfo.solace.broker.api.jpa;

/**
 * Represents the lifecycle status of a message publication attempt.
 */
public enum PublishStatus {
    /**
     * The message has been received by the system but has not yet been sent to the Solace broker.
     */
    PENDING,

    /**
     * The message has been successfully published to and acknowledged by the Solace broker.
     */
    PUBLISHED,

    /**
     * The attempt to publish the message to the Solace broker failed.
     */
    FAILED
}
