package io.github.tt432.eyelib.bridge.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.model.Model;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型几何遍历的 Port 持有者：ACL 声明接口，application（client）在启动期安装实现。
 *
 * <p>存在的理由：bridge 的方块几何烘焙需要遍历 domain 的 {@link Model}，而遍历与姿势栈
 * 语义由 application 层的 {@code ModelVisitor} 拥有。直接 import 会形成 ACL → Application
 * 反向依赖（ADR-0016 §2），故用 Port 反转。
 *
 * @author TT432
 */
public interface ModelBakeTraversal {
    AtomicReference<@Nullable ModelBakeTraversal> HOLDER = new AtomicReference<>();

    void traverse(Model model, PoseStack poseStack, ModelBakeSink sink);

    static void install(ModelBakeTraversal traversal) {
        HOLDER.set(traversal);
    }

    static ModelBakeTraversal get() {
        ModelBakeTraversal traversal = HOLDER.get();
        if (traversal == null) {
            throw new IllegalStateException("ModelBakeTraversal not installed");
        }
        return traversal;
    }
}
