package abdellah.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class BadRequestApiException extends ApiException {

    public BadRequestApiException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
