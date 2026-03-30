# Client Registry Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/registry/`
- Runtime publication and lookup-facing boundary for parsed client assets.

## Current Role
- `ClientAssetRegistry.java` is the first shared publication seam between loader parsing and manager-backed runtime storage.
- Loaders and tooling should parse and validate data, then hand publication off here instead of pushing directly into managers or loader-owned stores.
- Client-entity publication also enters the runtime through this seam before landing in `ClientEntityManager`.

## Read Only If Needed
- Start here when a task is about manager publication, runtime lookup, or reducing direct manager writes from loaders.
