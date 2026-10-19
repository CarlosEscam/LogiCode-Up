package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStudentEnablementRepository extends JpaRepository<StudentEnablement, UUID> {

    Optional<StudentEnablement> findByCedula(String cedula);

    boolean existsByCedula(String cedula);
}
