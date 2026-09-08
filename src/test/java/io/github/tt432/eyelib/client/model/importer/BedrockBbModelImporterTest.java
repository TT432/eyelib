package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.importer.model.importer.ModelImporter;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** bedrock 格式 .bbmodel 导入回归测试。
 * 覆盖：texture 缺省 uv_width 时回落到项目 resolution、面 texture 的 uuid 字符串引用、
 * box_uv cube 无每面 uv 时的 uv_offset 展开、group 的 reset/bedrock_binding 透传、
 * locator / null_object 元素导入。
 * @author TT432 */
class BedrockBbModelImporterTest {
    private static final float EPSILON = 1e-5F;

    @Test
    void importsBedrockFormatBbModel() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(fixturePath("blockbench/bedrock_minimal.bbmodel"));

        Model model = imported.get("geometry.bedrock_minimal");
        assertNotNull(model);
        assertEquals(1, model.allBones().size());
        // 动画约定契约：播放的剪辑是基岩 .animation.json（geo 空间），
        // 与模型导入路径无关，bbmodel 导入模型也必须要求 geo 补偿翻转
        assertTrue(model.flipAnimation(), "bbmodel 导入模型播放基岩剪辑需要 geo 空间补偿翻转");

        Model.Bone root = model.allBones().values().iterator().next();

        // bedrock bone 语义透传
        assertTrue(root.reset());
        assertEquals("q.item_slot", root.binding());

        // 3 个 cube（locator/null_object 不产生 cube）
        assertEquals(3, root.cubes().size());

        // 面 uv 按 resolution 64 归一化（texture 未写 uv_width）：[0,0,16,16] -> [0,0,0.25,0.25]
        assertTrue(hasFaceWithUvBounds(root.cubes(), 0F, 0F, 0.25F, 0.25F),
                "int 纹理引用的面应按 resolution 归一化");
        // uuid 字符串纹理引用解析到 textures[0]：[16,16,32,32] -> [0.25,0.25,0.5,0.5]
        assertTrue(hasFaceWithUvBounds(root.cubes(), 0.25F, 0.25F, 0.5F, 0.5F),
                "uuid 字符串纹理引用应解析成功");
        // box_uv cube 无每面 uv，从 uv_offset [8,8] 展开：north 矩形 [24,24,40,40] -> [0.375,0.375,0.625,0.625]
        assertTrue(hasFaceWithUvBounds(root.cubes(), 0.375F, 0.375F, 0.625F, 0.625F),
                "box_uv cube 应从 uv_offset 展开每面 UV");

        // locator / null_object 进入骨骼 locator 列表
        List<LocatorEntry> locators = root.locator().cubes();
        assertEquals(2, locators.size());

        LocatorEntry locator = locators.stream().filter(l -> l.name().equals("hand_locator")).findFirst().orElseThrow();
        assertEquals(0.25F, locator.offset().x(), EPSILON);
        assertEquals(0.125F, locator.offset().y(), EPSILON);
        assertEquals(0F, locator.offset().z(), EPSILON);
        assertEquals((float) Math.toRadians(45), locator.rotation().z(), EPSILON);
        assertTrue(locator.ignoreInheritedScale());
        assertTrue(!locator.isNullObject());

        LocatorEntry nullObject = locators.stream().filter(l -> l.name().equals("null_obj")).findFirst().orElseThrow();
        assertTrue(nullObject.isNullObject());
        assertEquals(0.5F, nullObject.offset().x(), EPSILON);
    }

    private static boolean hasFaceWithUvBounds(List<Model.Cube> cubes, float u0, float v0, float u1, float v1) {
        for (Model.Cube cube : cubes) {
            for (Model.Face face : cube.faces()) {
                float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE;
                float maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
                for (Model.Vertex vertex : face.vertexes()) {
                    minU = Math.min(minU, vertex.uv().x());
                    minV = Math.min(minV, vertex.uv().y());
                    maxU = Math.max(maxU, vertex.uv().x());
                    maxV = Math.max(maxV, vertex.uv().y());
                }
                if (approximatelyEquals(minU, u0) && approximatelyEquals(minV, v0)
                        && approximatelyEquals(maxU, u1) && approximatelyEquals(maxV, v1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean approximatelyEquals(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }

    private static Path fixturePath(String relativePath) {
        return Path.of("src", "test", "resources",
                "io", "github", "tt432", "eyelib", "importer", "model", "importer", relativePath);
    }
}
