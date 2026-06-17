package io.github.tt432.eyelibimporter.addon;

import com.mojang.serialization.Codec;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public enum BedrockAddonWarningCode {
    UNMANAGED_RESOURCE,
    SCHEMA_PARSE_FAILED,
    DUPLICATE_OVERRIDE,
    MANIFEST_FIELD_UNMANAGED,
    DEPENDENCY_NOT_RESOLVED;

    public static final Codec<BedrockAddonWarningCode> CODEC = Codec.STRING.xmap(
            BedrockAddonWarningCode::valueOf,
            BedrockAddonWarningCode::name
    );
}
