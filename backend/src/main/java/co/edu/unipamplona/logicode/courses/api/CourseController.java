package co.edu.unipamplona.logicode.courses.api;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import co.edu.unipamplona.logicode.courses.application.CourseView;
import co.edu.unipamplona.logicode.courses.application.EnrollStudents;
import co.edu.unipamplona.logicode.courses.application.ManageCourses;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.V1 + "/courses")
@Tag(name = "Cursos", description = "Cursos e inscripción de estudiantes")
public class CourseController {

    static final long MAX_LIST_BYTES = 1024 * 1024;

    public record CourseRequest(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 20) String period,
            @Size(max = 2000) String description) {
    }

    public record EnrollRequest(@NotBlank @Size(max = 20) String cedula, @Size(max = 150) String fullName) {
    }

    private final ManageCourses manageCourses;
    private final EnrollStudents enrollStudents;

    public CourseController(ManageCourses manageCourses, EnrollStudents enrollStudents) {
        this.manageCourses = manageCourses;
        this.enrollStudents = enrollStudents;
    }

    @GetMapping
    @Operation(summary = "Cursos del usuario (todos, para un Administrador)")
    public List<CourseView> list(@CurrentActor Actor actor) {
        return manageCourses.list(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un curso; quien lo crea queda como docente responsable")
    public CourseView create(@CurrentActor Actor actor, @Valid @RequestBody CourseRequest request) {
        return manageCourses.create(actor, request.name(), request.period(), request.description());
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Consultar un curso")
    public CourseView get(@CurrentActor Actor actor, @PathVariable UUID courseId) {
        return manageCourses.get(actor, courseId);
    }

    @PutMapping("/{courseId}")
    @Operation(summary = "Editar nombre, periodo y descripción de un curso")
    public CourseView update(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @Valid @RequestBody CourseRequest request) {
        return manageCourses.update(actor, courseId, request.name(), request.period(), request.description());
    }

    @GetMapping("/{courseId}/enrollments")
    @Operation(summary = "Estudiantes inscritos")
    public PageResult<EnrollStudents.EnrolledStudent> enrollments(@CurrentActor Actor actor,
            @PathVariable UUID courseId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return enrollStudents.list(actor, courseId, page, size);
    }

    @PostMapping("/{courseId}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscribir un estudiante por cédula (también habilita la cédula)")
    public EnrollStudents.EnrolledStudent enroll(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @Valid @RequestBody EnrollRequest request) {
        return enrollStudents.enrollOne(actor, courseId, request.cedula(), request.fullName());
    }

    @PostMapping(path = "/{courseId}/enrollments/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Inscribir estudiantes desde un archivo CSV o Excel .xlsx (cedula[,nombre])")
    public EnrollStudents.ImportResult importList(@CurrentActor Actor actor, @PathVariable UUID courseId,
            @RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw ApplicationException.businessRule("Sube un archivo .csv o .xlsx con una cédula por fila.");
        }
        if (file.getSize() > MAX_LIST_BYTES) {
            throw ApplicationException.businessRule("El archivo no puede superar 1 MB.");
        }
        return enrollStudents.importFile(actor, courseId, file.getInputStream(), file.getOriginalFilename());
    }

    @DeleteMapping("/{courseId}/enrollments/{cedula}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirar a un estudiante del curso")
    public void unenroll(@CurrentActor Actor actor, @PathVariable UUID courseId, @PathVariable String cedula) {
        enrollStudents.unenroll(actor, courseId, cedula);
    }
}
