# Routine

A habit tracker: list the small things you want to do every day, then tick them
off. Each day gets a score, and a gold star when everything's done.

Routine is two fully native apps that share one data format and the same rules:

| Folder | What | Built with |
| --- | --- | --- |
| [`ios/`](ios) | iPhone and iPad app, plus Home and Lock Screen widgets | Swift, SwiftUI, WidgetKit, StoreKit 2 (iOS 17+) |
| [`android/`](android) | Android app, plus a home-screen widget | Kotlin, Jetpack Compose, Material 3, Glance, Play Billing 8 (Android 8+) |
| [`fixtures/`](fixtures) | JSON both apps' tests are checked against | |
| [`docs/launch-checklist.md`](docs/launch-checklist.md) | Store setup: products, pricing, capabilities | |

The original Vue/Capacitor app was replaced by these apps. It's still in the git
history (commit `ddf397e`).

## Free and Pro

Routine is free to download. A single in-app purchase unlocks **Routine Pro**
for good: no subscription, and it's shared with your family on iOS.

| Free | Routine Pro (one-time) |
| --- | --- |
| Up to 3 habits | Unlimited habits |
| The daily grid, scores and stars | Home screen widgets you can tick from (and Lock Screen on iOS) |
| Back up to a file and restore | Daily reminders that skip finished days |
| | iCloud sync between iPhone and iPad |
| | Streaks, a monthly calendar and success rates |
| | Colour themes: Ocean, Forest, Amber, Violet |
| | CSV export |

There's also an optional tip jar. Anyone who used the Capacitor version gets Pro
free (see the [launch checklist](docs/launch-checklist.md#early-supporters-ios)).

## iOS

Open `ios/Routine.xcodeproj` in Xcode 16 or later and run the **Routine** scheme.
Purchases work in the simulator straight away: the scheme uses
`ios/Routine.storekit`.

```bash
cd ios && swift test     # model, storage, stats and migration tests (macOS or Linux)
```

Run the UI tests (⌘U, or `xcodebuild test`) to exercise the app in the simulator.

| Folder | What |
| --- | --- |
| `Shared/Core/` | Platform-independent logic: the model, saving, stats, CSV, sync rules, legacy import. Plain Foundation, so the tests also run on Linux. |
| `Shared/` | Shared by the app and the widget: App Group storage and colour themes |
| `Routine/` | The app: SwiftUI screens, purchases, reminders, iCloud sync |
| `RoutineWidget/` | The widget extension and its tick-off intent |
| `RoutineUITests/` | UI tests that drive the app and take screenshots |

## Android

Open `android/` in Android Studio and run the **app** configuration.

```bash
cd android
./gradlew :core:test            # model, storage, stats and migration tests (plain JVM)
./gradlew :app:assembleDebug    # build the app
./gradlew :app:connectedCheck   # UI tests on a connected device or emulator
```

| Folder | What |
| --- | --- |
| `core/` | Plain Kotlin module with the same model, rules and file format as the iOS core |
| `app/…/ui/` | Compose screens |
| `app/…/billing/`, `reminders/`, `widget/`, `data/` | Play Billing, alarms, the Glance widget, settings and storage |
| `app/src/androidTest/` | UI tests that drive the app and take screenshots |

Library versions are pinned to a known-good set (AGP 8.12, Kotlin 2.2.20, Compose
BOM 2025.08.00, Play Billing 8.0). Android Studio's upgrade assistant can bump them.

## CI

[`.github/workflows/native-apps.yml`](.github/workflows/native-apps.yml) runs on
every push that touches the apps. It:

- runs both cores' tests;
- builds the iOS app and widget, and runs the iOS UI tests in the simulator;
- builds and lints the Android app, and runs its UI tests on an emulator.

Screenshots from both sets of UI tests are uploaded as build artifacts.

## How it behaves

- **Habits** have a daily goal. A goal of *once* gives a tick box. A goal of
  *2–20 times* gives a counter that fills a ring as you tap it, then wraps back to
  zero, so you can always undo a mistaken tap.
- **Daily score** is the share of habits done that day. It's coloured by band and
  shows a star at 100%.
- **History is stable.** A habit you add today doesn't lower the scores of days
  before it existed. Changing a goal never changes which past days count as done.
- **Streaks** don't break until a day is over: today counts once it's done.
- The grid opens on today, and a new column appears at midnight.
- **Moving phones:** a backup file works across iOS and Android. On Android,
  Google's Auto Backup also restores the routine with the rest of the phone.

## Upgrading from the Capacitor app

Both apps keep the old app's identifiers (`wales.ryland.routine`), so they
install as updates (version 2.0, build 2). On first launch, each app imports what
the old app saved:

- iOS reads `UserDefaults` key `CapacitorStorage.routine`.
- Android reads SharedPreferences `CapacitorStorage`, key `routine`.

The old data is never modified or deleted. If the new save file can't be read
(for example, if a newer version wrote it), it's moved aside rather than
overwritten.
