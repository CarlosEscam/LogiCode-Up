package co.edu.unipamplona.logicode.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Reglas de dependencia entre capas del monolito modular (ver docs/arquitectura.md).
 */
@AnalyzeClasses(packages = "co.edu.unipamplona.logicode", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "..api..", "..application..", "..infrastructure..")
            .because("el dominio contiene reglas puras del negocio");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnApiOrInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..")
            .because("los casos de uso dependen de puertos, no de implementaciones técnicas ni de HTTP");

    @ArchTest
    static final ArchRule apiDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("los controladores llaman casos de uso, no repositorios ni adaptadores")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersLiveInApi = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("..api..");

    @ArchTest
    static final ArchRule noJdbcOrJpaInApi = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.jdbc..", "org.springframework.data..", "jakarta.persistence..")
            .because("los controladores no ejecutan consultas directas");

    private static final String ROOT = "co.edu.unipamplona.logicode.";

    @ArchTest
    static final ArchRule modulesTalkThroughApplicationOnly = classes()
            .should(new ArchCondition<JavaClass>("no usar api, domain o infrastructure de otro módulo") {
                @Override
                public void check(JavaClass origin, ConditionEvents events) {
                    String from = moduleOf(origin.getPackageName());
                    for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                        String target = dependency.getTargetClass().getPackageName();
                        String to = moduleOf(target);
                        boolean internal = target.matches(".*\\.(api|domain|infrastructure)(\\..*)?");
                        if (to != null && from != null && !to.equals(from) && !to.equals("shared") && internal) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                }
            })
            .because("un módulo usa a otro solo a través de su capa application");

    private static String moduleOf(String packageName) {
        if (!packageName.startsWith(ROOT)) {
            return null;
        }
        String rest = packageName.substring(ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
