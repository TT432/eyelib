# Data Attachment Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/util/data_attach/`
- Typed data-attachment containers, helper methods, providers, and event handlers.

## Start Reading Here
1. `docs/index/network.md`
2. `docs/architecture/02-side-boundaries.md`
3. `DataAttachmentHelper.java`

## Key Files
- `DataAttachmentHelper.java`: current helper that both mutates attachments and publishes sync packets
- `DataAttachmentEventHandlers.java`: event-driven attachment behavior
- `DataAttachmentContainer*.java`: attachment storage infrastructure

## Boundary Reminder
- Attachment state ownership belongs here, but packet routing belongs in `../../network/`.
- Any refactor in this area must keep client rendering/tooling dependencies out.
