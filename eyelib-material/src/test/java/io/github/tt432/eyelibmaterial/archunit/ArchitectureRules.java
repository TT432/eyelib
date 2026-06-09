package io.github.tt432.eyelibmaterial.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 验证 eyelib-material 不依赖 MC 类型。
 * 排除项及其解决轮次：
 * - EyelibMaterialMod → Forge @Mod bootstrap（无法避免）
 * - BrShaderMapping → R2 迁移至 bridge
 * - BrMaterialEntry → R2 提取 buildCustomRenderType 到 bridge
 *
 * @author TT432
 */
@NullMarked
class ArchitectureRules {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelibmaterial");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibMaterialMod")
                                || c.getSimpleName().equals("BrShaderMapping")
                                || c.getSimpleName().equals("BrMaterialEntry"))))
                .and().resideInAPackage("io.github.tt432.eyelibmaterial..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }

    @Test
    void portInterfacesHaveNoMcDependency() {
        noClasses()
                .that().resideInAPackage("io.github.tt432.eyelibmaterial.port..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
