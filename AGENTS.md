# AppPDF Codex Instructions

Always start from latest origin/main.
Do not work from stale PR branches.
Before opening a PR, sync/rebase against origin/main.
Resolve merge conflicts before finishing.
Run ./gradlew clean test assembleDebug before finishing.
Do not finish with a broken build.

Build constraints:
- Java 17
- Real Gradle wrapper must stay committed
- android.useAndroidX=true
- android.enableJetifier=true

PDFBox-Android constraints:
- Use com.tom_roush.pdfbox imports
- Do not use org.apache.pdfbox imports
- Do not use PDFBoxResourceLoader
- Do not use PDRectangle.rotate()
- For landscape rectangles, swap width and height manually

Product rules:
- All PDF functions are free.
- Pro only removes ads.
- Pro price: one-time 1.19 EUR.
- No feature paywall.
- Main visible app title: PDF Toolbox.
