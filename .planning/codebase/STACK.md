# Technology Stack

**Analysis Date:** 2026-05-06

## Languages

**Primary:**
- Java 17 - All source code across all modules; language version enforced via Gradle toolchain

**Secondary:**
- GLSL (`.vsh` / `.fsh`) - Minecraft-compatible shader programs in `src/main/resources/assets/eyelib/shaders/core/`
- JSON - Resource definitions, particle configs, material definitions, shader metadata
- TOML - Mod metadata (`mods.toml`) in each module
- ANTLR4 grammar (`.g4`) - Molang parser grammar in `eyelib-molang/` (generated sources)

## Runtime

**Environment:**
- Java 17 JDK (enforced by `java.toolchain.languageVersion = JavaLanguageVersion.of(17)` in all `build.gradle`)
- Target platform: Minecraft 1.20.1 + MinecraftForge 47.1.3 (FML loader version 47+)

**Package Manager:**
- Gradle 8.12.1 (`gradle/wrapper/gradle-wrapper.properties`)
- Groovy DSL for all build scripts
- Lockfile: Not present (no `gradle.lockfile`)

## Frameworks

**Core:**
- MinecraftForge 47.1.3 - Mod loader, event bus, network (SimpleChannel), rendering pipeline
- NeoForged ModDev Gradle (legacyforge) 2.0.91 - Forge development plugin providing run configs, remapping, IDE sync
- Parchment mappings (`2023.09.03` for `1.20.1`) - Human-readable Minecraft parameter names and Javadoc
- Mixin 0.8.5 (`org.spongepowered:mixin:0.8.5:processor`) - Bytecode injection for Minecraft integration
- MixinExtras 0.5.0 (`io.github.llamalad7:mixinextras-forge:0.5.0`) - Extended mixin utilities (jar-in-jar bundled)
- Mojang DataFixerUpper 6.0.8 (`com.mojang:datafixerupper`) - Codec/serialization system used for Bedrock resource parsing

**Testing:**
- JUnit Jupiter 5.10.2 - Primary test framework (`org.junit:junit-bom:5.10.2`)
- JUnit Platform Launcher - Test runtime support

**Build/Dev:**
- Project Lombok (`io.freefair.lombok` plugin 8.6) - Boilerplate reduction across all modules
- Error Prone 2.42.0 (`com.google.errorprone:error_prone_core`) - Compile-time static analysis
- NullAway 0.12.10 (`com.uber.nullaway:nullaway`) - Null-safety verification (custom `nullawayMain` task)
- ANTLR 4.9.1 runtime (`org.antlr:antlr4-runtime`) - Generated Molang parser runtime in `eyelib-molang`
- Signing plugin (`id 'signing'`) - Artifact signing for Maven Central publishing
- IDEA plugin - IDE integration (download sources/javadoc)
- RenderDoc (custom `runWithRenderDoc` task) - GPU frame capture integration

## Key Dependencies

**Critical:**
- `io.github.dmlloyd:jdk-classfile-backport:24.0` - Bytecode generation to JVM class files for Molang expression compilation (jar-in-jar bundled)
- `com.h2database:h2:2.4.240` - Embedded file-based SQL database for client-side performance instrumentation (jar-in-jar bundled)
- `com.mojang:datafixerupper:6.0.8` - Mojang's codec/serialization library, used by `eyelib-molang`, `eyelib-processor`, and `eyelib-attachment`

**Infrastructure:**
- `org.joml:joml:1.10.5` - Vector/matrix math library (`eyelib-attachment`, `eyelib-molang`)
- `org.slf4j:slf4j-api:2.0.7` - Logging facade (`eyelib-material`, `eyelib-attachment`, `eyelib-processor`, `eyelib-molang`); actual implementation from Minecraft/Forge runtime
- `maven.modrinth:acceleratedrendering:1.0.5-1.20.1-alpha` - Optional compileOnly compatibility with Accelerated Rendering mod

**Nullability & Quality:**
- `org.jspecify:jspecify:1.0.0` - `@Nullable` annotations (compileOnly, all modules)
- `com.google.errorprone:error_prone_core:2.42.0` - Error Prone static analysis
- `com.uber.nullaway:nullaway:0.12.10` - Null-safety checker via Error Prone

## Configuration

**Environment:**
- All configuration via `gradle.properties` - Minecraft/Forge versions, mod metadata, publishing settings
- No `.env` files detected; no runtime environment variable requirements for the mod itself
- Publishing credentials (`ossrhUsername`, `ossrhPassword`) sourced from Gradle properties

**Build:**
- `gradle.properties` - mod version, group, Minecraft/Forge versions, Parchment mappings
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.12.1 distribution
- `settings.gradle` - Project includes: root, `eyelib-attachment`, `eyelib-importer`, `eyelib-material`, `eyelib-molang`, `eyelib-processor`
- `src/main/templates/META-INF/mods.toml` - Mod metadata template expanded at build time
- `src/main/resources/eyelib.mixins.json` - Mixin configuration (3 client-only mixins)
- `src/main/resources/META-INF/accesstransformer.cfg` - 49 access widening entries for Minecraft internals

## Platform Requirements

**Development:**
- JDK 17
- Gradle 8.12.1 (via wrapper)
- IntelliJ IDEA (required; VS Code/Eclipse tooling explicitly prohibited per `AGENTS.md`)
- JetBrains MCP enabled for Gradle execution

**Production:**
- Minecraft 1.20.1
- MinecraftForge 47.1.3+ (FML 47+)
- Java 17 runtime
- Mod published to Maven Central at `io.github.tt432:eyelib`

---

*Stack analysis: 2026-05-06*
