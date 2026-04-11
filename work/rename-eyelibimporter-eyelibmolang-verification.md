# Rename Verification Record

## Scope
- Renamed importer Java package root from `io.github.tt432.eyelib.importer.*` to `io.github.tt432.eyelibimporter.*`
- Renamed importer mod id from `eyelib_resources_importer` to `eyelibimporter`
- Renamed importer Gradle subproject identity from historical `:resources-importer` to physical subproject `:eyelib-importer`
- Renamed Molang Java package root from `io.github.tt432.eyelib.molang.*` to `io.github.tt432.eyelibmolang.*`
- Renamed Molang mod id from `eyelib_molang` to `eyelibmolang`

## Verification Performed
- Compile: `./gradlew :eyelib-molang:compileJava :eyelib-importer:compileJava :compileJava`
  - Result: success
- Test: `./gradlew test`
  - Result: success
- Smoke: `./gradlew runClient`
  - Observed state: client process remained running for 35 seconds
  - Crash criterion: no new files appeared under `run/crash-reports/`
  - Result: pass

## Notes
- Submodule resource metadata warnings were resolved by adding `pack.mcmeta` to both subprojects.
- Remaining non-blocking runtime warnings still include Molang H2 cache driver warnings and existing codec/json warnings, but they do not produce crash reports or early startup failure.
