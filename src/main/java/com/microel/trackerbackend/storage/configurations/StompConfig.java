package com.microel.trackerbackend.storage.configurations;

import com.microel.trackerbackend.controllers.EmployeeSessionsController;
import com.microel.trackerbackend.services.MonitoringService;
import com.microel.trackerbackend.services.external.acp.AcpClient;
import com.microel.trackerbackend.storage.exceptions.IllegalFields;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Arrays;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    public static final String SIMPLE_BROKER_PREFIX = "/api";
    private final EmployeeSessionsController employeeSessionController;
    private final MonitoringService monitoringService;
    private final AcpClient acpClient;

    public StompConfig(EmployeeSessionsController employeeSessionController, MonitoringService monitoringService, @Lazy AcpClient acpClient) {
        this.employeeSessionController = employeeSessionController;
        this.monitoringService = monitoringService;
        this.acpClient = acpClient;
    }


    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        String tokenValue = accessor.getLogin();

        employeeSessionController.addSession(tokenValue, sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        employeeSessionController.removeSession(event.getSessionId());
        monitoringService.releasePingMonitoring(UUID.fromString(event.getSessionId()));
    }

    @EventListener
    public void onSubscribeEvent(SessionSubscribeEvent event) {
        Message<byte[]> message = event.getMessage();
        UUID sessionId = getSessionId(message);
        String[] path = getDestination(message);
        if ("monitoring".equals(path[0])) {
            if ("ping".equals(path[1])) {
                try{
                    monitoringService.appendPingMonitoring(path[2], sessionId);
                }catch (ArrayIndexOutOfBoundsException e){
                    throw new IllegalFields("Не указан ip адрес цели мониторинга");
                }
            }
        }
        if ("acp".equals(path[0])){
            if("commutator".equals(path[1])){
                if("remote-update-pool".equals(path[2])){
                    acpClient.multicastUpdateCommutatorRemoteUpdatePool();
                }
            }
        }
    }

    @EventListener
    public void onUnsubscribeEvent(SessionUnsubscribeEvent event) {
        UUID sessionId = getSessionId(event.getMessage());
        monitoringService.releasePingMonitoring(sessionId);
    }

    private String[] getDestination(Message<byte[]> message) {
        String destination = message.getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER).toString();
        return Arrays.stream(destination.split("/")).skip(2).toArray(String[]::new);
    }

    private UUID getSessionId(Message<byte[]> message) {
        return UUID.fromString(message.getHeaders().get(SimpMessageHeaderAccessor.SESSION_ID_HEADER).toString());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(SIMPLE_BROKER_PREFIX, "/user");
        config.setUserDestinationPrefix("/user");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/socket").setAllowedOrigins("*").withSockJS();
        registry.addEndpoint("/socket").setAllowedOrigins("*");
    }
}
