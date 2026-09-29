/**
 * Snowstorm 基岩粒子编辑器数据层的 as-is Java 移植（ADR-0036）。
 *
 * <p>镜像 Snowstorm v3.2.2 数据层（input_structure/input/curves/gradient/events/texture_edit/
 * export/import/emitter 装配），驱动 {@code eyelib.wintersky} 运行时做编辑器预览与导入导出。
 * 零 MC 依赖（ArchUnit 约束，同 wintersky）；UI 在 {@code client/gui/snowstorm}（LDLib2）。
 */
@NullMarked
package io.github.tt432.eyelib.snowstorm;

import org.jspecify.annotations.NullMarked;
