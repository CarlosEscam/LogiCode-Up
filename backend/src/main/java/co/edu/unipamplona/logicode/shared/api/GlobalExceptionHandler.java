package co.edu.unipamplona.logicode.shared.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce errores a respuestas JSON con formato Problem Details (RFC 9457) y mensajes en español.
 * Los errores de Spring MVC (validación, ruta inexistente, método no permitido) los resuelve la clase base.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<ApplicationException.Kind, HttpStatus> STATUS = Map.of(
            ApplicationException.Kind.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED,
            ApplicationException.Kind.FORBIDDEN, HttpStatus.FORBIDDEN,
            ApplicationException.Kind.NOT_FOUND, HttpStatus.NOT_FOUND,
            ApplicationException.Kind.CONFLICT, HttpStatus.CONFLICT,
            ApplicationException.Kind.BUSINESS_RULE, HttpStatus.UNPROCESSABLE_CONTENT,
            ApplicationException.Kind.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS);

    private static final Map<HttpStatus, String> TITLES = Map.of(
            HttpStatus.UNAUTHORIZED, "Sesión requerida",
            HttpStatus.FORBIDDEN, "Acceso denegado",
            HttpStatus.NOT_FOUND, "Recurso no encontrado",
            HttpStatus.CONFLICT, "Conflicto",
            HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida",
            HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes");

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplication(ApplicationException ex) {
        HttpStatus status = STATUS.get(ex.kind());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(TITLES.get(status));
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido.");
        problem.setTitle("Archivo demasiado grande");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.CONTENT_TOO_LARGE, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", fieldMessage(error.getCode())))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Revisa los datos del formulario.");
        problem.setTitle("Datos inválidos");
        problem.setProperty("errors", fields);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "El recurso solicitado no existe.");
        problem.setTitle("Recurso no encontrado");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde.");
        problem.setTitle("Error interno");
        problem.setType(URI.create("about:blank"));
        return problem;
    }

    private static String fieldMessage(String code) {
        if (code == null) {
            return "Valor inválido.";
        }
        return switch (code) {
            case "NotBlank", "NotNull" -> "Este campo es obligatorio.";
            case "Size" -> "La longitud no es válida.";
            default -> "Valor inválido.";
        };
    }
}
