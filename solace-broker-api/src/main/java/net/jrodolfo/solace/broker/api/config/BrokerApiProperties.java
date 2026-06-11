package net.jrodolfo.solace.broker.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import net.jrodolfo.solace.broker.api.service.MessageLifecycleSupport;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Runtime tuning settings for broker API operational behavior.
 *
 * <p>Values are bound from {@code app.*} properties and validated during
 * application startup so invalid operational settings fail fast.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class BrokerApiProperties {

    @Valid
    private final Lifecycle lifecycle = new Lifecycle();

    @Valid
    private final Retry retry = new Retry();

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public Retry getRetry() {
        return retry;
    }

    /**
     * Settings related to stored-message lifecycle interpretation.
     */
    public static class Lifecycle {

        /**
         * Duration after which a {@code PENDING} message is reported as stale.
         */
        @NotNull
        @DurationMin(nanos = 1)
        private Duration stalePendingThreshold = MessageLifecycleSupport.DEFAULT_STALE_PENDING_THRESHOLD;

        public Duration getStalePendingThreshold() {
            return stalePendingThreshold;
        }

        public void setStalePendingThreshold(Duration stalePendingThreshold) {
            this.stalePendingThreshold = stalePendingThreshold;
        }
    }

    /**
     * Settings related to retry endpoints.
     */
    public static class Retry {

        /**
         * Maximum number of message ids accepted by the bulk retry endpoint.
         */
        @Min(1)
        private int maxBatchSize = 100;

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }
    }
}
