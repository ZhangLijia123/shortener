package com.example.shortener.web;

import com.example.shortener.web.dto.ApiError;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Safety net: anything the container routes to {@code /error} (container-level 404, filter
 * failures, error dispatches that bypass {@link GlobalExceptionHandler}) still gets the
 * standard error body instead of Spring Boot's default {@code {"timestamp","path",...}}.
 *
 * <p>Declaring an {@link ErrorController} bean replaces {@code BasicErrorController}.</p>
 */
@RestController
public class FallbackErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiError> handle(HttpServletRequest request) {
        int status = resolveStatus(request);
        String error = switch (status) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_FAILED";
        };
        String message = switch (status) {
            case 404 -> "no handler found for this request";
            case 405 -> "http method not supported for this path";
            case 415 -> "content type must be application/json";
            default -> status >= 500 ? "unexpected error" : "request could not be processed";
        };
        return ResponseEntity.status(status).body(new ApiError(status, error, message));
    }

    private static int resolveStatus(HttpServletRequest request) {
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (attribute instanceof Integer value) {
            return value;
        }
        return 500;
    }
}
