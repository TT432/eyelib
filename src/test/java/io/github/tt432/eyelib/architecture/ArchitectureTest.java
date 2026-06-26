/** @author TT432 */
package io.github.tt432.eyelib.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构约束规则（ADR-0016 四层模型）。首次运行 freeze 模式记录 baseline 违规，后续只检测新违规。
 */
@AnalyzeClasses(
        packages = "io.github.tt432.eyelib",
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
class ArchitectureTest {

    /**
     * Domain 层（ADR-0016）：领域模型，需零 MC import。
     * 按用户决策（"修改规则 1"）：molang.platform / particle.client / particle.network /
     * model.network / animation.network 等边缘子包不再被排除——它们物理上在 domain
     * 包路径下，逻辑上也必须是 domain。这迫使这些子包内的 MC 适配代码物理迁出到
     * bridge/application 层，与 ADR-0016 §1 "Domain = molang/" 的精确语义对齐。
     */
    private static final DescribedPredicate<JavaClass> DOMAIN_CLASSES =
            resideInAnyPackage(
                    "io.github.tt432.eyelib.importer..",
                    "io.github.tt432.eyelib.behavior..",
                    "io.github.tt432.eyelib.util..",
                    "io.github.tt432.eyelib.model..",
                    "io.github.tt432.eyelib.molang..",
                    "io.github.tt432.eyelib.material..",
                    "io.github.tt432.eyelib.animation..",
                    "io.github.tt432.eyelib.particle.."
            )
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.bridge.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.client.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.common.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.network.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.capability.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.attachment.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.track.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.event.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.debug.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.mixin.."))
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.smoke.."))
                    .as("domain classes");

    /**
     * Application 层（ADR-0016）：编排，通过 ACL 接触 MC，自身版本无关。
     */
    private static final DescribedPredicate<JavaClass> APPLICATION_CLASSES =
            resideInAnyPackage(
                    "io.github.tt432.eyelib.client..",
                    "io.github.tt432.eyelib.common..",
                    "io.github.tt432.eyelib.network..",
                    "io.github.tt432.eyelib.capability..",
                    "io.github.tt432.eyelib.attachment..",
                    "io.github.tt432.eyelib.track..",
                    "io.github.tt432.eyelib.event.."
            )
                    .and(resideOutsideOfPackage("io.github.tt432.eyelib.bridge.."))
                    .as("application classes");

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
     * 版本特定 MC 包（ADR-0016 §5）：loader API + 渲染后端。Application/Domain 禁止直接 import。
     */
    private static final DescribedPredicate<JavaClass> VERSION_SPECIFIC_MC =
            resideInAnyPackage(
                            "net.minecraftforge..",
                            "net.neoforged..",
                            "com.mojang.blaze3d.pipeline..",
                            "com.mojang.blaze3d.platform..",
                            "com.mojang.blaze3d.systems.."
                    )
                    .as("版本特定 MC 包（Forge / NeoForge / blaze3d pipeline+platform+systems）");

    /**
     * 允许接触版本特定 MC 的包：ACL（bridge）+ Infrastructure（mixin / smoke / debug）。
     */
    private static final DescribedPredicate<JavaClass> ALLOWED_VERSION_SPECIFIC_MC_HOSTS =
            resideInAnyPackage(
                    "io.github.tt432.eyelib.bridge..",
                    "io.github.tt432.eyelib.mixin..",
                    "io.github.tt432.eyelib.smoke..",
                    "io.github.tt432.eyelib.debug.."
            ).as("ACL + Infrastructure（版本特定 MC 的合法栖息地）");

    /**
     * orchestration / infrastructure 层（domain 不应反向依赖）。
     * 使用项目内绝对路径前缀，避免 ..network.. 等通配符误匹配 net.minecraft.network /
     * com.google.common / io.github.tt432.eyelib.behavior.event.logic 等外部或 domain 内部子包。
     */
    private static final DescribedPredicate<JavaClass> ORCHESTRATION =
            resideInAnyPackage(
                            "io.github.tt432.eyelib.client..",
                            "io.github.tt432.eyelib.common..",
                            "io.github.tt432.eyelib.network..",
                            "io.github.tt432.eyelib.capability..",
                            "io.github.tt432.eyelib.attachment..",
                            "io.github.tt432.eyelib.track..",
                            "io.github.tt432.eyelib.event..",
                            "io.github.tt432.eyelib.bridge..",
                            "io.github.tt432.eyelib.mixin..",
                            "io.github.tt432.eyelib.smoke..",
                            "io.github.tt432.eyelib.debug.."
                    )
                    .as("orchestration 层");

    /**
     * Domain 内部实现：所有 {@code io.github.tt432.eyelib.<module>.internal..} 子包。
     * 与 {@link #DOMAIN_CLASSES} 取交集确保只匹配 Domain 层的 internal/，
     * 不误伤 application/ACL 内同名子包。
     */
    private static final DescribedPredicate<JavaClass> DOMAIN_INTERNALS =
            DOMAIN_CLASSES.and(resideInAnyPackage("io.github.tt432.eyelib..internal.."))
                    .as("domain 内部实现子包（*.internal..）");

    // ===== ADR-0016 规则 =====

    /** 规则 1：Domain 层零 MC 依赖（Persistence Ignorance + Infrastructure Ignorance）。 */
    @ArchTest
    static final ArchRule domainMustNotDependOnMinecraft = freeze(
            noClasses().that(DOMAIN_CLASSES)
                    .should().dependOnClassesThat(BANNED_MC)
                    .because("ADR-0016: domain 必须通过 bridge(ACL) 访问 MC；DFU/FriendlyByteBuf/NBT 是允许的序列化基础设施")
    );

    /** 规则 2：Domain 层不反向依赖 Application / ACL / Infrastructure。 */
    @ArchTest
    static final ArchRule domainMustNotDependOnOrchestration = freeze(
            noClasses().that(DOMAIN_CLASSES)
                    .should().dependOnClassesThat(ORCHESTRATION)
                    .because("ADR-0016: domain 不得反向依赖编排层（client/common/network/bridge 等）")
    );

    /** 规则 3：版本特定 MC 包只能在 ACL(bridge) 和 Infrastructure(mixin/smoke/debug) 中使用。 */
    @ArchTest
    static final ArchRule versionSpecificMcOnlyInBridgeOrInfrastructure = freeze(
            noClasses().that(DescribedPredicate.not(ALLOWED_VERSION_SPECIFIC_MC_HOSTS))
                    .should().dependOnClassesThat(VERSION_SPECIFIC_MC)
                    .because("ADR-0016 §5: 版本特定 MC 包（Forge/NeoForge/blaze3d）的唯一栖息地是 bridge(ACL) 和 infrastructure；"
                            + "Domain 和 Application 必须通过 ACL 翻译接口访问")
    );

    /**
     * 规则 4：ACL(bridge) 不反向依赖 Application 层（ADR-0016 §2 禁止反向依赖）。
     * ACL 是翻译层，依赖方向只能是 ACL → Domain + MC；任何 ACL → Application 的引用
     * 都意味着 ACL 承担了不该承担的编排/数据持有职责，应抽 Port 后让 Application 反向注入。
     */
    @ArchTest
    static final ArchRule aclMustNotDependOnApplication = freeze(
            noClasses().that(resideInAnyPackage("io.github.tt432.eyelib.bridge.."))
                    .should().dependOnClassesThat(APPLICATION_CLASSES)
                    .because("ADR-0016 §2: 禁止反向依赖——ACL(bridge) 不得引用 Application 层；"
                            + "过渡期 bridge/ 引用 application 运行时数据类是已知债务，阶段 2 抽 Port 解决")
    );

    /**
     * 规则 6：Application 层只能依赖 Domain 的公开 API，禁止引用 Domain 内部实现。
     *
     * <p>命名约定：Domain 模块用 {@code <module>/internal/..} 子包封装内部实现，
     * 其余 {@code <module>/} 顶层和显式 {@code <module>/api/..} 视为公开 API。
     *
     * <p>过渡期 Domain 尚未物理划分 internal/，baseline 为 0 违规；规则一旦就位，
     * 未来在 Domain 内加 internal/ 子包时自动捕获 Application 的越界引用。
     */
    @ArchTest
    static final ArchRule applicationMustNotDependOnDomainInternals = freeze(
            noClasses().that(APPLICATION_CLASSES)
                    .should().dependOnClassesThat(DOMAIN_INTERNALS)
                    .because("ADR-0016 §2: Domain 内部实现（*.internal.. 子包）是 Domain 私有，"
                            + "Application 必须通过 Domain 公开 API 访问")
    );
}
