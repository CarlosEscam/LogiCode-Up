package co.edu.unipamplona.logicode.identity.api;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.identity.application.EnableStudents;
import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import co.edu.unipamplona.logicode.shared.application.ApplicationException;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.V1 + "/student-enablements")
@Tag(name = "Cédulas habilitadas", description = "Carga de estudiantes por el docente (RF-05)")
public class StudentEnablementController {

    public static final long MAX_CSV_BYTES = 1024 * 1024;

    public record EnableRequest(@NotBlank @Size(max = 20) String cedula, @Size(max = 150) String fullName) {
    }

    public record EnablementResponse(UUID id, String cedula, String fullName, boolean registered, Instant enabledAt) {

        static EnablementResponse from(StudentEnablement e) {
            return new EnablementResponse(e.getId(), e.getCedula(), e.getFullName(), e.isRegistered(), e.getEnabledAt());
        }
    }

    private final EnableStudents enableStudents;

    public StudentEnablementController(EnableStudents enableStudents) {
        this.enableStudents = enableStudents;
    }

    @GetMapping
    @Operation(summary = "Listar cédulas habilitadas")
    public PageResult<EnablementResponse> list(@CurrentActor Actor actor,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return enableStudents.list(actor, page, size).map(EnablementResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Habilitar una cédula")
    public EnablementResponse enable(@CurrentActor Actor actor, @Valid @RequestBody EnableRequest request) {
        return EnablementResponse.from(enableStudents.enableOne(actor, request.cedula(), request.fullName()));
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Habilitar cédulas desde un archivo CSV (cedula[,nombre])")
    public EnableStudents.ImportResult importCsv(@CurrentActor Actor actor, @RequestPart("file") MultipartFile file)
            throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (file.isEmpty() || !name.endsWith(".csv")) {
            throw ApplicationException.businessRule("Sube un archivo .csv con una cédula por fila.");
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw ApplicationException.businessRule("El archivo CSV no puede superar 1 MB.");
        }
        return enableStudents.importCsv(actor, file.getInputStream());
    }
}
