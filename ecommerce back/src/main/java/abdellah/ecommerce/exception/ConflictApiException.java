package abdellah.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class ConflictApiException extends ApiException {

    public ConflictApiException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
