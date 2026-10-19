package co.edu.unipamplona.logicode.identity.infrastructure.persistence;

import java.util.Optional;

import co.edu.unipamplona.logicode.identity.application.StudentEnablementRepository;
import co.edu.unipamplona.logicode.identity.domain.StudentEnablement;
import co.edu.unipamplona.logicode.shared.application.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class StudentEnablementRepositoryAdapter implements StudentEnablementRepository {

    private final JpaStudentEnablementRepository jpa;

    StudentEnablementRepositoryAdapter(JpaStudentEnablementRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public StudentEnablement save(StudentEnablement enablement) {
        return jpa.save(enablement);
    }

    @Override
    public Optional<StudentEnablement> findByCedula(String cedula) {
        return jpa.findByCedula(cedula);
    }

    @Override
    public boolean existsByCedula(String cedula) {
        return jpa.existsByCedula(cedula);
    }

    @Override
    public PageResult<StudentEnablement> list(int page, int size) {
        return PageResults.of(jpa.findAll(PageRequest.of(page, size, Sort.by("enabledAt").descending())));
    }
}
