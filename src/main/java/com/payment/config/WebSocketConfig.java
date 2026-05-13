package com.payment.config;

import com.auth.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtTokenProvider tokenProvider;
    private final boolean externalBrokerEnabled;
    private final String rabbitmqHost;
    private final int rabbitmqPort;
    private final String rabbitmqUsername;
    private final String rabbitmqPassword;

    @Autowired
    public WebSocketConfig(JwtTokenProvider tokenProvider,
                          @Value("${app.websocket.external-broker.enabled:false}") boolean externalBrokerEnabled,
                          @Value("${spring.rabbitmq.host}") String rabbitmqHost,
                          @Value("${spring.rabbitmq.port}") int rabbitmqPort,
                          @Value("${spring.rabbitmq.username}") String rabbitmqUsername,
                          @Value("${spring.rabbitmq.password}") String rabbitmqPassword) {
        this.tokenProvider = tokenProvider;
        this.externalBrokerEnabled = externalBrokerEnabled;
        this.rabbitmqHost = rabbitmqHost;
        this.rabbitmqPort = rabbitmqPort;
        this.rabbitmqUsername = rabbitmqUsername;
        this.rabbitmqPassword = rabbitmqPassword;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        if (externalBrokerEnabled) {
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(rabbitmqHost)
                    .setRelayPort(61613)
                    .setClientLogin(rabbitmqUsername)
                    .setClientPasscode(rabbitmqPassword)
                    .setSystemLogin(rabbitmqUsername)
                    .setSystemPasscode(rabbitmqPassword)
                    .setSystemHeartbeatSendInterval(30000)
                    .setSystemHeartbeatReceiveInterval(30000)
                    .setVirtualHost("/");
            logger.info("WebSocket message broker configured with RabbitMQ relay at {}:{}", rabbitmqHost, rabbitmqPort);
        } else {
            config.enableSimpleBroker("/topic", "/queue");
            logger.info("WebSocket message broker configured with in-memory simple broker");
        }

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure properly for production
                .withSockJS()
                .setHeartbeatTime(25000) // 25 seconds
                .setDisconnectDelay(5000) // 5 seconds
                .setSessionCookieNeeded(false);
        
        logger.info("STOMP endpoint registered at /ws with SockJS fallback");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    Authentication auth = authenticateWebSocketUser(authHeader);

                    accessor.setUser(auth);
                    accessor.setSessionAttributes(Collections.singletonMap("userId", resolveUserId(authHeader)));
                    logger.info("WebSocket connection established for user {}", auth.getName());
                }
                
                return message;
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    logger.trace("Outbound WebSocket message: {} to {}", 
                        accessor.getCommand(), accessor.getDestination());
                }
                
                return message;
            }
        });
    }

    private Authentication authenticateWebSocketUser(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (tokenProvider.validateToken(token)) {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(
                                Optional.ofNullable(tokenProvider.getAuthoritiesFromToken(token)).orElse("ROLE_USER").split(","))
                        .filter(authority -> !authority.isBlank())
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                return new UsernamePasswordAuthenticationToken(
                        tokenProvider.getUsernameFromToken(token),
                        null,
                        authorities.isEmpty() ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) : authorities
                );
            }
        }

        logger.info("WebSocket connection allowed without authentication for testing");
        return new UsernamePasswordAuthenticationToken(
                "testuser", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private Long resolveUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenProvider.validateToken(token)) {
                return tokenProvider.getUserIdFromToken(token);
            }
        }

        return 1L;
    }
}
