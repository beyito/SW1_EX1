package com.politicanegocio.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host}")
    private String relayHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int relayPort;

    @Value("${spring.rabbitmq.username}")
    private String relayUsername;

    @Value("${spring.rabbitmq.password}")
    private String relayPassword;

    @Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app"); // Lo que va al @Controller
    registry.setUserDestinationPrefix("/user");

    // Habitualmente usamos /topic y /queue para RabbitMQ
    registry.enableStompBrokerRelay("/topic", "/queue") 
            .setRelayHost(relayHost)
            .setRelayPort(relayPort)
            .setSystemLogin(relayUsername)
            .setSystemPasscode(relayPassword)
            .setClientLogin(relayUsername)
            .setClientPasscode(relayPassword)
            .setVirtualHost("/");
}

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-designer")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-bpmn")
                .setAllowedOriginPatterns("*");
    }
}
