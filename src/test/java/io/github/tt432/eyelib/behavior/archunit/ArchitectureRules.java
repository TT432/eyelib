package io.github.tt432.eyelibbehavior.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 验证 eyelib-behavior 不依赖 MC 类型。
 * 排除项：
 * - EyelibBehaviorMod → Forge @Mod bootstrap（无法避免）
 *
 * @author TT432
 */
@NullMarked
class ArchitectureRules {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelibbehavior");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !c.getSimpleName().equals("EyelibBehaviorMod")))
                .and().resideInAPackage("io.github.tt432.eyelibbehavior..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
