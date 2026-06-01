package abdellah.ecommerce.service;

import abdellah.ecommerce.config.ResendProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ResendEmailService {

    private final ResendProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ResendEmailService(ResendProperties properties) {
        this.properties = properties;
    }

    public void sendVerificationEmail(String recipientEmail, String verificationUrl) {
        ensureConfigured();

        String subject = "Verifica il tuo account";
        String text = "Clicca il seguente link per verificare il tuo account: " + verificationUrl;
        String html = """
                <html>
                  <body>
                    <p>Grazie per la registrazione.</p>
                    <p><a href="%s">Verifica il tuo account</a></p>
                    <p>Se il link non funziona, copia e incolla questo URL nel browser:</p>
                    <p>%s</p>
                  </body>
                </html>
                """.formatted(verificationUrl, verificationUrl);

        String body = """
                {
                  "from": "%s",
                  "to": "%s",
                  "subject": "%s",
                  "text": "%s",
                  "html": "%s"
                }
                """.formatted(
                escapeJson(properties.getFromEmail()),
                escapeJson(recipientEmail),
                escapeJson(subject),
                escapeJson(text),
                escapeJson(html)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/emails"))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Resend returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Resend request was interrupted.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to send verification email through Resend.", ex);
        }
    }

    public void sendPaymentReceiptEmail(String recipientEmail,
                                        String orderNumber,
                                        String sessionId,
                                        String paymentIntentId,
                                        BigDecimal amountTotal,
                                        String currencyCode,
                                        List<String> items,
                                        Instant completedAt) {
        ensureConfigured();

        String formattedDate = completedAt == null
                ? "-"
                : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Rome"))
                .format(completedAt);
        String itemSummary = items == null || items.isEmpty()
                ? "Nessun dettaglio articolo disponibile."
                : String.join("\n", items);

        String subject = "Conferma pagamento " + (orderNumber == null ? "" : orderNumber);
        String text = """
                Pagamento completato con successo.

                Ordine: %s
                Sessione Stripe: %s
                Payment Intent: %s
                Importo: %s %s
                Data: %s

                Articoli:
                %s
                """.formatted(
                orderNumber == null ? "-" : orderNumber,
                sessionId == null ? "-" : sessionId,
                paymentIntentId == null ? "-" : paymentIntentId,
                amountTotal == null ? "-" : amountTotal.toPlainString(),
                currencyCode == null ? "EUR" : currencyCode,
                formattedDate,
                itemSummary
        );

        String html = """
                <html>
                  <body style="font-family:Arial,sans-serif;color:#111;background:#fff;">
                    <h2>Pagamento completato</h2>
                    <p>Il tuo ordine è stato confermato correttamente.</p>
                    <ul>
                      <li><strong>Ordine:</strong> %s</li>
                      <li><strong>Sessione Stripe:</strong> %s</li>
                      <li><strong>Payment Intent:</strong> %s</li>
                      <li><strong>Importo:</strong> %s %s</li>
                      <li><strong>Data:</strong> %s</li>
                    </ul>
                    <h3>Articoli</h3>
                    <pre style="white-space:pre-wrap;font-family:inherit;">%s</pre>
                  </body>
                </html>
                """.formatted(
                escapeHtml(orderNumber == null ? "-" : orderNumber),
                escapeHtml(sessionId == null ? "-" : sessionId),
                escapeHtml(paymentIntentId == null ? "-" : paymentIntentId),
                escapeHtml(amountTotal == null ? "-" : amountTotal.toPlainString()),
                escapeHtml(currencyCode == null ? "EUR" : currencyCode),
                escapeHtml(formattedDate),
                escapeHtml(itemSummary)
        );

        sendEmail(recipientEmail, subject, text, html);
    }

    private void ensureConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Resend integration is disabled. Set app.resend.enabled=true and configure credentials.");
        }
        if (isBlank(properties.getApiKey()) || isBlank(properties.getFromEmail())) {
            throw new IllegalStateException("Resend configuration is incomplete. Set RESEND_API_KEY and RESEND_FROM_EMAIL.");
        }
        if (properties.getFromEmail().toLowerCase().contains("localhost")) {
            throw new IllegalStateException("RESEND_FROM_EMAIL is invalid. Use a verified sender address.");
        }
    }

    private void sendEmail(String recipientEmail, String subject, String text, String html) {
        String body = """
                {
                  "from": "%s",
                  "to": "%s",
                  "subject": "%s",
                  "text": "%s",
                  "html": "%s"
                }
                """.formatted(
                escapeJson(properties.getFromEmail()),
                escapeJson(recipientEmail),
                escapeJson(subject),
                escapeJson(text),
                escapeJson(html)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/emails"))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Resend returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Resend request was interrupted.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to send email through Resend.", ex);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
