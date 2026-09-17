# Take My Meds

A tiny personal Android app that solves one problem: reminding you to take your
tablets with a notification you have to actively dismiss (by tapping
**"Taken"**), instead of an alarm you can silence with one swipe.

## What it does

- Two daily reminder times ("Dose 1" and "Dose 2"), which you set and can
  change any time from inside the app.
- Each reminder is a notification with a **Taken** button. Tapping it
  dismisses the notification and marks that dose as done for the day.
- If you don't tap **Taken**, one follow-up reminder fires at a specific
  clock time you set for that dose (e.g. Dose 1 at 8:00 AM, follow-up at 8:30
  AM). Each dose has its own follow-up time, and you can turn a dose's
  follow-up off entirely with its checkbox.
- Every reminder automatically re-arms itself for the next day — nothing to
  reset manually.
- Survives phone restarts (reminders are rescheduled on boot).

It's deliberately simple: no accounts, no internet permission, no ads,
nothing leaves your phone. All it stores is your chosen times and today's
"taken" status, in the app's own local settings.

## Getting it onto your phone

This project can't be turned into an installable file without either a
computer with Android's build tools, or a cloud build service — I wasn't able
to compile it into a ready `.apk` myself in this environment (no access to
Google's Android SDK servers). Here are two easy ways to get the real file,
neither of which requires you to know how to code:

### Option A — GitHub Actions (recommended, no software to install)

This repo already includes a GitHub Actions workflow
(`.github/workflows/build-apk.yml`) that builds the APK for you automatically
in the cloud, for free.

1. Create a free account at [github.com](https://github.com) if you don't
   already have one.
2. Create a new repository (any name, e.g. `take-my-meds`) and upload every
   file/folder from this project to it. The easiest way: on the new repo's
   page, click **"Add file" → "Upload files"**, then drag the whole
   `TakeMyMeds` folder's contents in and commit.
3. Click the **"Actions"** tab of your repository. A workflow run called
   "Build APK" should start automatically (if it doesn't, click **"Build
   APK"** on the left, then **"Run workflow"**).
4. Wait for it to finish (a green checkmark, usually 2-4 minutes).
5. Click into that run, scroll down to **"Artifacts"**, and download
   **`TakeMyMeds-debug-apk`**. It downloads as a `.zip` — unzip it to get
   `app-debug.apk`.
6. Get that `.apk` file onto your Android phone any way you like (email it to
   yourself, upload to Google Drive/Dropbox and download on the phone, or a
   USB cable).
7. On your phone, open the `.apk` file. Android will warn that it's from an
   "unknown source" — this is normal for any app installed outside the Play
   Store. Tap through to allow installing from that source (e.g. your Files
   app or browser) and confirm the install.

### Option B — Android Studio (if you'd rather build it on your own computer)

1. Install [Android Studio](https://developer.android.com/studio) (free).
2. Open Android Studio → **"Open"** → select the `TakeMyMeds` folder.
3. Let it sync (first time may take a few minutes while it downloads the
   Android SDK components it needs).
4. Easiest: plug your phone in by USB with
   [USB debugging enabled](https://developer.android.com/studio/debug/dev-options),
   then click the green **Run ▶** button — it installs straight onto your
   phone.
   Alternatively: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**,
   then locate the generated `app-debug.apk` under `app/build/outputs/apk/debug/`
   and transfer it to your phone as in step 6/7 above.

## Using the app

1. Open **Take My Meds**.
2. The first time, allow the notification permission when asked (needed for
   Android 13+), and if a green **"Enable exact alarms"** button appears, tap
   it and allow the permission — this makes sure the reminder fires at
   exactly the right minute rather than possibly being delayed by the system.
3. Tap the time under **Dose 1** / **Dose 2** to set when you want each
   reminder (untick a dose's checkbox if you only take one per day).
4. Under each dose, tap the "Remind me again at" time to set that dose's
   one follow-up reminder — it only fires if you haven't tapped "Taken" by
   then. Untick "Remind me again at" for a dose to turn its follow-up off.
   (If you set a follow-up time earlier in the clock than the dose time
   itself, it'll fire the next day instead of later the same day — the app
   warns you about this on Save.)
5. Tap **Save & Schedule**.
6. When a reminder notification appears, tap **Taken** once you've taken your
   tablet. That's it — it won't nag you again until tomorrow.

## A tip for reliability

Some phone brands (Xiaomi/MIUI, Huawei, Oppo, Samsung, and others) are
aggressive about killing background apps to save battery, which can
occasionally stop reminders from firing on time. If reminders ever feel
unreliable, go into your phone's battery settings and turn off battery
optimization / add "autostart" permission for **Take My Meds** — a quick web
search for "[your phone brand] disable battery optimization for an app"
will show you exactly where that setting lives on your model.

## Project structure

- `app/src/main/java/com/takemymeds/app/MainActivity.kt` — the settings
  screen (set dose times, follow-up times, enable/disable a dose).
- `AlarmScheduler.kt` — schedules/cancels the underlying Android alarms.
- `AlarmReceiver.kt` — runs when a reminder time (or follow-up) arrives.
- `NotificationHelper.kt` — builds and shows the notification with the
  "Taken" button.
- `TakenActionReceiver.kt` — runs when you tap "Taken": marks the dose done
  and cancels the follow-up.
- `BootReceiver.kt` — re-schedules everything after your phone restarts.
