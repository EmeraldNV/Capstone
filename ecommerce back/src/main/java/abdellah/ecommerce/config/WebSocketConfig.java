package abdellah.ecommerce.config;

import abdellah.ecommerce.service.PaymentWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PaymentWebSocketHandler paymentWebSocketHandler;
    private final FrontendProperties frontendProperties;

    public WebSocketConfig(PaymentWebSocketHandler paymentWebSocketHandler, FrontendProperties frontendProperties) {
        this.paymentWebSocketHandler = paymentWebSocketHandler;
        this.frontendProperties = frontendProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(paymentWebSocketHandler, "/ws/payments")
                .setAllowedOrigins(frontendProperties.getBaseUrl());
    }
}
