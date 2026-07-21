package fr.imta.acapela_webservice.model;

/// AppException.java
public abstract class AppException extends RuntimeException {
    private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() { return statusCode; }
    
    // USER EXCEPTION
    public static class UserNotFoundException extends AppException {
        public UserNotFoundException(Long id) {
            super(404, "User not found with id: " + id);
        }
    }

    public static class EmailNotValidException extends AppException {
        public EmailNotValidException() {
            super(304, "Email is not valid ");
        }
    }

    public static class EmailAlreadyExistException extends AppException {
        public EmailAlreadyExistException(String email) {
            super(305, "Email is already taken: " + email );
        }
    }

    public static class PasswordNotValidException extends AppException {
        public PasswordNotValidException() {
            super(505, "Password must be at least 12 characters long and contain at least one lowercase letter, one uppercase letter, one digit and one special character (!?$%*_-&@#)");
        }
    }
    
    public static class PasswordsNotSameException extends AppException {
        public PasswordsNotSameException() {
            super(505, "Passwords do not match");
        }
    }

    public static class HashageError extends AppException{
        public HashageError() {
            super(440,"Please try again later");
        }
    }

    public static class RoleNotValidException extends AppException{
        public RoleNotValidException() {
            super(400,"Role is not valid");
        }
    }

    // API EXCEPTION
    public static class ApiKeyAlreadyExistsException extends AppException {
        public ApiKeyAlreadyExistsException(String name) {
            super(409, "ApiKey already exists: " + name);
        }
    }

    public static class ApiKeyNotFoundException extends AppException {
        public ApiKeyNotFoundException(Long id) {
            super(404, "ApiKey not found with id: " + id);
        }
    }

    public static class CreditsNotValidException extends AppException {
        public CreditsNotValidException() {
            super(400, "Credits is not valid");
        }
    }

    public static class ApiKeyStatusNotValidException extends AppException {
        public ApiKeyStatusNotValidException() {
            super(400, "ApiKey status is not valid");
        }
    }

    //CACHE EXCEPTION

    public static class CacheNotFoundException extends AppException {
        public CacheNotFoundException(Long id) {
            super(404, "Cache not found with id: " + id);
        }
    }

    // REQUESTS

    public static class RequestNotFoundException extends AppException {
        public RequestNotFoundException(Long id) {
            super(404, "Request not found with id: " + id);
        }
    }

    public static class RequestStatusNotValidException extends AppException {
        public RequestStatusNotValidException() {
            super(400, "Request status is not valid");
        }
    }

    public static class RequestTypeNotValidException extends AppException {
        public RequestTypeNotValidException() {
            super(400, "Request type is not valid");
        }
    }

    public static class DateFormatNotValidException extends AppException {
        public DateFormatNotValidException () {
            super(400, "Date format is not valid");
        }
    }

    // GENERAL

    public static class IdNotValidException extends AppException {
        public IdNotValidException() {
            super(400, "Id is not valid");
        }
    }

    public static class AllArgRequiredException extends AppException {
        public AllArgRequiredException (String is_missing) {
            super(500, is_missing + " is required");
        }
    }

    public static class JsonBodyNotValidException extends AppException {
        public JsonBodyNotValidException() {
            super(400, "The JSON Body is not valid");
        }
    }

    public static class InternalServerException extends AppException {
        public InternalServerException() {
            super(300, "Internal server error");
        }
    }

    public static class UnauthorizedException extends AppException {
        public UnauthorizedException() {
            super(401, "Unauthorized");
        }
    }

    public static class ForbiddenException extends AppException {
        public ForbiddenException() {
            super(403, "Not enough rights");
        }
    }
}