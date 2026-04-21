package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.jpa.PublishStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public final class MessageLifecycleSupport {

    public static final Duration STALE_PENDING_THRESHOLD = Duration.ofMinutes(5);

    private MessageLifecycleSupport() {
    }

    public static boolean isStalePending(PublishStatus publishStatus, LocalDateTime createdAt) {
        if (publishStatus != PublishStatus.PENDING || createdAt == null) {
            return false;
        }

        return createdAt.isBefore(LocalDateTime.now().minus(STALE_PENDING_THRESHOLD));
    }
}
