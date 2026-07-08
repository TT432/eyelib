/** @author TT432 */
package io.github.tt432.eyelib.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

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

    // ===== ADR-0018 IQF 规则（片段形状判据）=====

    /**
     * Java record 类：超类为 {@code java.lang.Record}。
     * record 是不可变数据载体（DTO），不是服务实现，规则 8 不要求 record 变 interface。
     * I-5 不将 record 计为「bridge 具体类」（没有行为多态性）。
     */
    private static final DescribedPredicate<JavaClass> RECORD_CLASSES =
            DescribedPredicate.<JavaClass>describe("record",
                    jc -> jc.getSuperclass()
                            .map(sc -> sc.getName())
                            .filter("java.lang.Record"::equals)
                            .isPresent());

    /**
     * Java enum 类：超类为 {@code java.lang.Enum}。
     * enum 是常量定义，不是服务实现，规则 8 不要求 enum 变 interface。
     */
    private static final DescribedPredicate<JavaClass> ENUM_CLASSES =
            DescribedPredicate.<JavaClass>describe("enum",
                    jc -> jc.getSuperclass()
                            .map(sc -> sc.getName())
                            .filter("java.lang.Enum"::equals)
                            .isPresent());

    /**
     * 超类为 Minecraft 平台类（{@code net.minecraft..}）的 bridge 类。
     * 这些类是 MC API 的 bridge wrapper（如 extends Screen / AbstractWidget），
     * application extends 它们是因为需要是 MC 类型子类，不是「依赖 bridge 服务实现」。
     */
    private static final DescribedPredicate<JavaClass> EXTENDS_MC_CLASS =
            DescribedPredicate.<JavaClass>describe("extends a Minecraft class",
                    jc -> jc.getSuperclass()
                            .map(sc -> sc.getName())
                            .filter(name -> name.startsWith("net.minecraft."))
                            .isPresent());

    /**
     * bridge 具体类：包路径在 {@code bridge..} 但不是接口、注解、record 或 MC 平台子类。
     * record 是不可变数据载体（DTO），没有行为多态性，不是「具体实现」。
     * Application 引用 bridge record（如 packet、RenderEntityParams）不违反 I-5 精神。
     * extends MC 类的 bridge wrapper 是 MC API 适配层，application extends 它不违反 I-5 精神。
     */
    private static final DescribedPredicate<JavaClass> BRIDGE_CONCRETE_CLASSES =
            resideInAnyPackage("io.github.tt432.eyelib.bridge..")
                    .and(DescribedPredicate.not(JavaClass.Predicates.INTERFACES))
                    .and(DescribedPredicate.not(RECORD_CLASSES))
                    .and(DescribedPredicate.not(EXTENDS_MC_CLASS))
                    .as("bridge 具体类（非接口、非注解、非 record、非 MC 平台子类）");

    /**
     * Forge 生命周期入口类：带 {@code @Mod} / {@code @EventBusSubscriber} 注解或名称匹配 {@code Forge*Discovery}。
     * ADR-0018 line 234 明确允许这些类在 {@code bridge/<feature>/} 直接子级（仅 Mod 加载器与 bridge adapter 内部使用，
     * 不被 Application import）。
     */
    private static final DescribedPredicate<JavaClass> FORGE_LIFECYCLE_ENTRIES =
            DescribedPredicate.<JavaClass>describe("Forge lifecycle entry (@Mod/@EventBusSubscriber/Forge*Discovery)",
                    jc -> jc.getAnnotations().stream().anyMatch(a -> {
                        String annName = a.getRawType().getName();
                        return annName.contains("EventBusSubscriber")
                                || annName.equals("net.minecraftforge.fml.common.Mod")
                                || annName.equals("net.neoforged.fml.common.Mod");
                    }) || jc.getName().matches(".*\\.Forge\\w*Discovery$")
            ).as("Forge 生命周期入口类（ADR-0018 line 234 允许在 bridge 直接子级）");

    /**
     * abstract class：带 {@code abstract} 修饰符。
     * abstract class 是部分实现（模板方法 / Port 骨架），不是具体服务实现。
     * 具体逻辑由子类（在 adapter/ 或 application）完成，规则 8 不要求 abstract class 变 interface。
     */
    private static final DescribedPredicate<JavaClass> ABSTRACT_CLASSES =
            DescribedPredicate.<JavaClass>describe("abstract class",
                    jc -> jc.getModifiers().contains(JavaModifier.ABSTRACT));

    /**
     * bridge 包内的 public 顶层类（排除内部类、adapter/ 子包、Forge 生命周期入口、record、enum、abstract class）。
     * 用于规则 8：bridge 公开 API 必须是接口或注解。
     * record / enum 是数据载体 / 常量定义，abstract class 是模板骨架，均不是具体服务实现。
     */
    private static final DescribedPredicate<JavaClass> BRIDGE_PUBLIC_TOP_LEVEL_CLASSES =
            resideInAnyPackage("io.github.tt432.eyelib.bridge..")
                    .and(DescribedPredicate.not(resideInAnyPackage("io.github.tt432.eyelib.bridge..adapter..")))
                    .and(DescribedPredicate.not(FORGE_LIFECYCLE_ENTRIES))
                    .and(DescribedPredicate.not(RECORD_CLASSES))
                    .and(DescribedPredicate.not(ENUM_CLASSES))
                    .and(DescribedPredicate.not(ABSTRACT_CLASSES))
                    .and(DescribedPredicate.describe("public top-level",
                            jc -> jc.getModifiers().contains(JavaModifier.PUBLIC)
                                    && !jc.getName().contains("$")))
                    .as("bridge 包内的 public 顶层类（排除 adapter/ 子包、Forge 生命周期入口、record、enum、abstract class）");

    /**
     * ADR-0018 规则 6（判据 I-2）：Domain 包不得暴露服务定位器（{@code public static final Xxx INSTANCE}）。
     * P3 review 已完成：record 类的 INSTANCE 是不可变 null object / 标记组件（flyweight），已在
     * {@link #bePublicStaticFinalInstanceField()} 的 check 方法中排除（超类 {@code java.lang.Record} 检测）。
     * 用 {@code noFields()} API 让 condition 直接作用于 JavaField，避免 JavaClass.getFields() 的歧义。
     */
    @ArchTest
    static final ArchRule domainMustNotUseSingletonInstance = freeze(
            noFields()
                    .that(declaredInDomainClasses())
                    .should(bePublicStaticFinalInstanceField())
                    .because("ADR-0018 I-2: domain 不得暴露服务定位器（INSTANCE singleton）；"
                            + "不可变 null object（MolangNull/EmptyComponent 等）通过 baseline 区分，P3 review")
    );

    /**
     * ADR-0018 规则 7（判据 I-5 + ADR-0016 §5）：Application 层不得依赖 ACL（bridge）的具体类。
     * 仅允许依赖 Port 接口（{@code *Port}）、反射调度注解（{@code On*}）、domain 层契约（{@code *Discovery} 等）。
     * 对齐 {@code molang/mapping/} 范式（机制 E）。
     */
    @ArchTest
    static final ArchRule applicationMustNotDependOnBridgeConcreteClasses = freeze(
            noClasses().that(APPLICATION_CLASSES)
                    .should().dependOnClassesThat(BRIDGE_CONCRETE_CLASSES)
                    .because("ADR-0018 I-5: Application 只能依赖 Port 接口 + 反射调度注解（机制 E）；"
                            + "禁止直接调用 bridge 具体类（adapter 实现类等）")
    );

    /**
     * ADR-0018 规则 8（判据 I-5 反向校验）：ACL（bridge）对 Application 仅暴露接口与反射调度注解。
     * bridge 包内的 public 顶层类必须是 interface 或 {@code @interface}（注解类型 {@code isInterface()} 返回 true）。
     * P2 阶段引入 {@code adapter/} 子包后，规则收紧为"bridge 直接子级"（排除 adapter/）。
     */
    @ArchTest
    static final ArchRule aclPublicApiMustBeInterfaceOrAnnotation = freeze(
            classes().that(BRIDGE_PUBLIC_TOP_LEVEL_CLASSES)
                    .should().beInterfaces()
                    .because("ADR-0018 I-5: ACL 开放契约——bridge 对 Application 仅暴露接口与注解，"
                            + "具体实现收到 adapter/ 子包（机制 E）；注解类型 isInterface() 返回 true，被本规则允许")
                    .allowEmptyShould(true)
    );

    /**
     * 字段谓词：字段声明在 Domain 类中。
     * 用于规则 6：把 {@link #DOMAIN_CLASSES} 类谓词转成字段谓词。
     */
    private static DescribedPredicate<JavaField> declaredInDomainClasses() {
        return DescribedPredicate.describe("declared in domain classes",
                f -> DOMAIN_CLASSES.test(f.getOwner()));
    }

    /**
     * 字段 condition：字段名为 INSTANCE 且修饰符为 public static final（服务定位器模式）。
     * 用 {@code SimpleConditionEvent.satisfied} 而非 {@code violated}：因为外层是
     * {@code noFields().should(this)}，语义为"没有字段应该满足此 condition"，
     * 满足时需产生 satisfied event 才能被 noFields 判定为违规。
     */
    private static ArchCondition<JavaField> bePublicStaticFinalInstanceField() {
        return new ArchCondition<JavaField>("be a public static final field named INSTANCE (service locator)") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                // P3 review 结论：record 的 INSTANCE 是不可变 null object / 标记组件（flyweight），不是服务定位器。
                // 一阶逻辑：record 不可变 ∧ 服务定位器可变 ⟹ record.INSTANCE ¬ServiceLocator。
                if (field.getOwner().getSuperclass()
                        .map(sc -> sc.getName())
                        .filter("java.lang.Record"::equals)
                        .isPresent()) {
                    return;
                }
                Set<JavaModifier> mods = field.getModifiers();
                if ("INSTANCE".equals(field.getName())
                        && mods.contains(JavaModifier.PUBLIC)
                        && mods.contains(JavaModifier.STATIC)
                        && mods.contains(JavaModifier.FINAL)) {
                    events.add(SimpleConditionEvent.satisfied(field,
                            field.getOwner().getSimpleName() + "." + field.getName()));
                }
            }
        };
    }
}
