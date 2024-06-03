package IOT_Platform.Lantern_Of_Dusk_BE.websoket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ConcurrentHashMap<String, WebSocketSession> CLIENTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WebSocketSession> DEVICES = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> queryMap = parseQuery(session.getUri().getQuery());
        String sessionType = queryMap.get("type");

        if ("device".equals(sessionType)) {
            DEVICES.put(session.getId(), session);
            System.out.println("Device connected: " + session.getId());
        } else {
            CLIENTS.put(session.getId(), session);
            System.out.println("Client connected: " + session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (CLIENTS.containsKey(session.getId())) {
            CLIENTS.remove(session.getId());
            System.out.println("Client disconnected: " + session.getId());
        } else if (DEVICES.containsKey(session.getId())) {
            DEVICES.remove(session.getId());
            System.out.println("Device disconnected: " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 메시지 처리 로직 (예제로 기존 로직을 유지)
        CLIENTS.entrySet().forEach(arg -> {
            if (!arg.getKey().equals(session.getId())) {
                try {
                    arg.getValue().sendMessage(message);
                    System.out.println("Send to Client: " + message.getPayload());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> queryPairs = new ConcurrentHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }
}
