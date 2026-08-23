package io.github.tt432.eyelib.bridge.client.model;

//? if <26.1 {
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;
//? if <1.20.6 {
import net.minecraftforge.client.model.geometry.IGeometryLoader;
//?} else {
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
//?}

/**
 * Bedrock 方块/物品几何 loader：让 vanilla 模型 JSON 可以把
 * {@code assets/<ns>/eyelib/models/*.geo.json} 烘焙成 BakedModel（进区块网格），
 * 无需 BlockEntity/BER。
 *
 * <p>用法（模型 JSON）：</p>
 * <pre>
 * {
 *   "loader": "eyelib:bedrock",
 *   "model": "warframe:mech_bay",
 *   "textures": { "texture": "warframe:block/mech_bay", "particle": "warframe:block/mech_bay" }
 * }
 * </pre>
 * {@code model} 为 geo.json 的资源 id（省略 {@code eyelib/models/} 前缀与 {@code .geo.json} 后缀）。
 *
 * <p>26.1 渲染管线重写后未适配，该版本不注册此 loader。</p>
 *
 * @author TT432
 */
public class BrBlockGeometryLoader implements IGeometryLoader<BrBlockUnbakedGeometry> {
    @Override
    public BrBlockUnbakedGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext)
            throws JsonParseException {
        return new BrBlockUnbakedGeometry(GsonHelper.getAsString(jsonObject, "model"));
    }
}
//?}
