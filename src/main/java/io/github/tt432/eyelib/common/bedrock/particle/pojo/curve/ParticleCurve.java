package io.github.tt432.eyelib.common.bedrock.particle.pojo.curve;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import io.github.tt432.eyelib.util.EyelibLists;
import io.github.tt432.eyelib.util.molang.MolangValue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
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

    boolean nameValid() {
        return name != null && name.startsWith("variable.");
    }

    protected static class Serializer implements JsonDeserializer<ParticleCurve> {
        static final Type molangValueListType = TypeToken.getParameterized(List.class, MolangValue.class).getType();

        @Override
        public ParticleCurve deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ParticleCurve result = new ParticleCurve();
            JsonObject object = json.getAsJsonObject();

            result.name = context.deserialize(object.get("name"), String.class);
            if (!result.nameValid())
                throw new JsonParseException("name must start with 'variable.' : " + result.name);

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
                        nodes.get(i).setTimestamp(new Timestamp(i / ((double) (size - 1))));
                    }
                } else {
                    throw new JsonParseException("parse " + result.name + " error : nodes count MUST more than 1");
                }
            } else if (nodesJson.isJsonObject()) {
                nodesJson.getAsJsonObject().entrySet().forEach(entry -> {
                    Timestamp timestamp = Timestamp.valueOf(entry.getKey());
                    ParticleCurveNode node = context.deserialize(entry.getValue(), ParticleCurveNode.class);
                    node.setTimestamp(timestamp);
                    nodes.add(node);
                });
            }

            nodes.sort(Comparator.comparingDouble(node -> node.timestamp.getTick()));
            EyelibLists.link(nodes);
            result.nodes = nodes;

            result.input = context.deserialize(object.get("input"), MolangValue.class);
            result.horizontalRange = context.deserialize(object.get("horizontal_range"), MolangValue.class);

            return result;
        }
    }
}
