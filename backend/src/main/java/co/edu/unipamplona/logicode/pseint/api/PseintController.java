package co.edu.unipamplona.logicode.pseint.api;

import co.edu.unipamplona.logicode.pseint.application.RunPrograms;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/pseint")
@Tag(name = "PSeInt", description = "Intérprete de PSeInt con límites de pasos, tiempo, salida y memoria (RF-16, RNF-05)")
public class PseintController {

    public record RunRequest(@NotNull @Size(max = 20000) String code, @Size(max = 10000) String input) {
    }

    private final RunPrograms runPrograms;

    public PseintController(RunPrograms runPrograms) {
        this.runPrograms = runPrograms;
    }

    @PostMapping("/runs")
    @Operation(summary = "Ejecutar un programa con sus entradas (una por línea); requiere sesión")
    public RunPrograms.ProgramRun run(@Valid @RequestBody RunRequest request) {
        return runPrograms.run(request.code(), request.input());
    }
}
