# Launch checklist

These steps are done in App Store Connect, the Play Console and Xcode. The
code is ready for them; the product IDs below are the ones the apps look for.

## Pricing plan

| | Price | Notes |
| --- | --- | --- |
| Download | Free | Up to 3 habits, the grid, backups |
| **Routine Pro** (one-time) | **£3.99 / $4.99** | Launch at £2.99 / $2.99 for the first few weeks, then raise it |
| Small / medium / large tip | £0.99 / £2.99 / £4.99 | Optional; unlocks nothing |

People who used the Capacitor version get Pro free (see "Early supporters").

## Both stores

- [ ] **Keep the IDs.** iOS bundle ID and Android application ID stay
      `wales.ryland.routine`, so the native apps ship as updates and can import
      the old app's data.
- [ ] **Version numbers.** Both apps are version 2.0, build/versionCode 2. If the
      Capacitor app ever shipped a build numbered 2 or higher, raise these
      (`CURRENT_PROJECT_VERSION` in Xcode, `versionCode` in
      `android/app/build.gradle.kts`) and set
      `PurchaseStore.lastCapacitorBuild` (iOS) to the last Capacitor build number.
- [ ] **Privacy.** Routine collects no data and has no server. Declare "Data not
      collected" (App Store) and "No data collected or shared" (Play Data safety).

## App Store Connect (iOS)

- [ ] Accept the **Paid Apps agreement** and fill in banking and tax.
- [ ] Join the **App Store Small Business Program** (15% commission instead of
      30% on your first $1M a year).
- [ ] Create the in-app purchases (Monetization → In-App Purchases):

      | Product ID | Type | Family Sharing |
      | --- | --- | --- |
      | `wales.ryland.routine.unlock` | Non-consumable, "Routine Pro" | **On** |
      | `wales.ryland.routine.tip.small` | Consumable | Off |
      | `wales.ryland.routine.tip.medium` | Consumable | Off |
      | `wales.ryland.routine.tip.large` | Consumable | Off |

      Each needs a display name, description and a review screenshot (use the
      paywall). Attach them to the 2.0 version when you submit it.
- [ ] For launch pricing, set Routine Pro to the lower price now and schedule a
      price change to £3.99 / $4.99.
- [ ] **Capabilities.** Open `ios/Routine.xcodeproj`, select each target's
      *Signing & Capabilities* tab and let Xcode register them (automatic
      signing does this on the first device build):
      - Routine: **App Groups** `group.wales.ryland.routine` and **iCloud → Key-value storage**
      - RoutineWidgetExtension: **App Groups** `group.wales.ryland.routine`
- [ ] **Test purchases** before App Store Connect is set up: the scheme already
      uses `ios/Routine.storekit`, so purchases work in the simulator from Xcode.
      Then test with a sandbox account in TestFlight.

### Early supporters (iOS)

Two checks unlock Pro for existing users, and either one is enough:

1. On first launch, the app finds the Capacitor app's saved data and imports it.
2. In production builds, the App Store says this Apple ID first downloaded a
   Capacitor build (`AppTransaction.originalAppVersion` ≤ `lastCapacitorBuild`).
   This also works after reinstalling and on other devices. Sandbox and
   TestFlight always report "1.0", so the check is skipped there.

## Play Console (Android)

- [ ] Set up a **payments profile** (Settings → Payments profile).
- [ ] Check the **15% service fee** applies to your first $1M a year (Settings →
      Developer account → Service fee).
- [ ] **Signing.** An update must be signed with the same app signing key as the
      Capacitor app. If that app used Play App Signing, upload with your upload
      key as usual.
- [ ] Upload a build to a testing track first: the Play Console only lets you
      create products once a build that uses Play Billing exists.
- [ ] Create the one-time products (Monetize → Products → In-app products), using
      the same IDs as iOS:
      `wales.ryland.routine.unlock`, `wales.ryland.routine.tip.small`,
      `wales.ryland.routine.tip.medium`, `wales.ryland.routine.tip.large`.
- [ ] Add your Google account under **License testing** so test purchases aren't
      charged.
- [ ] Data safety: no data collected. The app asks for the notifications
      permission only when someone turns on reminders.

### Early supporters (Android)

On first launch, the app finds the Capacitor app's saved data
(`CapacitorStorage` preferences) and imports it, and that unlocks Pro. Play has
no "first downloaded" record the app can check without a server, so someone
who uninstalled the old app before updating won't be recognised. If that
matters, a promo code from the Play Console can unlock it for them.

## After launch

- The apps ask for a rating on a day when everything gets done, once there have
  been three such days, and at most every 120 days. Nothing to configure.
- Reply to early reviews. Habit apps live and die by their first ratings.
