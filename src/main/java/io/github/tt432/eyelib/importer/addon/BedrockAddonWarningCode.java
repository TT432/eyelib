package io.github.tt432.eyelib.importer.addon;

import com.mojang.serialization.Codec;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public enum BedrockAddonWarningCode {
    UNMANAGED_RESOURCE,
    SCHEMA_PARSE_FAILED,
    DUPLICATE_OVERRIDE,
    MANIFEST_FIELD_UNMANAGED,
    DEPENDENCY_NOT_RESOLVED,
    /** 用户选择的 subpack 不在 manifest.subpacks 中（回落自动选择）。 */
    SUBPACK_OVERRIDE_UNKNOWN;

    public static final Codec<BedrockAddonWarningCode> CODEC = Codec.STRING.xmap(
            BedrockAddonWarningCode::valueOf,
            BedrockAddonWarningCode::name
    );
}
