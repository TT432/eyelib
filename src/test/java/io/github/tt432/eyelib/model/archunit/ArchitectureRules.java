package io.github.tt432.eyelibmodel.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 验证 eyelib-model 不依赖 MC 类型。
 * 排除项：
 * - EyelibModelMod → Forge @Mod bootstrap（无法避免）
 * - network.packet/* → FriendlyByteBuf（StreamCodec 硬性要求）
 * - Model.java → ExtraCodecs.lazyInitializedCodec（Java record CODEC 辅助）
 *
 * @author TT432
 */
@NullMarked
class ArchitectureRules {

    private static final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.tt432.eyelibmodel");

    @Test
    void domainLayerHasNoMcDependency() {
        noClasses()
                .that(DescribedPredicate.describe("not excluded",
                        c -> !(c.getSimpleName().equals("EyelibModelMod")
                                || c.getPackageName().contains(".network.packet")
                                || c.getName().startsWith("io.github.tt432.eyelibmodel.Model"))))
                .and().resideInAPackage("io.github.tt432.eyelibmodel..")
                .should().dependOnClassesThat()
                .resideInAPackage("net.minecraft..")
                .check(classes);
    }
}
