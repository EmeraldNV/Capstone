package abdellah.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class NotFoundApiException extends ApiException {

    public NotFoundApiException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
