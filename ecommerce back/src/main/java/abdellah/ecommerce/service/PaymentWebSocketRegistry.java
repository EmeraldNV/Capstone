package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.payment.PaymentStatusUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentWebSocketRegistry {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebSocketRegistry.class);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByCheckoutSession = new ConcurrentHashMap<>();

    public PaymentWebSocketRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String checkoutSessionId, WebSocketSession webSocketSession) {
        sessionsByCheckoutSession.computeIfAbsent(checkoutSessionId, key -> ConcurrentHashMap.newKeySet())
                .add(webSocketSession);
    }

    public void unregister(String checkoutSessionId, WebSocketSession webSocketSession) {
        Set<WebSocketSession> sessions = sessionsByCheckoutSession.get(checkoutSessionId);
        if (sessions == null) {
            return;
        }
        sessions.remove(webSocketSession);
        if (sessions.isEmpty()) {
            sessionsByCheckoutSession.remove(checkoutSessionId);
        }
    }

    public void broadcast(PaymentStatusUpdate update) {
        Set<WebSocketSession> sessions = sessionsByCheckoutSession.get(update.sessionId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(update);
        } catch (Exception ex) {
            log.warn("Unable to serialize payment websocket update for session {}", update.sessionId(), ex);
            return;
        }

        TextMessage message = new TextMessage(payload);
        sessions.removeIf(session -> !sendSafely(session, message));
    }

    private boolean sendSafely(WebSocketSession session, TextMessage message) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
            return true;
        } catch (IOException ex) {
            log.debug("Payment websocket send failed for session {}: {}", session.getId(), ex.getMessage());
            return false;
        }
    }
}
