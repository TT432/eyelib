# Eyelib Architecture Blueprint

## Module Relationships

```mermaid
graph TB
    subgraph subprojects["Gradle Subprojects"]
        molang["eyelib-molang<br/>java-library<br/>Molang engine / compiler / type"]
        importer["eyelib-importer<br/>legacyForge<br/>Schema / CODEC / parse"]
        processor["eyelib-preprocessing<br/>legacyForge<br/>Processing / batching / parsing helpers"]
        utilmod["eyelib-util<br/>legacyForge<br/>Shared utility leaf"]
        attachment["eyelib-attachment<br/>legacyForge<br/>Attachment contracts"]
        material["eyelib-material<br/>legacyForge<br/>Material definitions"]
    end

    subgraph root["Root : (Forge mod)"]
        subgraph client["client/"]
            loader["loader<br/>resource reload"]
            registry["registry<br/>publish boundary"]
            manager["manager<br/>singleton stores"]
            anim["animation<br/>clip + controller runtime"]
            model["model<br/>bake adapters"]
            render["render<br/>visitors / pipeline"]
            particle["particle<br/>emitters / components"]
            entity["entity<br/>client entity runtime"]
        end
        subgraph infra["infra"]
            network["network/<br/>packet route + register"]
            capability["capability/<br/>component / RenderData"]
            mc_impl["mc/impl/<br/>MC/Forge quarantine"]
        end
        common["common/<br/>shared behavior"]
    end

    %% Gradle deps
    root -- "impl" --> molang
    root -- "impl" --> importer
    root -- "impl" --> processor
    root -- "impl/api" --> utilmod
    root -- "impl" --> attachment
    root -- "impl" --> material
    attachment -- "impl" --> utilmod
    material -- "impl" --> utilmod
    importer -- "impl" --> molang
    processor -- "impl" --> molang

    %% Data flow: importer schema → processor IR → root runtime
    loader -- "parse JSON" --> importer
    loader -- "bake()" --> processor
    anim -- "fromSchema() → Baker" --> processor
    model -- "importFile()" --> importer

    %% Write lane
    loader --> registry
    registry --> manager

    %% Read lane (lookup seams)
    anim -.-> manager
    model -.-> manager
    render -.-> manager
    entity -.-> manager
    particle -.-> manager

    %% Runtime
    anim -- "MolangScope" --> molang
    particle -- "MolangScope" --> molang
    render --> model
    entity --> model
    entity --> anim

    %% Sync lane
    network --> mc_impl
    network --> client

    %% Capability
    capability --> render
    capability --> mc_impl
```

## Communication Lanes
- **Write lane**: loaders parse → publish via registry → managers store
- **Read lane**: runtime modules query `*Lookup` facades, not `Eyelib.getXManager()` reach-through
- **Sync lane**: packets route to domain apply services (`ClientRenderSyncService`, `ParticleSpawnService`)
- **Notification lane**: `MinecraftForge.EVENT_BUS` for coarse invalidation only

## Target Roles
- `eyelib-molang`: Molang engine / compiler / type system (no MC/Forge deps)
- `eyelib-importer`: schema definitions / CODECs / raw JSON parsing (no runtime execution)
- `eyelib-preprocessing`: Forge-side processing / batching / parsing helpers (no root runtime deps)
- `eyelib-util`: shared Forge-aware utility leaf module with no project-internal dependencies
- `eyelib-attachment`: typed attachment contracts and attachment packet contracts; consumes `eyelib-util` stream codecs
- `eyelib-material`: Bedrock material definitions; consumes `eyelib-util` codec infrastructure
- `bootstrap`: composition root only
- `client/loader`: parse-only resource loading pipeline
- `client/registry`: publication boundary from parsed data into runtime stores
- `client/manager`: observable runtime stores
- `client/* runtime`: lookup- and service-driven readers
- `network/*`: routing and packet registration only
- `network/dataattach`: sync service seam between packets and local attachment state

## Execution Priorities
1. Normalize all asset publication through `client/registry`
2. Move packet application logic into domain services; keep `NetClientHandlers` shallow
3. Introduce lookup facades for core runtime reads
4. Extract pure-data conversions to `eyelib-preprocessing` (processing pattern)
5. Migrate `eyelib-importer` to pure `java-library` after `StringRepresentable`/`ExtraCodecs` cleanup

## Rules
- New cross-module writes must not go directly from UI or loader into manager singletons
- New cross-module reads should prefer lookup facades inside the target domain
- New packet handlers should route immediately into a domain apply service
- Once a legacy read/write path is replaced, delete the old path in the same stage
- `eyelib-util` must remain a dependency leaf: consumers may depend on it, but it must not depend on root or sibling Gradle projects
