package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.jpa.PublishStatus;

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
    public static final Duration STALE_PENDING_THRESHOLD = Duration.ofMinutes(5);

    private MessageLifecycleSupport() {
    }

    /**
     * Determines if a message is stuck in {@code PENDING} state longer than
     * the allowed threshold.
     *
     * @param publishStatus the current {@link PublishStatus} of the message
     * @param createdAt     the timestamp when the message was created
     * @return {@code true} if the message status is {@code PENDING} and it was created
     * before the {@link #STALE_PENDING_THRESHOLD}; {@code false} otherwise
     */
    public static boolean isStalePending(PublishStatus publishStatus, LocalDateTime createdAt) {
        if (publishStatus != PublishStatus.PENDING || createdAt == null) {
            return false;
        }

        return createdAt.isBefore(LocalDateTime.now().minus(STALE_PENDING_THRESHOLD));
    }
}
