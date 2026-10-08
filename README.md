# Routine

A habit tracker: list the small things you want to do every day, then tick them
off. Each day gets a score, and a gold star when everything's done.

This repo holds two fully native apps, plus the original web app they replace:

| Folder | What | Built with |
| --- | --- | --- |
| [`ios/`](ios) | iPhone and iPad app | Swift, SwiftUI (iOS 17+) |
| [`android/`](android) | Android app | Kotlin, Jetpack Compose, Material 3 (Android 8+) |
| [`fixtures/`](fixtures) | Test data both apps are checked against | JSON |
| `src/`, `src-capacitor/` | The original Vue/Quasar/Capacitor app | Vue 3 |

## iOS

Open `ios/Routine.xcodeproj` in Xcode 16 or later and run the **Routine** scheme.
The signing team and bundle ID (`wales.ryland.routine`) are already set.

```bash
cd ios && swift test     # model, storage and migration tests (runs on macOS or Linux)
```

- `Routine/Core/` has the platform-independent logic: the model, saving, and
  importing data from the old app. It's plain Foundation, which is why the tests
  can also run on Linux.
- `Routine/Views/` holds the SwiftUI screens, and `RoutineModel.swift` connects
  them to the core.

## Android

Open `android/` in Android Studio and run the **app** configuration.

```bash
cd android
./gradlew :core:test          # model, storage and migration tests (plain JVM, no emulator)
./gradlew :app:assembleDebug  # build the app
```

- `core/` is a plain Kotlin module with the same model, rules and file format as
  the iOS core.
- `app/` holds the Compose UI (`ui/`), plus `RoutineViewModel` and `MainActivity`.

Library versions are pinned to a known-good mid-2025 set (AGP 8.12, Kotlin 2.2.20,
Compose BOM 2025.08.00). Android Studio's upgrade assistant can bump them.

## Upgrading from the Capacitor app

Both apps keep the old app's identifiers, so they install as **updates** to it
(iOS bundle ID and Android application ID `wales.ryland.routine`; build number 2,
version 2.0). On first launch, each app imports what the old app saved:

- iOS reads `UserDefaults` key `CapacitorStorage.routine`.
- Android reads SharedPreferences `CapacitorStorage`, key `routine`.

The old data is never modified or deleted. After the import, the routine is saved as
`routine.json`: in Application Support on iOS, and in the app's files directory on
Android, where Auto Backup covers it. If that file can't be read (for example, if it
was written by a newer version), it's moved aside rather than overwritten.

## How it behaves

- **Habits** have a daily goal. A goal of *once* gives a tick box. A goal of
  *2–20 times* gives a counter that fills a ring as you tap it, then wraps back to
  zero, so you can always undo a mistaken tap.
- **Daily score** is the share of habits done that day. It's coloured by band and
  shows a star at 100%.
- **History is stable.** A habit you add today doesn't lower the scores of days
  before it existed. Changing a goal never changes which past days count as done.
- The grid opens on today, and a new column appears at midnight.
- **Edit habits** lets you rename, reorder (drag), change a goal, or delete.
  Deleting asks for confirmation.

### Fixed from the web app

- Changing a habit between tick box and counter no longer wipes or invents history.
  Before, switching to a counter reset past days to 0, and switching back marked
  0-count days as done.
- Doing more than a lowered target now counts as done. Before, only an exact match
  counted.
- Clearing the target field could leave a habit stuck as a broken tick box. The
  goal is now a picker, so it can't be blank.
- The score rounds down, so the star only appears when everything really is done.
- Habits can be reordered. The web app showed a drag handle that did nothing.
- Deleting a habit asks first.
- Rows are keyed by a stable ID, not by the object itself (which Vue warns about).
- Dates no longer depend on the browser parsing `"23 Aug 2024"` strings. Days are
  stored as ISO dates (`2024-08-23`) and calculated without time-zone drift.
- A new day now appears straight away at midnight, or when you return to the app,
  instead of up to an hour later.
- There's a real app icon. The iOS project still had Capacitor's placeholder.

## Web app (original)

```bash
npm install
npx quasar dev
```
