package io.github.flexitech_realtime_provider.realtime_service.handlers.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
public class RealtimeWebsocketHandler implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.send(
                session.receive()
                        .map(webSocketMessage -> {
                            String payload = webSocketMessage.getPayloadAsText();
                            // Process incoming messages
                            return "Echo: " + payload;
                        })
                        .map(session::textMessage)
        );
    }
}
