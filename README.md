# PDF Werkzeugkasten

Android-native MVP für eine datenschutzfreundliche PDF-Tools-App mit Kotlin, Jetpack Compose und Material 3. Die App ist mobil-first gestaltet: große Touch-Flächen, klare Cards, echte Suche, verständliche Workflows und lokale PDF-Verarbeitung.

## Funktionen im MVP

- PDF komprimieren: lokale PDFBox-Speicherung mit strukturellem Cleanup und Ergebnis-Screen.
- PDFs zusammenführen: mehrere PDFs per SAF auswählen und lokal mergen.
- PDF teilen: Seitenbereiche wie `1-3, 5, 8-10` extrahieren.
- Bilder zu PDF: Android Photo Picker, A4-Ausgabe, lokale Generierung.
- Seiten drehen, Passwort schützen und Passwort entfernen.
- Ergebnis speichern, teilen, öffnen oder direkt einen neuen Vorgang starten.
- Lokale Historie mit Room, Einstellungen/Pro-Status/Sprache mit DataStore.
- Echte Tool-Suche über Titel, Beschreibung und Keywords.
- Share Intents für PDFs und Bilder.
- UMP Consent-Start, AdMob-Test-IDs und Google-Play-Billing-Vorbereitung.
- Release-Konfiguration mit R8, Version `1.0.0`, `versionCode 1`, `targetSdk 35`.

## Build-Anleitung

```bash
./gradlew test assembleDebug
```

Falls der Wrapper noch nicht erzeugt wurde:

```bash
gradle wrapper --gradle-version 8.14.4
./gradlew test assembleDebug
```

Die Debug-APK liegt danach unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Für echte Releases eine Keystore-Datei setzen:

```bash
export PDFWZ_KEYSTORE=/path/to/release-keystore.jks
export PDFWZ_KEYSTORE_PASSWORD=...
export PDFWZ_KEY_ALIAS=pdfwerkzeugkasten
export PDFWZ_KEY_PASSWORD=...
./gradlew bundleRelease
```

## Test-Anleitung auf Handy

1. `app-debug.apk` installieren.
2. Onboarding abschließen.
3. Auf dem Home Screen nach `merge`, `Passwort`, `Bilder` oder `compress` suchen und prüfen, dass die Tool-Liste live gefiltert wird.
4. Zwei oder mehr PDFs auswählen und zusammenführen.
5. Als Free-Nutzer nach erfolgreichem Merge den Debug-Test-Ad-Platzhalter bzw. eine Test-Interstitial-Anzeige prüfen.
6. Ergebnis speichern, teilen, öffnen und danach „Neuer Vorgang“ testen.
7. In den Einstellungen die Sprache zwischen Deutsch und Englisch wechseln.
8. Dark Mode/Systemdesign testen.
9. Fehlerfälle testen: leere Auswahl, ungültige PDF, falsches Passwort, mehr als 5 PDFs im Free-Plan, mehr als 10 Bilder im Free-Plan.

## Architektur

- `domain/model`: App- und Job-Modelle plus suchbare Tool-Texte.
- `domain/usecase`: Seitenbereich-Parser, Dateinamen-Generator, Free/Pro-Limits.
- `data/pdf`: PDF-Engine-Abstraktion auf Basis PDFBox-Android (`com.tom_roush.pdfbox...`) und Android `PdfRenderer`.
- `data/history`: Room-Verlauf ohne PDF-Inhalte.
- `data/settings`: DataStore für Onboarding, Theme, Sprache und Pro-Status.
- `data/billing`: Google Play Billing Wrapper für `pdf_toolbox_pro`.
- `data/ads`: Google UMP/AdMob Initialisierung und Merge-Ad-Abstraktion.
- Compose UI: Onboarding, Home, Result, History, Pro und Settings mit Material 3.

## Pro-Version

- Produkt-ID: `pdf_toolbox_pro`
- Preis: **einmalig 1,19 €**
- Vorteile in der UI:
  - Keine Werbung
  - Höhere Limits
  - Alle Premium-Funktionen

Google Play Billing ist vorbereitet. Im Debug-Build crasht die App nicht, wenn Produktdetails noch nicht in der Play Console verfügbar sind; stattdessen wird eine verständliche Meldung angezeigt.

## Werbung

- Free-Nutzer sehen nach erfolgreichem PDF-Merge eine Anzeige bzw. im Debug-Fall einen Test-Ad-Platzhalter.
- Pro-Nutzer sehen keine Werbung.
- Werbung wird nicht während der Verarbeitung angezeigt und blockiert den Workflow nicht, wenn kein Ad geladen werden kann.
- UMP Consent wird beim App-Start und über Einstellungen berücksichtigt.
- Debug nutzt Google-Test-Ad-IDs.

## Spracheinstellungen

Die App unterstützt mindestens:

- Deutsch
- Englisch

Alle sichtbaren Compose-UI-Texte liegen in Android String Resources (`values/strings.xml`, `values-de/strings.xml`). Die gewählte Sprache wird in DataStore gespeichert und kann in den Einstellungen umgeschaltet werden.

## Library- und Lizenzhinweise

- AndroidX, Jetpack Compose, Room, DataStore, WorkManager: Apache License 2.0.
- Coil: Apache License 2.0.
- PDFBox-Android (`com.tom-roush:pdfbox-android`): Apache License 2.0. Keine AGPL-PDF-Library wird verwendet.
- Google Mobile Ads SDK, UMP SDK, Google Play Billing: Google SDK Terms; für Play-Store-Nutzung vorgesehen.
- Firebase Crashlytics ist nicht aktiviert, um sensible Dokumentinhalte nicht versehentlich in Crashlogs zu senden.

## Datenschutz-Hinweis

PDF Werkzeugkasten verarbeitet ausgewählte PDFs und Bilder lokal auf dem Gerät. Dokumente werden nicht auf eigene Server hochgeladen, nicht inhaltlich analysiert und nicht für Konten synchronisiert. Der Verlauf speichert nur Metadaten wie Tool-Typ, Dateiname, Zeitpunkt und Größenangaben. Fertige Dateien werden erst gespeichert, wenn Nutzer im Android-Speicherdialog einen Speicherort wählen.

Für Werbung können Google AdMob und das Google UMP SDK Gerätekennungen einschließlich Werbe-ID und Consent-Signale verarbeiten. EU-Nutzer erhalten eine Zustimmungsauswahl; nicht-personalisierte Werbung soll angeboten werden, wenn keine Personalisierung gewünscht ist. In-App-Käufe werden über Google Play Billing verarbeitet. Crashlytics ist im MVP deaktiviert. Nutzer können App-Daten über Android-Systemeinstellungen löschen. Kontakt: `privacy@example.com`.

## Google-Play-Store-Listing

**Kurzbeschreibung:** PDFs komprimieren, zusammenführen, teilen und Bilder in PDFs umwandeln – schnell, einfach und lokal auf deinem Gerät.

**Lange Beschreibung:**
PDF Werkzeugkasten ist deine einfache All-in-One-App für tägliche PDF-Aufgaben. Komprimiere große PDF-Dateien, führe mehrere PDFs zusammen, teile Seitenbereiche auf, wandle Bilder in PDFs um, exportiere PDF-Seiten als Bilder und schütze Dokumente mit Passwort.

Die App ist für schnelle Workflows gebaut: Datei auswählen, Option wählen, Ergebnis speichern oder teilen. Viele Vorgänge laufen lokal auf deinem Gerät, ohne dass deine Dokumente auf Server hochgeladen werden.

PDF Werkzeugkasten Pro entfernt Werbung und hebt Limits für Power-User auf.

## Play Data Safety Notizen

- Werbung/Ad-ID offenlegen, sobald AdMob aktiv ausgeliefert wird.
- Käufe offenlegen, weil Google Play Billing integriert ist.
- Crashdaten nur offenlegen, falls Crashlytics später aktiviert wird.
- Dokumentinhalte werden nicht gesammelt und nicht auf eigene Server hochgeladen.

## Bekannte Einschränkungen

- PDF-Komprimierung ist im MVP konservativ; aggressive Bild-Rekomprimierung ist wegen Qualitätsrisiko als Pro-Roadmap vorbereitet.
- Billing benötigt ein reales Play-Console-In-App-Produkt `pdf_toolbox_pro`, bevor Käufe produktiv funktionieren.
- PDF-zu-Bilder und detailliertes per-Seite-Drehen sind UI-seitig vorbereitet, aber noch nicht vollständig ausgebaut.
