# Attachable 渲染注入点选择

## 背景

Bedrock attachable 只在物品被装备（手持/穿戴）时激活，**不覆盖**掉落物或 GUI。
Java Edition 中这两者是不同渲染路径。

## 注入点对照

| Bedrock 路径 | MC Java 对应类 | 是否注入 |
|---|---|---|
| `controller.render.item_default`（手持） | `ItemInHandRenderer.renderItem()` | ✅ 需要 |
| `controller.render.armor`（穿戴盔甲/鞘翅） | `HumanoidModel.renderArmor()` | ❌ 未覆盖 |
| `minecraft:icon`（GUI/掉落） | `ItemRenderer.render()` for item entity | ❌ 不需要 |

## 常见错误

- **混入 `ItemRenderer.class`**（`net.minecraft.client.renderer.entity.ItemRenderer`）：该类渲染物品实体和 GUI，
  不处理手持物品。手持走的是 `net.minecraft.client.renderer.ItemInHandRenderer`。
- **混入 `ItemRenderer.renderStatic()`**：参数签名为 `(LivingEntity, ItemStack, ItemDisplayContext, ...)`
  但有多个 overload，类型不匹配会导致 mixin 不生效。

## 正确注入点

```java
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                              boolean left, PoseStack pose, MultiBufferSource buffer,
                              int light, CallbackInfo ci) {
        // 检查 attachable，渲染自定义模型，ci.cancel()
    }
}
```

## 注意

对 eyelib 实体的 `EntityRenderSystem.renderItemInHand()` 不会调用 `ItemInHandRenderer.renderItem()`
（attachable 分支走 early return 自渲染），因此不会和该混入冲突。
