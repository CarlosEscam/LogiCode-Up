package co.edu.unipamplona.logicode.administration.api;

import co.edu.unipamplona.logicode.administration.application.AdministrationPanel;
import co.edu.unipamplona.logicode.administration.application.PlatformOverview;
import co.edu.unipamplona.logicode.identity.application.Actor;
import co.edu.unipamplona.logicode.identity.application.CurrentActor;
import co.edu.unipamplona.logicode.shared.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
@Tag(name = "Panel de administración", description = "Resumen de la plataforma y respaldos (RNF-09, RF-28)")
public class AdministrationController {

    private final AdministrationPanel panel;

    public AdministrationController(AdministrationPanel panel) {
        this.panel = panel;
    }

    @GetMapping("/overview")
    @Operation(summary = "Cifras de la plataforma: cuentas, cursos, actividades, entregas, biblioteca y foro")
    public PlatformOverview overview(@CurrentActor Actor actor) {
        return panel.overview(actor);
    }

    @GetMapping("/backups")
    @Operation(summary = "Respaldos disponibles, estado del servicio y resultado de la prueba de restauración")
    public AdministrationPanel.Backups backups(@CurrentActor Actor actor) {
        return panel.backups(actor);
    }

    @PostMapping("/backups")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pedir un respaldo inmediato; 409 si ya hay uno solicitado")
    public AdministrationPanel.Backups requestBackup(@CurrentActor Actor actor) {
        return panel.requestBackup(actor);
    }
}
