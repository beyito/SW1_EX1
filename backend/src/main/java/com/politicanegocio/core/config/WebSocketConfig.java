package com.politicanegocio.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${spring.rabbitmq.host}")
    private String relayHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int relayPort;

    @Value("${spring.rabbitmq.username}")
    private String relayUsername;

    @Value("${spring.rabbitmq.password}")
    private String relayPassword;

    @Value("${app.websocket.use-relay:false}")
    private boolean useRelay;

    @Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app"); // Lo que va al @Controller
    registry.setUserDestinationPrefix("/user");

    if (useRelay) {
        log.info("WebSocket broker mode=RELAY host={} port={}", relayHost, relayPort);
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setSystemLogin(relayUsername)
                .setSystemPasscode(relayPassword)
                .setClientLogin(relayUsername)
                .setClientPasscode(relayPassword)
                .setVirtualHost("/");
    } else {
        log.warn("WebSocket broker mode=SIMPLE (relay disabled). This is recommended for local development.");
        registry.enableSimpleBroker("/topic", "/queue");
    }
}

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-designer")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-bpmn")
                .setAllowedOriginPatterns("*");
    }
}
