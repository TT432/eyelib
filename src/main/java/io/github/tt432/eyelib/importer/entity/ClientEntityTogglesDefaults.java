package io.github.tt432.eyelib.importer.entity;

import io.github.tt432.eyelib.molang.MolangValue;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 装备渲染开关（{@link BrClientEntity.Toggles}）的缺省值注册表。
 * <p>
 * 动机：不同 Bedrock 版本 / 不同整合包对四个开关的缺省期望不同（例如某些包默认渲染 attachable）。
 * 缺省值必须同时作用于解码与编码两侧——解码缺键时取当前缺省，编码时「值等于当前缺省」即省略该键，
 * 与 DFU {@code optionalFieldOf(name, literalDefault)} 的语义同构，从而保证缺省不写回、往返不被污染。
 * <p>
 * 注册覆盖语义：同一 {@link Field} 后注册者覆盖先注册者；{@link BrClientEntityNormalizations.Handle#close()}
 * 恢复先前注册（或字段出厂缺省）。
 *
 * @author TT432
 */
public final class ClientEntityTogglesDefaults {
    /**
     * {@code enable_attachables}：能否渲染 attachable（盔甲与武器），出厂缺省 {@code false}。
     */
    public static final Field<Boolean> ENABLE_ATTACHABLES = new Field<>("enable_attachables", false);

    /**
     * {@code hide_armor}：只压盔甲，出厂缺省 {@code false}。
     */
    public static final Field<Boolean> HIDE_ARMOR = new Field<>("hide_armor", false);

    /**
     * {@code held_item_ignores_lighting}：手持物是否全亮渲染，出厂缺省 {@code false}。
     */
    public static final Field<Boolean> HELD_ITEM_IGNORES_LIGHTING = new Field<>("held_item_ignores_lighting", false);

    /**
     * {@code held_item_scale}：手持物缩放 Molang 表达式，出厂缺省 {@link MolangValue#ONE}。
     */
    public static final Field<MolangValue> HELD_ITEM_SCALE = new Field<>("held_item_scale", MolangValue.ONE);

    /**
     * 缺省值字段句柄。无公开构造器：只能在 {@link ClientEntityTogglesDefaults} 内定义，
     * 外部只能引用常量，避免出现注册表里不存在的「野字段」。
     *
     * @param <T> 字段值的 Java 类型
     */
    public static final class Field<T> {
        private final String jsonName;
        private final T factoryDefault;

        private Field(String jsonName, T factoryDefault) {
            this.jsonName = jsonName;
            this.factoryDefault = factoryDefault;
        }

        /** JSON 里的键名（与 {@code Toggles} 的字段名一致）。 */
        public String jsonName() {
            return jsonName;
        }
    }

    /**
     * 一条注册记录（字段 + 覆盖值），按引用身份注销；后注册者在解析时优先。
     * 故意不覆写 equals：身份等价即注册条目等价，确保关闭句柄只移除自己那一条。
     */
    private static final class Registration {
        private final Field<?> field;
        private final Object value;

        private Registration(Field<?> field, Object value) {
            this.field = field;
            this.value = value;
        }
    }

    private static final CopyOnWriteArrayList<Registration> REGISTRATIONS = new CopyOnWriteArrayList<>();

    private ClientEntityTogglesDefaults() {
    }

    /**
     * 注册字段缺省值，返回可注销句柄。
     * <p>
     * 同一字段多次注册时，后注册者覆盖先注册者；关闭句柄后回落到前一次注册（没有则回落到出厂缺省）。
     */
    public static <T> BrClientEntityNormalizations.Handle register(Field<T> field, T value) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(value, "value");
        Registration registration = new Registration(field, value);
        REGISTRATIONS.add(registration);
        return new BrClientEntityNormalizations.Handle() {
            private volatile boolean closed;

            @Override
            public void close() {
                synchronized (this) {
                    if (closed) {
                        return;
                    }
                    closed = true;
                }
                REGISTRATIONS.remove(registration);
            }
        };
    }

    /**
     * 字段当前生效的缺省值：最后一次仍有效的注册，没有注册则为出厂缺省。
     */
    @SuppressWarnings("unchecked")
    public static <T> T of(Field<T> field) {
        Objects.requireNonNull(field, "field");
        List<Registration> registrations = REGISTRATIONS;
        for (int i = registrations.size() - 1; i >= 0; i--) {
            Registration registration = registrations.get(i);
            if (registration.field == field) {
                return (T) registration.value;
            }
        }
        return field.factoryDefault;
    }

    /**
     * 当前生效缺省值组装成的 {@link BrClientEntity.Toggles}，供便捷构造器补齐缺省。
     */
    public static BrClientEntity.Toggles effective() {
        return new BrClientEntity.Toggles(
                of(ENABLE_ATTACHABLES),
                of(HIDE_ARMOR),
                of(HELD_ITEM_IGNORES_LIGHTING),
                of(HELD_ITEM_SCALE));
    }

    /**
     * 清空所有注册（恢复出厂缺省）。仅供测试使用。
     */
    public static void clear() {
        REGISTRATIONS.clear();
    }
}
