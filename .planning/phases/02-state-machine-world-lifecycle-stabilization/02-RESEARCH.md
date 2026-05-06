# Research: Phase 2 — State Machine + World Lifecycle + Stabilization

**Researched:** 2026-05-06
**Source:** Architecture docs, PITFALLS.md, iris-tutorial-mod reference, Phase 1 codebase analysis
**Confidence:** HIGH

## 1. State Machine Pattern

### Decision: Tick-driven enum state machine with single @EventBusSubscriber

**Evidence:** The iris-tutorial-mod `TutorialClientHandler` uses a flat set of `static boolean` flags driven by `ClientTickEvent.Pre`. Our design formalizes this into an explicit `enum`-based state machine.

**Key design decisions:**
1. **Single tick handler** — one `@SubscribeEvent` method on `TickEvent.ClientTickEvent` (Phase.START), not one per state. A `switch` statement is simple, debuggable, and avoids FML event bus overhead.
2. **Static fields** — the state machine is naturally a singleton (one client per JVM). Matches iris-tutorial-mod pattern exactly.
3. **ERROR state** — terminal state for failures; still performs cleanup and logging before halting.

**Phase 2 states (complete scope):**
```
INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE
```
States beyond STABILIZE (TEST_EXEC, SCREENSHOT, NEXT_TEST, REPORT, EXIT) are Phase 3+4 scope.

### State enum design

```java
public enum ClientSmokeState {
    INIT,           // Entry — check config.enabled
    IDLE,           // Terminal — framework disabled
    CONFIG_LOAD,    // Config already loaded in Phase 1 (fast transition)
    SCAN,           // Scanner already ran in Phase 1 (fast transition)
    WORLD_CREATE,   // Create/open the test world
    WORLD_WAIT,     // Wait for player spawn + world load
    STABILIZE,      // Wait configurable ticks for render stabilization
    // Phase 3+: TEST_EXEC, SCREENSHOT, NEXT_TEST, REPORT, EXIT
    ERROR           // Terminal — failure recovery
}
```

### Forge 1.20.1 Event API

| Event | Forge 1.20.1 |
|-------|-------------|
| Tick event | `net.minecraftforge.event.TickEvent.ClientTickEvent` with `TickEvent.Phase.START` |
| EventBusSubscriber | `net.minecraftforge.fml.common.Mod.EventBusSubscriber` — works with `value = Dist.CLIENT` |
| SubscribeEvent | `net.minecraftforge.eventbus.api.SubscribeEvent` |

**Critical:** `@EventBusSubscriber` must include `value = Dist.CLIENT` (Pitfall 13 — prevents server-side loading of client-only classes).

**IMPORTANT distinction from iris-tutorial-mod:** The reference uses NeoForge 1.21.1's `net.neoforged.fml.common.EventBusSubscriber`. Forge 1.20.1 uses `net.minecraftforge.fml.common.Mod.EventBusSubscriber` (note: nested inside `Mod` interface). Both are functionally equivalent.

## 2. World Creation — Forge 1.20.1 API

### Platform: Forge 1.20.1 via legacyForge 2.0.91

**Key difference from NeoForge 1.21.1:** No `WorldOpenFlows` API. World creation on Forge 1.20.1 must use `Minecraft` methods directly or go through `IntegratedServer`.

### Available APIs

**1. Creating a new world:**
Forge 1.20.1's `Minecraft` class provides:
- `Minecraft.getInstance().getLevelSource()` — returns `LevelStorageSource`
- `LevelStorageSource.levelExists(String)` — checks if world save exists
- `LevelStorageSource.createLevel(String, LevelSettings, WorldGenSettings, ...)` — creates new level data

OR:
- `Minecraft.createLevel(String, LevelSettings, WorldGenSettings, ...)` — may or may not exist in Forge 1.20.1

**2. Loading an existing world:**
- `Minecraft.loadLevel(String)` — loads by world name
- Or via `Minecraft.doLoadLevel(String)` (private)

**3. World configuration for creative superflat:**
```java
LevelSettings settings = new LevelSettings(
    worldName,
    GameType.CREATIVE,    // creative mode
    false,                // not hardcore
    Difficulty.NORMAL,    // normal difficulty
    true,                 // allow commands
    new GameRules(),
    WorldDataConfiguration.DEFAULT
);

// For superflat: WorldPresets.FLAT or FlatLevelGeneratorSettings
WorldOptions worldOptions = new WorldOptions(
    12345L,               // fixed seed for determinism
    true,                 // generate structures
    false                 // bonus chest off
);
```

### Implementation Strategy

The plan's action section will specify:
1. Check `mc.getLevelSource().levelExists(worldName)` 
2. If not exists: create the level using available Forge 1.20.1 APIs
3. Join the world using `mc.loadLevel(worldName)` or equivalent
4. World name: `"ClientSmokeTest"` (configurable)

**The executor must verify the exact API by reading Minecraft 1.20.1 source.** The plan gives the behavioral contract; the exact method call is discovered during implementation.

**Known risk (MEDIUM, from STATE.md):** "Forge 1.20.1 world creation API: Exact API surface differs from 1.21.1's WorldOpenFlows. Needs implementation-phase verification."

## 3. Multi-Stage Readiness Check

### Three stages of readiness:

| Stage | Check | State |
|-------|-------|-------|
| 1. World created | `mc.getLevelSource().levelExists(worldName)` | WORLD_CREATE → WORLD_WAIT |
| 2. Player spawned | `mc.player != null && mc.level != null` | WORLD_WAIT → STABILIZE |
| 3. Render stable | `mc.level.getGameTime() - stabilizeStartTick >= configTicks` | STABILIZE → (Phase 3) |

### Stabilization timer pattern (from iris-tutorial-mod):
```java
private static long stabilizeStartTick = -1L;

// In WORLD_WAIT → STABILIZE transition:
if (mc.player != null && mc.level != null) {
    state = ClientSmokeState.STABILIZE;
    stabilizeStartTick = mc.level.getGameTime();
}

// In STABILIZE:
long waitedTicks = mc.level.getGameTime() - stabilizeStartTick;
if (waitedTicks >= ClientSmokeConfig.RELOAD_STABILIZE_TICKS.get()) {
    // Ready for Phase 3
}
```

**Default stabilization:** 40 ticks (2 seconds at 20 TPS), matching iris-tutorial-mod's `RELOAD_STABILIZE_TICKS`.

## 4. Integration Points

### Phase 1 interfaces consumed by Phase 2:
- `ClientSmokeConfig.ENABLED` — gate for state machine activation
- `ClientSmokeConfig.RELOAD_STABILIZE_TICKS` — stabilization duration
- `ClientSmokeScanner.DiscoveredTest` — (stored for Phase 3/4 use)
- `ClientSmokeMod.MOD_ID = "clientsmoke"` — for @EventBusSubscriber modid

### Wiring into ClientSmokeMod:
The Phase 1 constructor has this placeholder:
```java
// Phase 2: EventBusSubscriber registration point
//   bus.register(ClientSmokeStateMachine.class);
```

The @EventBusSubscriber annotation on ClientSmokeStateMachine handles auto-registration — no manual `bus.register()` needed. However, we should verify the mod event bus receives the subscriber.

## 5. PITFALLS.md — Phase 2 Relevant Risks

| Pitfall | Risk | Mitigation |
|---------|------|------------|
| Pitfall 2 (Wrong tick phase) | Using wrong event phase causes stale world state | Use `TickEvent.ClientTickEvent` with `Phase.START` for state transitions; Phase 3 will handle render-thread capture separately |
| Pitfall 7 (Mixin scope) | Mixins conflicting with target mods | Phase 2 uses ZERO mixins — pure event-driven design |
| Pitfall 8 (World save/load race) | Checking `mc.level != null` too early | Multi-stage readiness check: level exists → player spawned → ticks stabilized |
| Pitfall 13 (Side guard) | Client-only classes crash on dedicated server | `@EventBusSubscriber(value = Dist.CLIENT)`; all methods check `FMLEnvironment.dist` |

## 6. File Structure

### New files:
```
eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/
├── ClientSmokeState.java           — State enum
└── ClientSmokeStateMachine.java    — @EventBusSubscriber, tick handler, state transitions
```

### Modified files:
```
eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/
└── ClientSmokeMod.java             — Store DiscoveredTest list, log state machine ready
```

## 7. Open Questions

1. **World name configurability:** Should the world name be a config entry or hardcoded? Decision: Hardcode `"ClientSmokeTest"` for v1 (config can be added later if needed).

2. **What if world already exists?** Decision: Reuse the existing world (per iris-tutorial-mod pattern). This saves time on subsequent runs.

3. **What happens at STABILIZE completion?** Phase 2 ends here. The state machine should log "Ready for test execution" and remain in STABILIZE state. Phase 3 picks up from STABILIZE → TEST_EXEC.

4. **Scanner results storage:** Phase 1's scanner runs in the constructor but the results aren't stored. Phase 2 needs to store the `List<DiscoveredTest>` so Phase 3/4 can use it. Decision: Store in a static field in `ClientSmokeStateMachine` (or `ClientSmokeMod`).

## Sources

| Source | Topic | Confidence |
|--------|-------|------------|
| iris-tutorial-mod `TutorialClientHandler.java` | State machine pattern, world creation, stabilization | HIGH (direct reference code) |
| eyelib `ClientSmokeMod.java` (Phase 1 output) | @Mod entrypoint, config, scanner wiring | HIGH (existing code) |
| eyelib `ClientSmokeConfig.java` (Phase 1 output) | Config entries available to state machine | HIGH (existing code) |
| eyelib `ClientSmokeScanner.java` (Phase 1 output) | DiscoveredTest record, scanner API | HIGH (existing code) |
| ARCHITECTURE.md | State machine design, state transitions | HIGH (planning doc) |
| PITFALLS.md | World creation risks, tick phase risks, side guards | HIGH (research doc) |
