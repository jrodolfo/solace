package org.orgname.solace.subscriber;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectReceiverTest {

    @Test
    void handleInboundMessageStateTracksReceivedMessagesWithoutDiscard() {
        DirectReceiver directReceiver = new DirectReceiver(
                new AccessProperties(buildEnvironmentProvider(Map.of()))
        );

        directReceiver.handleInboundMessageState(false, false);

        assertEquals(1, directReceiver.getMsgRecvCounter());
        assertFalse(directReceiver.hasDetectedDiscard());
    }

    @Test
    void handleInboundMessageStateTracksDiscardSignals() {
        DirectReceiver directReceiver = new DirectReceiver(
                new AccessProperties(buildEnvironmentProvider(Map.of()))
        );

        directReceiver.handleInboundMessageState(true, false);

        assertEquals(1, directReceiver.getMsgRecvCounter());
        assertTrue(directReceiver.hasDetectedDiscard());
    }

    private static java.util.function.Function<String, String> buildEnvironmentProvider(Map<String, String> environment) {
        return environment::get;
    }
}
