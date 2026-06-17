package io.github.tt432.eyelib.molang.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 验证 eyelib-molang 不依赖 MC 类型。
 * 排除项：
 * - EyelibMolangMod → Forge @Mod bootstrap（无法避免）
 * - platform/* → 平台绑定代码（按 ROADMAP 保留在 molang 中）
 *
 * @author TT432
 */
@NullMarked
class ArchitectureRules {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelib.molang");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibMolangMod")
                                || c.getPackageName().contains(".platform."))))
                .and().resideInAPackage("io.github.tt432.eyelib.molang..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }

    @Test
    void portInterfacesHaveNoMcDependency() {
        noClasses()
                .that().resideInAPackage("io.github.tt432.eyelib.molang.port..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
