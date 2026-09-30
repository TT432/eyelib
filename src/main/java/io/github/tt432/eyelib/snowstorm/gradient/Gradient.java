package io.github.tt432.eyelib.snowstorm.gradient;

import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snowstorm v3.2.2 {@code gradient.js} 逐字移植（原作者 JannisX11，GPL-3.0）：
 * {@code class Gradient extends Input}，value 为 {@link GradientStop} 列表。
 *
 * <p>偏离记录：
 * <ul>
 *   <li>{@code change(e, node, test)} 的 e 是取色器事件（{@code e.hex8}）、node 是 DOM 节点
 *       （{@code node.parentNode.querySelector(':active')} 检测拖拽中）；两者抽象为
 *       {@link ColorChangeEvent}/{@link SliderNode} 接缝。JS 第三参 test 未被使用，不移植。</li>
 *   <li>Config JSON 回填的 value 元素是普通 Map，构造/setValue 时就地归一为
 *       {@link GradientStop}（本身是 Map，键序一致，导出字节级不变）。</li>
 *   <li>{@code removePoint} 中 JS {@code indexOf} 是 === 引用比较；GradientStop 作为 Map
 *       的内容 equals 会误判同值不同对象，故按引用扫描（as-is）。</li>
 * </ul>
 */
public class Gradient extends Input {

    /** JS 取色器事件接缝：{@code e.hex8}。 */
    public interface ColorChangeEvent {
        String hex8();
    }

    /** JS DOM 节点接缝：{@code node.parentNode.querySelector(':active')} 真值检测（拖拽滑动中）。 */
    public interface SliderNode {
        boolean isActive();
    }

    /** JS {@code this.default_value}：白 → 黑两个 stop。 */
    public final List<GradientStop> default_value;

    /** JS {@code this.selected}：当前选中 stop（value 内引用）。 */
    public @Nullable GradientStop selected;

    public Gradient(Data data) {
        super(data);
        this.default_value = new ArrayList<>(List.of(
                new GradientStop(0, "#ffffffff", SnowstormUtil.bbuid(8)),
                new GradientStop(100, "#000000ff", SnowstormUtil.bbuid(8))
        ));
        normalizeValue();
        // JS：if (!this.value.length) this.value.splice(0, 0, ...this.default_value)
        if (value().isEmpty()) {
            value().addAll(0, this.default_value);
        }
        this.selected = value().get(0);
    }

    /** Input value 的类型化视图（元素保证为 GradientStop，见 {@link #normalizeValue()}）。 */
    @SuppressWarnings("unchecked")
    public List<GradientStop> value() {
        // JS：gradient 构造后 value 恒为数组（Input 构造器 data.value || []）；null 即用法错误（as-is 抛）
        return (List<GradientStop>) java.util.Objects.requireNonNull(getValue());
    }

    /** Config 回填的 Map 元素就地替换为 GradientStop（Java 类型化适配，JS 为鸭子类型）。 */
    private void normalizeValue() {
        Object v = getValue();
        if (!(v instanceof List<?> list)) return;
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            if (!(element instanceof GradientStop) && element instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                List<Object> mutable = (List<Object>) list;
                mutable.set(i, GradientStop.fromJson(element));
            }
        }
    }

    @Override
    public void setValue(@Nullable Object v) {
        super.setValue(v);
        normalizeValue();
    }

    /**
     * JS {@code change(e, node, test)}：写入选中 stop 颜色并 registerEdit；
     * 拖拽滑动中（node 处于 :active）走 typing 600ms 合并。
     */
    public Gradient change(ColorChangeEvent e, @Nullable SliderNode node) {
        if (this.selected != null) {
            this.selected.color(e.hex8());
        }
        // JS：let is_sliding = node && node.parentNode.querySelector(':active')
        boolean isSliding = node != null && node.isActive();
        EditHistory.registerEdit("change gradient", isSliding);
        return this;
    }

    /** JS {@code update(Data)}：按 percent 找回 selected（super.update 可能重排 value）。 */
    @Override
    public Gradient update(@Nullable Map<String, InputStructure.Subject> Data) {
        if (this.selected != null) {
            double selectedPercent = this.selected.percent();
            List<GradientStop> v = value();
            // JS：this.value.findIndex(point => (point.percent == this.selected.percent))
            int selectedIndex = -1;
            for (int i = 0; i < v.size(); i++) {
                if (v.get(i).percent() == selectedPercent) {
                    selectedIndex = i;
                    break;
                }
            }
            // JS：super.update()（无参，Data=undefined）。Java 不能调 Input 无参 update()——它会
            // 经 this.update(null) 动态分派回本覆盖方法造成无限递归；直接 super.update(null) 等价。
            super.update(null);
            // JS：this.value[Math.clamp(selected_index, 0, length-1)]——value 为空时 clamp 得 -1，
            // JS value[-1] = undefined（selected 变 undefined，不抛）；Java 映射为 null（as-is 语义）
            int idx = (int) SnowstormUtil.clamp(selectedIndex, 0, v.size() - 1);
            this.selected = idx >= 0 && idx < v.size() ? v.get(idx) : null;
        }
        return this;
    }

    /** JS {@code reset()}：恢复默认白→黑（与 default_value 共享 stop 引用，as-is）。 */
    @Override
    public Gradient reset() {
        List<GradientStop> v = value();
        v.clear();
        v.addAll(this.default_value);
        this.selected = v.get(0);
        return this;
    }

    /** JS {@code sortValues()}：按 percent 升序（JS sort 与 Java sort 均稳定）。 */
    public Gradient sortValues() {
        value().sort(Comparator.comparingDouble(GradientStop::percent));
        return this;
    }

    /**
     * JS {@code export(range)}：导出 {@code {time: '#AARRGGBB'}}。
     * time = roundTo(percent/100*range, 2)，无小数点补 ".0"；颜色 #RRGGBBAA → #AA+RRGGBB。
     */
    public Map<String, String> export(double range) {
        Map<String, String> obj = new LinkedHashMap<>();
        for (GradientStop point : value()) {
            double time = (point.percent() / 100) * range;
            // JS：Math.roundTo(time, 2).toString()
            String timeStr = JsSemantics.toJsString(SnowstormUtil.roundTo(time, 2));
            if (timeStr.indexOf('.') < 0) timeStr += ".0";
            String color = point.color();
            obj.put(timeStr, "#" + JsSemantics.jsSubstr(color, 7, 2) + JsSemantics.jsSubstr(color, 1, 6));
        }
        return obj;
    }

    /** JS 同名方法（遮蔽全局 registerEdit）：{@code registerEdit('update gradient')}。 */
    public Gradient registerEdit() {
        EditHistory.registerEdit("update gradient");
        return this;
    }

    /** JS {@code addPoint()}：新 stop 无 id 键（as-is），加入后选中并排序。 */
    public Gradient addPoint() {
        GradientStop point = new GradientStop();
        point.percent(50);
        point.color("#ffffffff");
        value().add(point);
        this.selected = value().get(value().size() - 1);
        sortValues();
        EditHistory.registerEdit("add gradient point");
        return this;
    }

    /** JS {@code removePoint()}：至少保留 2 个 stop；删除后选中前一个（JS false-1=-1 怪癖 as-is）。 */
    public Gradient removePoint() {
        List<GradientStop> v = value();
        if (v.size() > 2) {
            // JS Array.remove：indexOf 为 === 引用比较，返回下标或 false
            int index = -1;
            for (int i = 0; i < v.size(); i++) {
                if (v.get(i) == this.selected) {
                    index = i;
                    break;
                }
            }
            if (index > -1) {
                v.remove(index);
            }
            // JS：i 为 false 时 Math.clamp(false-1, ...) = clamp(-1, ...) = 0
            double i = index > -1 ? index : -1;
            this.selected = v.get((int) SnowstormUtil.clamp(i - 1, 0, v.size() - 1));
            EditHistory.registerEdit("remove gradient point");
        }
        return this;
    }
}
