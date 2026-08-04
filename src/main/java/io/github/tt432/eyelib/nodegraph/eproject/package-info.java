/**
 * eproject 图项目容器：一个「项目」= 项目元信息 + N 个命名
 * {@link io.github.tt432.eyelib.nodegraph.GraphLibrary} 图文档库，
 * 以 OPC（Open Packaging Conventions）布局持久化。
 *
 * <p>两种等价物理形态（内部条目布局一致）：
 * <ul>
 *   <li>目录形态：解压展开的 OPC 目录（含 {@code project.json} 标识），便于版本管理与增量同步；</li>
 *   <li>单文件形态：{@code *.eproject}（zip 容器），便于分发。</li>
 * </ul>
 *
 * <p>纯 domain 模块：仅依赖 JDK + Gson + Mojang Codec，零 Minecraft import。
 */
@NullMarked
package io.github.tt432.eyelib.nodegraph.eproject;

import org.jspecify.annotations.NullMarked;
