package io.github.tt432.eyelib.snowstorm.editor;

import java.util.List;

/**
 * Snowstorm v3.2.2 {@code molang_data.js} 部分移植（原作者 JannisX11，GPL-3.0）：
 * 仅 {@code DefaultVariables} / {@code DefaultContext} 两个列表（variable_placeholders.js 依赖）。
 *
 * <p>RootTokens / MolangQueries / MolangQueryLabels / MathFunctions / MathFunctionLabels
 * 服务于 molang_autocomplete.js / path_autocomplete.js，归 P3 UI 阶段移植（ADR-0036 §5）。
 */
public final class MolangData {

    private MolangData() {
    }

    /** JS {@code DefaultContext}（as-is 顺序）。 */
    public static final List<String> DefaultContext = List.of(
            "item_slot",
            "block_face",
            "cardinal_block_face_placed_on",
            "is_first_person",
            "owning_entity",
            "player_offhand_arm_height",
            "other",
            "count"
    );

    /** JS {@code DefaultVariables}（as-is 顺序）。 */
    public static final List<String> DefaultVariables = List.of(
            "emitter_lifetime",
            "emitter_age",
            "emitter_random_1",
            "emitter_random_2",
            "emitter_random_3",
            "emitter_random_4",
            "particle_lifetime",
            "particle_age",
            "particle_random_1",
            "particle_random_2",
            "particle_random_3",
            "particle_random_4",
            "entity_scale"
    );
}
