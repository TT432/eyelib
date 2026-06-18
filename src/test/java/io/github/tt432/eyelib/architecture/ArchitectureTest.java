/** @author TT432 */
package io.github.tt432.eyelib.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构约束规则。首次运行 freeze 模式记录 baseline 违规，后续只检测新违规。
 */
@AnalyzeClasses(packages = "io.github.tt432.eyelib")
class ArchitectureTest {

    /**
     * domain 包（leaf domain，需零 MC import）。
     */
    private static final DescribedPredicate<JavaClass> DOMAIN_CLASSES =
            resideInAnyPackage(
                    "..importer..",
                    "..behavior..",
                    "..util..",
                    "..model..",
                    "..molang..",
                    "..material..",
                    "..animation..",
                    "..particle.."
            )
                    .and(resideOutsideOfPackage("..bridge.."))
                    .and(resideOutsideOfPackage("..client.."))
                    .and(resideOutsideOfPackage("..common.."))
                    .and(resideOutsideOfPackage("..network.."))
                    .and(resideOutsideOfPackage("..capability.."))
                    .and(resideOutsideOfPackage("..attachment.."))
                    .and(resideOutsideOfPackage("..track.."))
                    .and(resideOutsideOfPackage("..event.."))
                    .and(resideOutsideOfPackage("..debug.."))
                    .and(resideOutsideOfPackage("..mixin.."))
                    .and(resideOutsideOfPackage("..molang.platform.."))
                    .and(resideOutsideOfPackage("..particle.client.."))
                    .and(resideOutsideOfPackage("..particle.network.."))
                    .and(resideOutsideOfPackage("..model.network.."))
                    .and(resideOutsideOfPackage("..animation.network.."))
                    .as("domain classes");

    /**
     * MC 序列化白名单：DFU / FriendlyByteBuf / NBT / ExtraCodecs。
     */
    private static final DescribedPredicate<JavaClass> MC_WHITELIST =
            resideInAPackage("com.mojang.serialization..")
                    .or(resideInAPackage("com.mojang.datafixers.."))
                    .or(resideInAPackage("net.minecraft.nbt.."))
                    .or(resideInAPackage("net.minecraft.network"))
                    .or(resideInAPackage("net.minecraft.util"))
                    .as("序列化白名单（DFU / FriendlyByteBuf / NBT / ExtraCodecs）");

    /**
     * 禁止的 MC/Forge 依赖（白名单外）。
     */
    private static final DescribedPredicate<JavaClass> BANNED_MC =
            resideInAnyPackage(
                            "net.minecraft..",
                            "net.minecraftforge..",
                            "net.neoforged..",
                            "com.mojang.blaze3d.."
                    )
                    .and(DescribedPredicate.not(MC_WHITELIST));

    /**
     * orchestration 层（domain 不应反向依赖）。
     */
    private static final DescribedPredicate<JavaClass> ORCHESTRATION =
            resideInAnyPackage(
                            "..client..",
                            "..common..",
                            "..network..",
                            "..capability..",
                            "..attachment..",
                            "..track..",
                            "..event..",
                            "..bridge..",
                            "..mixin..",
                            "..debug.."
                    )
                    .as("orchestration 层");

    @ArchTest
    static final ArchRule domainMustNotDependOnMinecraft = freeze(
            noClasses().that(DOMAIN_CLASSES)
                    .should().dependOnClassesThat(BANNED_MC)
                    .because("domain 必须通过 Port/Bridge 访问 MC；DFU/FriendlyByteBuf/NBT 是允许的序列化基础设施")
    );

    @ArchTest
    static final ArchRule domainMustNotDependOnOrchestration = freeze(
            noClasses().that(DOMAIN_CLASSES)
                    .should().dependOnClassesThat(ORCHESTRATION)
                    .because("domain 不得反向依赖 orchestration 层（client/common/network/bridge 等）")
    );
}
