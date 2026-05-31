package net.jrodolfo.solace.broker.api.service;

import net.jrodolfo.solace.broker.api.jpa.PublishStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Utility class for message lifecycle logic and state transitions.
 */
public final class MessageLifecycleSupport {

    /**
     * The threshold after which a message in {@code PENDING} state is considered stale.
     * Currently set to 5 minutes.
     */
    public static final Duration DEFAULT_STALE_PENDING_THRESHOLD = Duration.ofMinutes(5);

    private MessageLifecycleSupport() {
    }

    /**
     * Determines if a message is stuck in {@code PENDING} state longer than
     * the allowed threshold.
     *
     * @param publishStatus the current {@link PublishStatus} of the message
     * @param createdAt     the timestamp when the message was created
     * @return {@code true} if the message status is {@code PENDING} and it was created
     * before the {@link #DEFAULT_STALE_PENDING_THRESHOLD}; {@code false} otherwise
     */
    public static boolean isStalePending(PublishStatus publishStatus, LocalDateTime createdAt) {
        return isStalePending(publishStatus, createdAt, DEFAULT_STALE_PENDING_THRESHOLD);
    }

    /**
     * Determines if a message is stuck in {@code PENDING} state longer than
     * the provided threshold.
     *
     * @param publishStatus the current {@link PublishStatus} of the message
     * @param createdAt     the timestamp when the message was created
     * @param threshold     the duration after which a pending message is stale
     * @return {@code true} if the message status is {@code PENDING} and it was created
     * before the threshold; {@code false} otherwise
     */
    public static boolean isStalePending(PublishStatus publishStatus, LocalDateTime createdAt, Duration threshold) {
        if (publishStatus != PublishStatus.PENDING || createdAt == null) {
            return false;
        }

        Duration effectiveThreshold = threshold == null ? DEFAULT_STALE_PENDING_THRESHOLD : threshold;
        return createdAt.isBefore(LocalDateTime.now().minus(effectiveThreshold));
    }
}
