package io.github.tt432.eyelib.common.bedrock.particle.pojo.curve;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.math.Constant;
import io.github.tt432.eyelib.molang.math.MolangVariable;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.util.math.Interpolates;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author DustW
 */
@JsonAdapter(ParticleCurve.Serializer.class)
public class ParticleCurve {
    String name;
    CurveType type;

    /**
     * nodes are the control nodes for the curve.
     * These are assumed to be equally spaced,
     * meaning that the first node is at input value 0 and the second node is at 0.25,
     * and so on and so forth.<p>
     * 节点是曲线的控制节点。这些节点被假定为等距的，也就是说，第一个节点在输入值0处，第二个节点在0.25处，以此类推。 <p>
     * This notation works only for linear, bezier, and catmull_rom.<p>
     * 这个符号只适用于线性、贝赛尔和catmull_rom。
     * <p>
     * nodes for bezier chain are the control nodes for bezier_chain.
     * The nodes will be sorted prior to parsing,
     * so if you declare nodes 0.3, 0.6, 0.5, they will be re-ordered to 0.3, 0.5, 0.6.<p>
     * bezier chain的节点是bezier_chain的控制节点。节点在解析前会被排序，所以如果你声明节点0.3, 0.6, 0.5，它们会被重新排序为0.3, 0.5, 0.6。
     */
    List<ParticleCurveNode> nodes;

    /**
     * input <float/Molang> This is the input value to use. For example,<p>
     * variable.particle_age/variable.particle_lifetime would result in an input from 0 to 1 over the lifetime of the particle,
     * while variable.particle_age would have input of how old the particle is in seconds.
     */
    MolangValue input;

    /**
     * horizontal range (default: 1.0) This is the range that the input is mapped onto between 0 and this value.
     * This field is deprecated and optional.<p>
     * This field is ignored for bezier_chain
     */
    @SerializedName("horizontal_range")
    MolangValue horizontalRange;

    public MolangVariable asVariable() {
        return switch (type) {
            case LINEAR -> new MolangVariable(name, s -> {
                double range = horizontalRange.evaluate(s);
                double inputValue = input.evaluate(s);

                int maxIndex = nodes.size() - 1;
                int inputIndex = (int) Math.floor(inputValue / range * maxIndex);

                if (inputIndex <= 0) {
                    return nodes.get(0).value.evaluate(s);
                } else if (inputIndex >= maxIndex) {
                    return nodes.get(maxIndex).value.evaluate(s);
                } else {
                    var curr = nodes.get(inputIndex);
                    var next = nodes.get(inputIndex + 1);
                    double currTime = range * curr.index / maxIndex;
                    double nextTime = range * next.index / maxIndex;

                    return Interpolates.linear(
                            new Interpolates.Node(currTime, curr.value.evaluate(s)),
                            new Interpolates.Node(nextTime, next.value.evaluate(s)),
                            inputValue
                    );
                }
            });
            case BEZIER -> new MolangVariable(name, s -> {
                double range = horizontalRange.evaluate(s);
                double inputValue = input.evaluate(s);

                int maxIndex = nodes.size() - 1;
                int inputIndex = (int) Math.floor(inputValue / range * maxIndex);

                if (inputIndex <= 0) {
                    return nodes.get(0).value.evaluate(s);
                } else if (inputIndex >= maxIndex) {
                    return nodes.get(maxIndex).value.evaluate(s);
                } else {
                    var curr = nodes.get(inputIndex);
                    var next = nodes.get(inputIndex + 1);
                    double currTime = range * curr.index / maxIndex;
                    double nextTime = range * next.index / maxIndex;

                    return Interpolates.bezier(
                            new Interpolates.Node(currTime, curr.value.evaluate(s)),
                            new Interpolates.Node(nextTime, next.value.evaluate(s)),
                            inputValue
                    );
                }
            });
            case CATMULL_ROM -> new MolangVariable(name, s -> {
                double range = horizontalRange.evaluate(s);
                double inputValue = input.evaluate(s);

                int maxIndex = nodes.size() - 1;
                int inputIndex = (int) Math.floor(inputValue / range * maxIndex);

                if (inputIndex <= 1) {
                    return nodes.get(maxIndex > 0 ? 1 : 0).value.evaluate(s);
                } else if (inputIndex >= maxIndex - 1) {
                    return nodes.get(Math.max(maxIndex - 1, 0)).value.evaluate(s);
                } else {
                    var curr = nodes.get(inputIndex);
                    var next = nodes.get(inputIndex + 1);
                    double currTime = range * curr.index / maxIndex;
                    double nextTime = range * next.index / maxIndex;

                    return Interpolates.catmullRom(
                            new Interpolates.Node(range * (inputIndex - 1) / maxIndex, nodes.get(inputIndex - 1).value.evaluate(s)),
                            new Interpolates.Node(currTime, curr.value.evaluate(s)),
                            new Interpolates.Node(nextTime, next.value.evaluate(s)),
                            new Interpolates.Node(range * (inputIndex + 2) / maxIndex, nodes.get(inputIndex + 2).value.evaluate(s)),
                            inputValue
                    );
                }
            });
            case BEZIER_CHAIN -> null;
        };
    }

    boolean nameValid() {
        return name != null && name.startsWith("variable.");
    }

    protected static class Serializer implements JsonDeserializer<ParticleCurve> {
        static final Type molangValueListType = TypeToken.getParameterized(List.class, MolangValue.class).getType();

        @Override
        public ParticleCurve deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ParticleCurve result = new ParticleCurve();
            JsonObject object = json.getAsJsonObject();

            result.type = context.deserialize(object.get("type"), CurveType.class);

            List<ParticleCurveNode> nodes = new ArrayList<>();
            JsonElement nodesJson = object.get("nodes");

            if (nodesJson.isJsonArray()) {
                List<MolangValue> values = context.deserialize(nodesJson, molangValueListType);
                nodes.addAll(values.stream().map(v -> {
                    ParticleCurveNode node = new ParticleCurveNode();
                    node.setValue(v);
                    return node;
                }).toList());

                int size = nodes.size();

                if (size > 1) {
                    for (int i = 0; i < size; i++) {
                        nodes.get(i).setIndex(i);
                    }
                } else {
                    throw new JsonParseException("parse " + result.name + " error : nodes count MUST more than 1");
                }
            } else if (nodesJson.isJsonObject()) {
                // TODO need impl
                //nodesJson.getAsJsonObject().entrySet().forEach(entry -> {
                //    ParticleCurveNode node = context.deserialize(entry.getValue(), ParticleCurveNode.class);
                //    node.setTimestamp(Double.parseDouble(entry.getKey()));
                //    nodes.add(node);
                //});
            }

            result.nodes = nodes;

            result.input = context.deserialize(object.get("input"), MolangValue.class);
            result.horizontalRange = JsonUtils.parseOrDefault(context, object, "horizontal_range",
                    MolangValue.class, new Constant(1));

            return result;
        }
    }
}
