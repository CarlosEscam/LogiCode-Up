package co.edu.unipamplona.logicode.system.api;

import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import co.edu.unipamplona.logicode.system.application.GetSystemInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/system")
@Tag(name = "Sistema", description = "Información técnica de la plataforma")
public class SystemController {

    private final GetSystemInfo getSystemInfo;

    public SystemController(GetSystemInfo getSystemInfo) {
        this.getSystemInfo = getSystemInfo;
    }

    @GetMapping("/info")
    @Operation(summary = "Consultar versión, entorno, hora del negocio y versión del esquema de base de datos")
    public SystemInfoResponse info() {
        return SystemInfoResponse.from(getSystemInfo.execute());
    }
}
