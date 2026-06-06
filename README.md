# PDF Toolbox

Android-native PDF tools app built with Kotlin, Jetpack Compose and Material 3. The app focuses on clear mobile workflows: select files, choose a PDF action, then save, share, open or start a new task.

## Main functions

- Compress PDFs.
- Merge multiple PDFs.
- Split PDFs by page ranges.
- Convert images to PDF.
- Rotate pages.
- Protect PDFs with a password or remove passwords from your own files.
- Search tools by title, description and keywords.
- Keep a local history with metadata only.
- Change language in settings.

PDF to images and PDF preview are shown as coming soon, not as Pro-only features.

## Pro / remove ads

- Product ID: `pdf_toolbox_pro`
- Price: **one-time 1.19 EUR**
- Pro removes ads only.
- All PDF functions are free for every user.
- There is no feature paywall and no Pro-only limit increase.

## Ads

- Free users may see a non-blocking ad after a successful merge.
- Pro users see no ads.
- If an ad cannot load, the result workflow still continues.
- UMP consent remains available at startup and in settings.
- Debug builds use Google test ad IDs.

## Languages

The app supports English, German, Spanish, French, Italian, Portuguese and Turkish via Android string resources. The selected language is saved in DataStore and the activity reloads after changes.

## Privacy and Data Safety notes

- Documents are processed locally on the device.
- The app does not upload document contents to own servers.
- History stores metadata only.
- Ads may run through Google AdMob and consent through Google UMP.
- Purchases run through Google Play Billing.
- Disclose Ads/Ad ID and Purchases in Play Data Safety when publishing.

## Build

```bash
./gradlew clean test assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release notes

Set a release keystore before building a release bundle:

```bash
export PDFWZ_KEYSTORE=/path/to/release-keystore.jks
export PDFWZ_KEYSTORE_PASSWORD=...
export PDFWZ_KEY_ALIAS=pdfwerkzeugkasten
export PDFWZ_KEY_PASSWORD=...
./gradlew bundleRelease
```

## Architecture

- `domain/model`: app models and searchable tool text.
- `domain/usecase`: page range parser, file name generator and free-for-all product policy checks.
- `data/pdf`: PDFBox-Android engine using `com.tom_roush.pdfbox...` imports.
- `data/history`: Room history without PDF contents.
- `data/settings`: DataStore for onboarding, theme, language and Pro/ad status.
- `data/billing`: Google Play Billing wrapper for `pdf_toolbox_pro`.
- `data/ads`: Google UMP/AdMob initialization and merge-ad abstraction.
