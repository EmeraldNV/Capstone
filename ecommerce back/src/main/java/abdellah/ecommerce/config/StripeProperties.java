package abdellah.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {

    private String secretKey = "";
    private String publishableKey = "";
    private String webhookSecret = "";
    private String successPath = "/checkout/stripe/success";
    private String cancelPath = "/checkout/stripe/cancel";

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getSuccessPath() {
        return successPath;
    }

    public void setSuccessPath(String successPath) {
        this.successPath = successPath;
    }

    public String getCancelPath() {
        return cancelPath;
    }

    public void setCancelPath(String cancelPath) {
        this.cancelPath = cancelPath;
    }
}
