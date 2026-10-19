package co.edu.unipamplona.logicode.identity.application;

import java.util.List;
import java.util.Optional;

import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.shared.application.PageResult;

public interface StudentEnablementRepository {

    StudentEnablement save(StudentEnablement enablement);

    Optional<StudentEnablement> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    PageResult<StudentEnablement> list(int page, int size);

    List<String> findAllCedulas();
}
