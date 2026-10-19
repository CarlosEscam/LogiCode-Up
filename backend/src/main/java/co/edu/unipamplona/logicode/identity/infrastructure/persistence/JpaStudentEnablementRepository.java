package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaStudentEnablementRepository extends JpaRepository<StudentEnablement, UUID> {

    Optional<StudentEnablement> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    @Query("select e.cedula from StudentEnablement e order by e.enabledAt")
    List<String> findAllCedulas();
}
