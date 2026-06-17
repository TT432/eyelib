package io.github.tt432.eyelib.importer.addon;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public enum BedrockUnmanagedReason {
    NO_TYPED_SCHEMA_YET,
    OUTSIDE_IMPORTER_SCOPE,
    TEXTURE_SIDE_METADATA,
    UNKNOWN_LAYOUT,
    SCHEMA_PARSE_FAILED,
    MANIFEST_FIELD_NOT_MODELED
}
