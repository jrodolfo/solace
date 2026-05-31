package net.jrodolfo.solace.broker.api.config;

import net.jrodolfo.solace.broker.api.service.MessageLifecycleSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class BrokerApiPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindDefaultTuningValues() {
        contextRunner.run(context -> {
            BrokerApiProperties properties = context.getBean(BrokerApiProperties.class);

            assertThat(properties.getLifecycle().getStalePendingThreshold())
                    .isEqualTo(MessageLifecycleSupport.DEFAULT_STALE_PENDING_THRESHOLD);
            assertThat(properties.getRetry().getMaxBatchSize()).isEqualTo(100);
        });
    }

    @Test
    void shouldRejectZeroBulkRetryBatchSize() {
        contextRunner
                .withPropertyValues("app.retry.max-batch-size=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectZeroStalePendingThreshold() {
        contextRunner
                .withPropertyValues("app.lifecycle.stale-pending-threshold=PT0S")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(BrokerApiProperties.class)
    static class TestConfiguration {
    }
}
