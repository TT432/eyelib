/**
 * bridge 的 Molang 层：MolangBuiltInQuery（平台查询函数）、EntityPortAdapter、MinecraftMolangQueryRuntime。
 * ComponentStoreView / MolangEntityContextView 为 application 层提供适配器接口。
 * MolangContextPort 提供工厂方法。
 */
@NullMarked
package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.bridge.molang.adapter.EntityPortAdapter;
import io.github.tt432.eyelib.bridge.molang.adapter.MinecraftMolangQueryRuntime;
import org.jspecify.annotations.NullMarked;
