package org.orgname.solace.subscriber;

import com.solace.messaging.MessagingService;
import com.solace.messaging.receiver.DirectMessageReceiver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void shutdownIsIdempotentAcrossRepeatedCalls() {
        DirectReceiver directReceiver = new DirectReceiver(
                new AccessProperties(buildEnvironmentProvider(Map.of()))
        );
        AtomicInteger receiverTerminateCalls = new AtomicInteger();
        AtomicInteger messagingDisconnectCalls = new AtomicInteger();
        DirectMessageReceiver receiver = proxy(
                DirectMessageReceiver.class,
                (proxy, method, args) -> {
                    if ("terminate".equals(method.getName())) {
                        receiverTerminateCalls.incrementAndGet();
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        MessagingService messagingService = proxy(
                MessagingService.class,
                (proxy, method, args) -> {
                    if ("disconnect".equals(method.getName())) {
                        messagingDisconnectCalls.incrementAndGet();
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        directReceiver.shutdown(receiver, messagingService);
        directReceiver.shutdown(receiver, messagingService);

        assertTrue(directReceiver.isShutdown());
        assertEquals(1, receiverTerminateCalls.get());
        assertEquals(1, messagingDisconnectCalls.get());
    }

    private static java.util.function.Function<String, String> buildEnvironmentProvider(Map<String, String> environment) {
        return environment::get;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler invocationHandler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, invocationHandler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
