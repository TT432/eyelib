package io.github.tt432.eyelibmolang;

import org.jspecify.annotations.NullMarked;

/**
 * 有序键值对，用于保留 Bedrock objectArray（如 RC 的 materials / part_visibility）中的条目顺序和重复键。
 *
 * @author TT432
 */
@NullMarked
public record MolangMapEntry(String key, MolangValue value) {
}
