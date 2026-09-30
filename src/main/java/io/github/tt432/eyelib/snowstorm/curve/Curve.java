package io.github.tt432.eyelib.snowstorm.curve;

import io.github.tt432.eyelib.snowstorm.editor.EditHistory;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.input.Input;
import io.github.tt432.eyelib.snowstorm.input.InputStructure;
import io.github.tt432.eyelib.snowstorm.input.InputType;
import io.github.tt432.eyelib.snowstorm.util.SnowstormUtil;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.JsonValues;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Snowstorm curves.js 逐字移植（Curve 类 + updateCurvesPanel）。
 *
 * <p>一条曲线 = inputs{id, mode, input, range} + nodes。nodes 元素为 {@link Double}
 * （linear/catmull_rom/bezier）或 {@link Config.BezierNode}（bezier_chain）。
 * {@link #config} 是暴露给 {@code Config.curves} 的曲线对象（wintersky 侧
 * {@code Emitter.calculateCurve} 直接消费）。
 *
 * <p>与 JS 的差异（可观察行为等价，详见移植报告偏离清单）：
 * <ul>
 *   <li>JS config 的 input/range/mode 为惰性 getter，此处改为在 Input onchange 时即时同步
 *       （{@link #syncConfig()}）；JS 源中绕过 change() 直接写 {@code .value} 的调用点仅有
 *       import.js 对 id 的写入，而 id 不在 config 内，故无可见差异。</li>
 *   <li>构造函数末尾的 {@code setTimeout(20) + Vue.nextTick(updateSVG)} 改为直接置位
 *       {@link #svg_needs_update}（无 Vue 调度器，标志最终同为 true）。</li>
 *   <li>{@code updateCurvesPanel} 的 Vue.nextTick 延迟同样去除。</li>
 *   <li>JS {@code removeNode(index, event)} 的 event 参数函数体未使用，略去。</li>
 * </ul>
 */
public class Curve {

    /** JS this.uuid = guid()。 */
    public final String uuid = SnowstormUtil.guid();
    /** JS this.svg_needs_update（UI 刷新标志，由 Vue watch 消费；数据层 as-is 保留）。 */
    public boolean svg_needs_update = false;

    /** JS this.inputs = {id, mode, input, range}（LinkedHashMap 保序，对应 for-in 迭代序）。 */
    public final LinkedHashMap<String, Input> inputs = new LinkedHashMap<>();

    /**
     * JS this.nodes：数值数组（linear/catmull_rom/bezier）或 bezier_chain 节点对象数组。
     * 与 {@link #config}.nodes 为同一 List 实例（JS 中 {@code this.config.nodes === this.nodes}）。
     */
    public final List<Object> nodes;

    /** JS this.selected_point。 */
    public int selected_point = -1;
    /** 以下 svg_* 字段为 UI 阶段（Curve.vue updateSVG）的模型侧载体，as-is 保留。 */
    public String svg_data = "";
    public String bezier_handles = "";
    public String vertical_line_data = "";
    public String horizontal_line_data = "";

    /** JS this.min / this.max（updateMinMax 自适应，clamp ±256）。 */
    public double min = 0;
    public double max = 1;

    /**
     * JS this.config = {get input, get range, get mode, nodes}：暴露给 Config.curves 的形态。
     * nodes 与 {@link #nodes} 共享同一 List；input/range/mode 由 {@link #syncConfig()} 即时同步。
     */
    public final Config.Curve config = new Config.Curve();

    /** JS new Curve()（data 缺省 = 0，falsy 分支）。 */
    public Curve() {
        this(null);
    }

    /**
     * JS new Curve(data)：data 为 Config.curves 中的曲线对象
     * （import.js：{@code new Curve(Config.curves[id])}）。
     */
    public Curve(Config.@Nullable Curve data) {
        // JS: this.nodes = data.nodes instanceof Array ? data.nodes : [0, 1, 0]
        // （Java 侧 Config.Curve.nodes 恒为 List；元素逐个搬入 config.nodes，
        //   BezierNode 对象引用与 JS 一样共享，List 容器本身为新建）
        this.nodes = this.config.nodes;
        if (data != null) {
            this.nodes.addAll(data.nodes);
        } else {
            this.nodes.add(0.0);
            this.nodes.add(1.0);
            this.nodes.add(0.0);
        }

        Input id = new Input(new Input.Data()
                .label("Name")
                .info("The Molang variable to be used later in Molang expressions. Must begin with \"variable.\"")
                .placeholder("variable.curve1")
                .type(InputType.TEXT)
                .value(data != null ? "" : "variable.")
                // JS: onchange() { scope.updateName(this.value); }（this = 该 Input）
                .onchange(event -> updateName(JsSemantics.toJsString(in("id").getValue()))));
        Input mode = new Input(new Input.Data()
                .type(InputType.SELECT)
                .label("Mode")
                .info("Curve interpolation type")
                // JS: ['catmull_rom','linear','bezier','bezier_chain'].includes(data.mode) ? data.mode : 'linear'
                // （data 缺省时 data.mode 为 undefined，includes 为 false）
                .value(data != null && CurveMode.byId(data.mode) != null ? data.mode : CurveMode.LINEAR.id)
                .options(CurveMode.optionPairs())
                .onchange(event -> {
                    updateSVG();
                    if ("bezier".equals(in("mode").getValue())) {
                        // JS: nodes.splice(4, Infinity, 0, 0, 0, 0); nodes.splice(4);
                        // 净效果：恰好保留前 4 个节点，不足补 0
                        while (nodes.size() > 4) nodes.remove(nodes.size() - 1);
                        while (nodes.size() < 4) nodes.add(0.0);
                    }
                    if ("bezier_chain".equals(in("mode").getValue())) {
                        // JS: nodes.splice(0, Infinity, {time:0,...}, {time:1,...})
                        nodes.clear();
                        nodes.add(bezierNode(0, 0, 0, 0, 0));
                        nodes.add(bezierNode(1, 1, 1, 0, 0));
                    } else if (nodes.stream().anyMatch(n -> n instanceof Config.BezierNode)) {
                        // JS: typeof n == 'object' → nodes[i] = n.left_value || 0
                        for (int i = 0; i < nodes.size(); i++) {
                            if (nodes.get(i) instanceof Config.BezierNode node) {
                                nodes.set(i, JsSemantics.truthy(node.left_value) ? node.left_value : 0.0);
                            }
                        }
                    }
                    syncConfig();
                }));
        Input input = new Input(new Input.Data()
                .label("Input")
                .info("Horizontal input")
                .type(InputType.MOLANG)
                .value(data != null ? data.input : "v.particle_age")
                // JS 无 onchange；此处仅为 config 即时同步（见类文档偏离说明）
                .onchange(event -> syncConfig()));
        Input range = new Input(new Input.Data()
                .label("Range")
                .info("Horizontal range that the input is mapped to")
                .type(InputType.MOLANG)
                .value(data != null ? data.range : "v.particle_lifetime")
                // JS: condition(curve) { return curve.inputs.mode.value !== 'bezier_chain' }
                // （isVisible 传入的 group 即本 Curve，闭包引用等价）
                .condition(group -> !"bezier_chain".equals(in("mode").getValue()))
                .onchange(event -> syncConfig()));

        inputs.put("id", id);
        inputs.put("mode", mode);
        inputs.put("input", input);
        inputs.put("range", range);

        syncConfig();

        // JS: setTimeout(() => Vue.nextTick(() => this.updateSVG()), 20) → 无调度器，直接置位
        updateSVG();
    }

    /** JS updateSVG()。 */
    public void updateSVG() {
        this.svg_needs_update = true;
    }

    /** JS updateMinMax()：按节点值自适应 min/max（基准 0/1，clamp 到 [-256,0]/[0,256]）。 */
    public void updateMinMax() {
        this.min = 0;
        this.max = 1;
        for (Object v : nodes) {
            this.min = Math.min(this.min, v instanceof Config.BezierNode node
                    ? Math.min(node.right_value, node.left_value) : ((Number) v).doubleValue());
            this.max = Math.max(this.max, v instanceof Config.BezierNode node
                    ? Math.max(node.right_value, node.left_value) : ((Number) v).doubleValue());
        }
        // JS: Math.clamp(Math.round(x*100)/100, ...)（Math.clamp：NaN→min）
        this.min = SnowstormUtil.clamp(JsSemantics.jsRound(this.min * 100) / 100, -256, 0);
        this.max = SnowstormUtil.clamp(JsSemantics.jsRound(this.max * 100) / 100, 0, 256);
        this.updateSVG();
    }

    /**
     * JS updateName(new_name)：名字唯一化（冲突时追加递增序号），写入 Config.curves，
     * 并清理不再被任何曲线引用的旧键。
     */
    public Curve updateName(String new_name) {
        String valid_name = new_name;
        int i = 2;
        Map<String, Config.Curve> curves = EditorRuntime.Config.curves;
        // JS: Config.curves[valid_name] && Config.curves[valid_name] !== this.config
        while (curves.containsKey(valid_name) && curves.get(valid_name) != this.config) {
            valid_name = new_name + i;
            i++;
        }
        curves.put(valid_name, this.config);
        List<Curve> curveList = dataCurves();
        // JS for-in + delete：先收集键再删除，语义等价
        for (String key : new ArrayList<>(curves.keySet())) {
            if (!key.equals(valid_name)
                    && curveList.stream().noneMatch(curve -> key.equals(JsSemantics.toJsString(curve.in("id").getValue())))) {
                curves.remove(key);
            }
        }
        return this;
    }

    /** JS removeNode(index, event)（event 未使用，略去）。 */
    public void removeNode(int index) {
        if (nodes.size() <= 2) return;
        Object node = nodes.get(index);
        nodes.remove(index);
        if ("bezier_chain".equals(in("mode").getValue())) {
            Config.BezierNode removed = (Config.BezierNode) node;
            List<Object> sorted = new ArrayList<>(nodes);
            sorted.sort((a, b) -> Double.compare(
                    Math.abs(((Config.BezierNode) a).time - removed.time),
                    Math.abs(((Config.BezierNode) b).time - removed.time)));
            Object nearest = sorted.get(0);
            this.selected_point = nodes.indexOf(nearest);
        } else {
            this.selected_point--;
        }
        this.updateSVG();
        EditHistory.registerEdit("remove curve node");
    }

    /** JS setNode(index, value)：parseFloat + 两位舍入；bezier_chain 同时写左右值。 */
    public void setNode(int index, Object value) {
        double v = JsSemantics.jsRound(JsonValues.jsParseFloat(value) * 100) / 100;
        if ("bezier_chain".equals(in("mode").getValue())) {
            Config.BezierNode node = (Config.BezierNode) nodes.get(index);
            node.left_value = node.right_value = v;
        } else {
            nodes.set(index, v);
        }
        this.updateMinMax();
    }

    /** JS remove()：从 Config.curves 与 Data 曲线列表中移除自身。 */
    public void remove() {
        EditorRuntime.Config.curves.remove(JsSemantics.toJsString(in("id").getValue()));
        dataCurves().remove(this);
        EditHistory.registerEdit("remove curve");
    }

    /** JS curves.js updateCurvesPanel()（Vue.nextTick 延迟去除）。 */
    public static void updateCurvesPanel() {
        if (curvesGroup()._folded) return;
        for (Curve curve : dataCurves()) {
            curve.svg_needs_update = true;
        }
    }

    /** JS Data.variables.curves.curves。 */
    private static List<Curve> dataCurves() {
        return curvesGroup().curves;
    }

    /** inputs 四键构造后恒在（JS 对象字面量语义），requireNonNull 收口（NullAway）。 */
    private Input in(String key) {
        return Objects.requireNonNull(inputs.get(key));
    }

    /** Data.variables.curves 组（InputStructure 初始化后恒在），requireNonNull 收口。 */
    private static InputStructure.Group curvesGroup() {
        return Objects.requireNonNull(Objects.requireNonNull(InputStructure.Data.get("variables")).group("curves"));
    }

    /** config 惰性 getter 的即时同步等价物（见类文档）。 */
    private void syncConfig() {
        config.mode = JsSemantics.toJsString(in("mode").getValue());
        config.input = JsSemantics.toJsString(in("input").getValue());
        config.range = JsSemantics.toJsString(in("range").getValue());
    }

    private static Config.BezierNode bezierNode(double time, double leftValue, double rightValue,
                                                double leftSlope, double rightSlope) {
        Config.BezierNode node = new Config.BezierNode();
        node.time = time;
        node.left_value = leftValue;
        node.right_value = rightValue;
        node.left_slope = leftSlope;
        node.right_slope = rightSlope;
        return node;
    }
}
