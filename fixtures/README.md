# Shared test fixtures

Both native apps' unit tests read these files, so the iOS and Android apps
are held to the same on-disk format and the same legacy-import rules.

- `routine-v1.json` — the current save format (version 1).
- `legacy-capacitor.json` — what the original Vue/Capacitor app saved, including
  the odd values it could produce (a target typed as text or cleared, counts
  above the target, duplicate dates, junk entries).
