package io.github.tt432.eyelib.particle.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 验证 eyelib-particle 不依赖 MC 类型。
 * 排除项及其解决轮次：
 * - EyelibParticleMod → Forge @Mod bootstrap（无法避免）
 * - client/* → MC 渲染代码（R4 提取后迁移至 bridge）
 * - network/* → FriendlyByteBuf（StreamCodec 硬性要求）
 *
 * @author TT432
 */
@NullMarked
class ArchitectureRules {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelib.particle");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibParticleMod")
                                || c.getPackageName().matches(".*\\.client($|\\..*)")
                                || c.getPackageName().matches(".*\\.network($|\\..*)"))))
                .and().resideInAPackage("io.github.tt432.eyelib.particle..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
