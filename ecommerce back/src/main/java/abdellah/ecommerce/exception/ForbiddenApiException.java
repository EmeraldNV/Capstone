package abdellah.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenApiException extends ApiException {

    public ForbiddenApiException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
