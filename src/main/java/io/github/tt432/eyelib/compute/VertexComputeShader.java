package io.github.tt432.eyelib.compute;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * @author TT432
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class VertexComputeShader {
    @Getter
    private static ComputeShader shader;

    @SubscribeEvent
    public static void onEvent(FMLClientSetupEvent event) {
        RenderSystem.recordRenderCall(() -> {
            shader = ComputeShader.of("""
                    #version 460 core
                    
                    struct Vertex {
                        float x;
                        float y;
                        float z;
                        int color;
                        float u0;
                        float v0;
                        int uv1;
                        int uv2;
                        uint normal;
                    };
                    
                    struct Extra {
                        int customColor;
                        int customOverlay;
                        int customLight;
                    };
                    
                    layout(local_size_x=1, local_size_y=1) in;
                    
                    layout(binding=0, std430) buffer Data {
                        Vertex vertices[];
                    } data;
                    layout(binding=1, std430) readonly buffer Transforms {
                        mat4 matrices[];
                    } transforms;
                    layout(binding=2, std430) readonly buffer Normals {
                        mat3 matrices[];
                    } normals;
                    layout(binding=3, std430) readonly buffer Indices {
                        int indices[];
                    } indices;
                    layout(binding=4, std430) readonly buffer ExtraData {
                        Extra values[];
                    } extraData;
                    
                    void main() {
                        int index = int(gl_GlobalInvocationID.x);
                        int transformIndex = indices.indices[index];
                        if (transformIndex < 0) {
                            return;
                        }
                        Vertex vertex = data.vertices[index];
                        data.vertices[index].color = extraData.values[transformIndex].customColor;
                        data.vertices[index].uv1 = extraData.values[transformIndex].customOverlay;
                        data.vertices[index].uv2 = extraData.values[transformIndex].customLight;
                        vec3 pos = vec3(vertex.x, vertex.y, vertex.z);
                        int normalZ = int((vertex.normal >> 16) & 0xFFu);
                        int normalY = int((vertex.normal >> 8) & 0xFFu);
                        int normalX = int((vertex.normal >> 0) & 0xFFu);
                        if (normalX > 127) normalX -= 256;
                        if (normalY > 127) normalY -= 256;
                        if (normalZ > 127) normalZ -= 256;
                        vec3 normal = vec3(normalX / 127.0, normalY / 127.0, normalZ / 127.0);
                        vec4 transformed = transforms.matrices[transformIndex] * vec4(pos, 1.0);
                        vec3 transformedNormal = normals.matrices[transformIndex] * normal;
                        data.vertices[index].x = transformed.x;
                        data.vertices[index].y = transformed.y;
                        data.vertices[index].z = transformed.z;
                        uint transformedNormalX = uint(int(clamp(transformedNormal.x, -1.0, 1.0) * 127.0) & 0xFF);
                        uint transformedNormalY = uint(int(clamp(transformedNormal.y, -1.0, 1.0) * 127.0) & 0xFF);
                        uint transformedNormalZ = uint(int(clamp(transformedNormal.z, -1.0, 1.0) * 127.0) & 0xFF);
                        data.vertices[index].normal = (transformedNormalZ << 16) | (transformedNormalY << 8) | (transformedNormalX << 0);
                    }
                    """);
        });
    }
}
