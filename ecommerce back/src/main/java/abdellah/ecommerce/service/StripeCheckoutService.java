package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.payment.StripeCartCheckoutRequest;
import abdellah.ecommerce.api.dto.payment.StripeCheckoutItemRequest;
import abdellah.ecommerce.api.dto.payment.StripeCheckoutResponse;
import abdellah.ecommerce.api.dto.payment.StripeCheckoutSnapshot;
import abdellah.ecommerce.api.dto.payment.StripeCheckoutStatusResponse;
import abdellah.ecommerce.api.dto.payment.StripeQuickBuyRequest;
import abdellah.ecommerce.config.FrontendProperties;
import abdellah.ecommerce.config.StripeProperties;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.domain.entity.CustomerOrder;
import abdellah.ecommerce.domain.entity.CustomerOrderItem;
import abdellah.ecommerce.domain.entity.CustomerProfile;
import abdellah.ecommerce.domain.entity.PaymentMethod;
import abdellah.ecommerce.domain.entity.PaymentTransaction;
import abdellah.ecommerce.domain.entity.Product;
import abdellah.ecommerce.domain.entity.ProductVariant;
import abdellah.ecommerce.domain.entity.ShippingMethod;
import abdellah.ecommerce.domain.entity.StripeCheckoutSession;
import abdellah.ecommerce.domain.enums.CheckoutType;
import abdellah.ecommerce.domain.enums.OrderPaymentStatus;
import abdellah.ecommerce.domain.enums.OrderStatus;
import abdellah.ecommerce.domain.enums.PaymentTransactionStatus;
import abdellah.ecommerce.domain.enums.StripeCheckoutStatus;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.AppUserRepository;
import abdellah.ecommerce.repository.CustomerOrderItemRepository;
import abdellah.ecommerce.repository.CustomerOrderRepository;
import abdellah.ecommerce.repository.CustomerProfileRepository;
import abdellah.ecommerce.repository.PaymentMethodRepository;
import abdellah.ecommerce.repository.PaymentTransactionRepository;
import abdellah.ecommerce.repository.ProductRepository;
import abdellah.ecommerce.repository.ProductVariantRepository;
import abdellah.ecommerce.repository.ShippingMethodRepository;
import abdellah.ecommerce.repository.StripeCheckoutSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class StripeCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutService.class);
    private static final String DEFAULT_SHIPPING_CODE = "STANDARD";
    private static final String DEFAULT_PAYMENT_CODE = "STRIPE";
    private static final String STRIPE_CHECKOUT_URL = "https://api.stripe.com/v1/checkout/sessions";

    private final StripeProperties stripeProperties;
    private final FrontendProperties frontendProperties;
    private final StripeCheckoutSessionRepository checkoutSessionRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AppUserRepository appUserRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerOrderItemRepository customerOrderItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentNotificationService paymentNotificationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StripeCheckoutService(StripeProperties stripeProperties,
                                 FrontendProperties frontendProperties,
                                 StripeCheckoutSessionRepository checkoutSessionRepository,
                                 ProductRepository productRepository,
                                 ProductVariantRepository productVariantRepository,
                                 CustomerProfileRepository customerProfileRepository,
                                 AppUserRepository appUserRepository,
                                 ShippingMethodRepository shippingMethodRepository,
                                 PaymentMethodRepository paymentMethodRepository,
                                 CustomerOrderRepository customerOrderRepository,
                                 CustomerOrderItemRepository customerOrderItemRepository,
                                 PaymentTransactionRepository paymentTransactionRepository,
                                 PaymentNotificationService paymentNotificationService,
                                 ObjectMapper objectMapper) {
        this.stripeProperties = stripeProperties;
        this.frontendProperties = frontendProperties;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.appUserRepository = appUserRepository;
        this.shippingMethodRepository = shippingMethodRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.customerOrderItemRepository = customerOrderItemRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentNotificationService = paymentNotificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StripeCheckoutResponse createCartCheckout(Long userId, String authenticatedEmail, StripeCartCheckoutRequest request) {
        validateEmail(userId, authenticatedEmail, request.customerEmail());
        return createCheckoutSession(userId, authenticatedEmail, CheckoutType.CART, buildSnapshots(request.items()));
    }

    @Transactional
    public StripeCheckoutResponse createQuickBuyCheckout(Long userId, String authenticatedEmail, StripeQuickBuyRequest request) {
        validateEmail(userId, authenticatedEmail, request.customerEmail());
        StripeCheckoutItemRequest itemRequest = new StripeCheckoutItemRequest(
                request.productId(),
                request.productVariantId(),
                request.quantity(),
                request.variantLabel()
        );
        return createCheckoutSession(userId, authenticatedEmail, CheckoutType.QUICK_BUY, buildSnapshots(List.of(itemRequest)));
    }

    @Transactional(readOnly = true)
    public StripeCheckoutStatusResponse getCheckoutStatus(String sessionId, String authenticatedEmail) {
        StripeCheckoutSession session = checkoutSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NotFoundApiException("STRIPE_SESSION_NOT_FOUND", "Stripe checkout session not found."));
        if (!normalizeEmail(session.getCustomerEmail()).equals(normalizeEmail(authenticatedEmail))) {
            throw new BadRequestApiException("CHECKOUT_SESSION_FORBIDDEN", "You cannot access this checkout session.");
        }
        return toStatusResponse(session);
    }

    @Transactional
    public void handleCheckoutSessionCompleted(String sessionId, String paymentIntentId, String rawPayload) {
        StripeCheckoutSession session = checkoutSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NotFoundApiException("STRIPE_SESSION_NOT_FOUND", "Stripe checkout session not found."));
        if (session.getStatus() == StripeCheckoutStatus.COMPLETED && session.getCustomerOrder() != null) {
            registerAfterCommit(() -> paymentNotificationService.publishFinalState(sessionId));
            return;
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(session.getCustomerEmail())
                .orElseThrow(() -> new NotFoundApiException("USER_NOT_FOUND", "Customer user not found."));
        CustomerProfile profile = customerProfileRepository.findWithDetailsByUser_Id(user.getId())
                .orElseThrow(() -> new NotFoundApiException("PROFILE_NOT_FOUND", "Customer profile not found."));

        ShippingMethod shippingMethod = resolveDefaultShippingMethod();
        PaymentMethod paymentMethod = resolveStripePaymentMethod();

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerProfile(profile);
        order.setShippingMethod(shippingMethod);
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        order.setSubtotalAmount(session.getAmountTotal());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(session.getAmountTotal());
        order.setCurrencyCode(session.getCurrencyCode());
        order.setPlacedAt(Instant.now());
        customerOrderRepository.saveAndFlush(order);

        List<StripeCheckoutSnapshot> snapshots = readSnapshots(session.getPayloadJson());
        for (StripeCheckoutSnapshot snapshot : snapshots) {
            Product product = productRepository.findById(snapshot.productId())
                    .orElseThrow(() -> new NotFoundApiException("PRODUCT_NOT_FOUND", "Product not found for checkout item."));
            ProductVariant variant = resolveVariantForOrder(product, snapshot.productVariantId(), snapshot.variantSnapshot());

            CustomerOrderItem orderItem = new CustomerOrderItem();
            orderItem.setCustomerOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductVariant(variant);
            orderItem.setSkuSnapshot(variant.getSku());
            orderItem.setProductNameSnapshot(snapshot.productName());
            orderItem.setVariantSnapshot(snapshot.variantSnapshot());
            orderItem.setQuantity(snapshot.quantity());
            orderItem.setUnitPrice(snapshot.unitPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setTaxAmount(BigDecimal.ZERO);
            orderItem.setLineTotal(snapshot.lineTotal());
            orderItem.setCurrencyCode(snapshot.currencyCode());
            customerOrderItemRepository.save(orderItem);
            order.getItems().add(orderItem);
        }

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setCustomerOrder(order);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setTransactionReference(sessionId);
        transaction.setProviderReference(paymentIntentId);
        transaction.setPaymentStatus(PaymentTransactionStatus.CAPTURED);
        transaction.setAmount(session.getAmountTotal());
        transaction.setRefundedAmount(BigDecimal.ZERO);
        transaction.setCurrencyCode(session.getCurrencyCode());
        transaction.setAuthorizedAt(Instant.now());
        transaction.setCapturedAt(Instant.now());
        transaction.setGatewayResponse(rawPayload);
        paymentTransactionRepository.save(transaction);
        order.getPaymentTransactions().add(transaction);

        session.setCustomerOrder(order);
        session.setPaymentIntentId(paymentIntentId);
        session.setStatus(StripeCheckoutStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        session.setFailureReason(null);
        checkoutSessionRepository.save(session);
        registerAfterCommit(() -> paymentNotificationService.publishFinalState(sessionId));

        log.info("Stripe checkout completed for session {} and order {}", sessionId, order.getOrderNumber());
    }

    @Transactional
    public void handleCheckoutSessionCanceled(String sessionId, String rawPayload) {
        checkoutSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setStatus(StripeCheckoutStatus.CANCELED);
            session.setCanceledAt(Instant.now());
            session.setFailureReason(rawPayload);
            checkoutSessionRepository.save(session);
            registerAfterCommit(() -> paymentNotificationService.publishCurrentState(sessionId));
        });
    }

    private StripeCheckoutResponse createCheckoutSession(Long userId,
                                                         String authenticatedEmail,
                                                         CheckoutType checkoutType,
                                                         List<StripeCheckoutSnapshot> snapshots) {
        ShippingMethod shippingMethod = resolveDefaultShippingMethod();
        BigDecimal amountTotal = calculateTotal(snapshots, shippingMethod);
        String currencyCode = resolveCurrency(snapshots);
        String successUrl = buildRedirectUrl(stripeProperties.getSuccessPath());
        String cancelUrl = buildRedirectUrl(stripeProperties.getCancelPath());

        String payload = buildStripeFormBody(snapshots, authenticatedEmail, userId, checkoutType, successUrl, cancelUrl, shippingMethod);

        StripeGatewayResponse gatewayResponse = createStripeSession(payload, authenticatedEmail);
        String payloadJson = writeSnapshots(snapshots);

        StripeCheckoutSession session = new StripeCheckoutSession();
        session.setSessionId(gatewayResponse.sessionId());
        session.setPaymentIntentId(gatewayResponse.paymentIntentId());
        session.setCheckoutType(checkoutType);
        session.setStatus(StripeCheckoutStatus.PENDING);
        session.setCustomerEmail(authenticatedEmail);
        session.setAmountTotal(amountTotal);
        session.setCurrencyCode(currencyCode);
        session.setSuccessUrl(successUrl);
        session.setCancelUrl(cancelUrl);
        session.setPayloadJson(payloadJson);
        checkoutSessionRepository.saveAndFlush(session);

        registerAfterCommit(() -> paymentNotificationService.publishCurrentState(session.getSessionId()));

        log.info("Created Stripe checkout session {} for {}", gatewayResponse.sessionId(), authenticatedEmail);
        return new StripeCheckoutResponse(
                gatewayResponse.sessionId(),
                gatewayResponse.url(),
                session.getStatus().name(),
                amountTotal,
                currencyCode
        );
    }

    private StripeGatewayResponse createStripeSession(String formBody, String authenticatedEmail) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(STRIPE_CHECKOUT_URL))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + stripeProperties.getSecretKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("Stripe session creation failed for {} with status {} and body {}", authenticatedEmail, response.statusCode(), response.body());
                throw new BadRequestApiException("STRIPE_SESSION_FAILED", "Payment session could not be created.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String sessionId = root.path("id").asText(null);
            String url = root.path("url").asText(null);
            String paymentIntentId = root.path("payment_intent").asText(null);
            if (sessionId == null || url == null) {
                throw new BadRequestApiException("STRIPE_SESSION_FAILED", "Payment session could not be created.");
            }
            return new StripeGatewayResponse(sessionId, url, paymentIntentId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestApiException("STRIPE_SESSION_FAILED", "Payment session could not be created.");
        } catch (IOException ex) {
            log.error("Stripe session creation IO failure for {}: {}", authenticatedEmail, ex.getMessage(), ex);
            throw new BadRequestApiException("STRIPE_SESSION_FAILED", "Payment session could not be created.");
        }
    }

    private String buildStripeFormBody(List<StripeCheckoutSnapshot> snapshots,
                                       String customerEmail,
                                       Long userId,
                                       CheckoutType checkoutType,
                                       String successUrl,
                                       String cancelUrl,
                                       ShippingMethod shippingMethod) {
        List<String> params = new ArrayList<>();
        addParam(params, "mode", "payment");
        addParam(params, "success_url", successUrl);
        addParam(params, "cancel_url", cancelUrl);
        addParam(params, "customer_email", customerEmail);
        addParam(params, "client_reference_id", String.valueOf(userId));
        addParam(params, "metadata[checkout_type]", checkoutType.name());
        addParam(params, "metadata[customer_email]", customerEmail);
        addParam(params, "metadata[shipping_method]", shippingMethod.getCode());

        for (int index = 0; index < snapshots.size(); index++) {
            StripeCheckoutSnapshot snapshot = snapshots.get(index);
            String prefix = "line_items[" + index + "]";
            addParam(params, prefix + "[quantity]", String.valueOf(snapshot.quantity()));
            addParam(params, prefix + "[price_data][currency]", snapshot.currencyCode().toLowerCase(Locale.ROOT));
            addParam(params, prefix + "[price_data][unit_amount]", String.valueOf(toMinorUnits(snapshot.unitPrice(), snapshot.currencyCode())));
            addParam(params, prefix + "[price_data][product_data][name]", snapshot.productName());
            addParam(params, prefix + "[price_data][product_data][description]", snapshot.variantSnapshot());
            addParam(params, prefix + "[price_data][product_data][metadata][product_id]", String.valueOf(snapshot.productId()));
            addParam(params, prefix + "[price_data][product_data][metadata][product_variant_id]", String.valueOf(snapshot.productVariantId()));
            addParam(params, prefix + "[price_data][product_data][metadata][product_code]", snapshot.productCode());
        }

        return String.join("&", params);
    }

    private List<StripeCheckoutSnapshot> buildSnapshots(List<StripeCheckoutItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestApiException("CHECKOUT_ITEMS_REQUIRED", "At least one checkout item is required.");
        }

        Map<String, StripeCheckoutSnapshot> snapshots = new LinkedHashMap<>();
        for (StripeCheckoutItemRequest item : items) {
            if (item.quantity() <= 0) {
                throw new BadRequestApiException("INVALID_QUANTITY", "Item quantity must be greater than zero.");
            }
            Product product = productRepository.findById(item.productId())
                    .filter(Product::getActive)
                    .orElseThrow(() -> new NotFoundApiException("PRODUCT_NOT_FOUND", "Product not found."));
            ProductVariant variant = resolveVariantForCheckout(product, item.productVariantId(), item.variantLabel());

            BigDecimal unitPrice = resolveVariantPrice(variant);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            String key = product.getId() + ":" + variant.getId();
            snapshots.put(key, new StripeCheckoutSnapshot(
                    product.getId(),
                    variant.getId(),
                    item.quantity(),
                    product.getProductCode(),
                    product.getName(),
                    buildVariantSnapshot(variant, item.variantLabel()),
                    unitPrice,
                    lineTotal,
                    product.getCurrencyCode()
            ));
        }

        return new ArrayList<>(snapshots.values());
    }

    private ProductVariant resolveVariantForCheckout(Product product, Long requestedVariantId, String requestedVariantLabel) {
        if (requestedVariantId != null) {
            ProductVariant variant = productVariantRepository.findWithProductById(requestedVariantId)
                    .orElseThrow(() -> new NotFoundApiException("PRODUCT_VARIANT_NOT_FOUND", "Product variant not found."));
            if (variant.getProduct() == null || !Objects.equals(variant.getProduct().getId(), product.getId())) {
                throw new BadRequestApiException("PRODUCT_VARIANT_MISMATCH", "The selected variant does not belong to the product.");
            }
            if (!Boolean.TRUE.equals(variant.getActive())) {
                throw new BadRequestApiException("PRODUCT_VARIANT_INACTIVE", "The selected variant is not available.");
            }
            return variant;
        }

        List<ProductVariant> variants = productVariantRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId()).stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getActive()))
                .toList();
        if (!variants.isEmpty()) {
            return variants.get(0);
        }

        return createDefaultVariant(product, requestedVariantLabel);
    }

    private ProductVariant resolveVariantForOrder(Product product, Long variantId, String variantSnapshot) {
        if (variantId != null) {
            return productVariantRepository.findWithProductById(variantId)
                    .filter(candidate -> candidate.getProduct() != null && Objects.equals(candidate.getProduct().getId(), product.getId()))
                    .orElseGet(() -> createDefaultVariant(product, variantSnapshot));
        }
        return createDefaultVariant(product, variantSnapshot);
    }

    private ProductVariant createDefaultVariant(Product product, String variantLabel) {
        String sku = generateDefaultSku(product);
        return productVariantRepository.findBySkuIgnoreCase(sku)
                .filter(variant -> variant.getProduct() != null && Objects.equals(variant.getProduct().getId(), product.getId()))
                .orElseGet(() -> {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(product);
                    variant.setSku(sku);
                    variant.setListPrice(product.getListPrice());
                    variant.setSalePrice(product.getSalePrice());
                    variant.setCurrencyCode(product.getCurrencyCode());
                    variant.setStockQuantity(Math.max(product.getStockQuantity() == null ? 0 : product.getStockQuantity(), 0));
                    variant.setReservedQuantity(0);
                    variant.setActive(Boolean.TRUE);
                    return productVariantRepository.saveAndFlush(variant);
                });
    }

    private PaymentMethod resolveStripePaymentMethod() {
        return paymentMethodRepository.findByMethodCodeIgnoreCase(DEFAULT_PAYMENT_CODE)
                .orElseGet(() -> {
                    PaymentMethod method = new PaymentMethod();
                    method.setMethodCode(DEFAULT_PAYMENT_CODE);
                    method.setMethodName("Stripe Checkout");
                    method.setProvider("Stripe");
                    method.setSupportsRefunds(Boolean.TRUE);
                    method.setActive(Boolean.TRUE);
                    return paymentMethodRepository.saveAndFlush(method);
                });
    }

    private ShippingMethod resolveDefaultShippingMethod() {
        return shippingMethodRepository.findByCodeIgnoreCase(DEFAULT_SHIPPING_CODE)
                .orElseGet(() -> {
                    ShippingMethod method = new ShippingMethod();
                    method.setCode(DEFAULT_SHIPPING_CODE);
                    method.setName("Standard shipping");
                    method.setDescription("Default shipping method for Stripe checkout.");
                    method.setBasePrice(BigDecimal.ZERO);
                    method.setDeliveryDaysMin(0);
                    method.setDeliveryDaysMax(0);
                    method.setActive(Boolean.TRUE);
                    return shippingMethodRepository.saveAndFlush(method);
                });
    }

    private BigDecimal calculateTotal(List<StripeCheckoutSnapshot> snapshots, ShippingMethod shippingMethod) {
        BigDecimal subtotal = snapshots.stream()
                .map(StripeCheckoutSnapshot::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.add(shippingMethod.getBasePrice() == null ? BigDecimal.ZERO : shippingMethod.getBasePrice());
    }

    private String resolveCurrency(List<StripeCheckoutSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return "EUR";
        }
        return snapshots.get(0).currencyCode().toUpperCase(Locale.ROOT);
    }

    private StripeCheckoutStatusResponse toStatusResponse(StripeCheckoutSession session) {
        String orderNumber = session.getCustomerOrder() == null ? null : session.getCustomerOrder().getOrderNumber();
        String message = switch (session.getStatus()) {
            case COMPLETED -> "Payment completed successfully.";
            case CANCELED -> "Payment canceled.";
            case EXPIRED -> "Checkout session expired.";
            case FAILED -> "Payment failed.";
            case PENDING -> "Payment session pending.";
        };
        return new StripeCheckoutStatusResponse(
                session.getSessionId(),
                session.getStatus().name(),
                session.getCheckoutType().name(),
                orderNumber,
                session.getPaymentIntentId(),
                session.getCustomerEmail(),
                session.getAmountTotal(),
                session.getCurrencyCode(),
                message
        );
    }

    private List<StripeCheckoutSnapshot> readSnapshots(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return List.of();
        }
        try {
            StripeCheckoutSnapshot[] snapshots = objectMapper.readValue(payloadJson, StripeCheckoutSnapshot[].class);
            return List.of(snapshots);
        } catch (JsonProcessingException ex) {
            throw new BadRequestApiException("CHECKOUT_PAYLOAD_INVALID", "Stored checkout payload could not be read.");
        }
    }

    private String writeSnapshots(List<StripeCheckoutSnapshot> snapshots) {
        try {
            return objectMapper.writeValueAsString(snapshots);
        } catch (JsonProcessingException ex) {
            throw new BadRequestApiException("CHECKOUT_PAYLOAD_INVALID", "Checkout payload could not be serialized.");
        }
    }

    private String buildRedirectUrl(String path) {
        String baseUrl = frontendProperties.getBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath + "?session_id={CHECKOUT_SESSION_ID}";
    }

    private void validateEmail(Long userId, String authenticatedEmail, String requestEmail) {
        String canonicalAuth = normalizeEmail(authenticatedEmail);
        String canonicalRequest = normalizeEmail(requestEmail);
        if (canonicalAuth.isEmpty() || canonicalRequest.isEmpty()) {
            throw new BadRequestApiException("EMAIL_REQUIRED", "Customer email is required.");
        }
        if (!canonicalAuth.equals(canonicalRequest)) {
            throw new BadRequestApiException("EMAIL_MISMATCH", "The provided email does not match the authenticated account.");
        }
        if (userId == null) {
            throw new BadRequestApiException("AUTH_REQUIRED", "Authentication is required to proceed with checkout.");
        }
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }

        action.run();
    }

    private String buildVariantSnapshot(ProductVariant variant, String requestedLabel) {
        List<String> parts = new ArrayList<>();
        if (requestedLabel != null && !requestedLabel.isBlank()) {
            parts.add(requestedLabel.trim());
        }
        if (variant.getSizeCode() != null && !variant.getSizeCode().isBlank()) {
            parts.add(variant.getSizeCode().trim());
        }
        if (variant.getColorName() != null && !variant.getColorName().isBlank()) {
            parts.add(variant.getColorName().trim());
        }
        if (parts.isEmpty()) {
            parts.add("Default");
        }
        return String.join(" / ", parts);
    }

    private BigDecimal resolveVariantPrice(ProductVariant variant) {
        BigDecimal salePrice = variant.getSalePrice();
        if (salePrice != null) {
            return salePrice;
        }
        return variant.getListPrice();
    }

    private String generateOrderNumber() {
        String candidate = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        while (customerOrderRepository.existsByOrderNumberIgnoreCase(candidate)) {
            candidate = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        }
        return candidate;
    }

    private String generateDefaultSku(Product product) {
        String base = product.getProductCode() == null ? "PRODUCT" : product.getProductCode().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        String suffix = "-DEFAULT";
        String candidate = base + suffix;
        if (candidate.length() > 80) {
            candidate = candidate.substring(0, 80);
        }
        return candidate;
    }

    private long toMinorUnits(BigDecimal amount, String currencyCode) {
        int fractionDigits;
        try {
            fractionDigits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        } catch (IllegalArgumentException ex) {
            fractionDigits = 2;
        }
        if (fractionDigits < 0) {
            fractionDigits = 2;
        }
        BigDecimal scaled = amount.movePointRight(fractionDigits).setScale(0, RoundingMode.HALF_UP);
        return scaled.longValueExact();
    }

    private void addParam(List<String> params, String key, String value) {
        params.add(encode(key) + "=" + encode(value));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record StripeGatewayResponse(String sessionId, String url, String paymentIntentId) {
    }
}
