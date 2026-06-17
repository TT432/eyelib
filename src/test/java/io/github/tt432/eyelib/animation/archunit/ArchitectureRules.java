package io.github.tt432.eyelib.animation.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 验证 eyelib-animation 不依赖 MC 类型。
 * 排除项及其解决轮次：
 * - EyelibAnimationMod → Forge @Mod bootstrap（无法避免）
 * - AnimationComponentSyncPacket → FriendlyByteBuf（StreamCodec 硬性要求）
 * - BrAnimationEntryDefinition, BrControllerExecutor → Entity 引用（R4 将使用 PortEntity）
 *
 * @author TT432
 */
@NullMarked
class ArchitectureRules {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelib.animation");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibAnimationMod")
                                || c.getPackageName().matches(".*\\.network($|\\..*)")
                                || c.getSimpleName().equals("BrAnimationEntryDefinition")
                                || c.getSimpleName().equals("BrControllerExecutor"))))
                .and().resideInAPackage("io.github.tt432.eyelib.animation..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
