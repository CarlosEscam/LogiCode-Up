package co.edu.unipamplona.logicode.identity.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface StudentEnablementRepository {

    StudentEnablement save(StudentEnablement enablement);

    Optional<StudentEnablement> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    /** Habilitaciones, de la más reciente a la más antigua; con {@code enabledBy} solo las de esa cuenta. */
    PageResult<StudentEnablement> list(UUID enabledBy, int page, int size);

    List<String> findAllCedulas();
}
