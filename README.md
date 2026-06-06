# PDF Werkzeugkasten

Android-native MVP für eine datenschutzfreundliche PDF-Tools-App mit Kotlin, Jetpack Compose und Material 3.

## Funktionen im MVP

- PDF komprimieren: lokale PDFBox-Speicherung mit strukturellem Cleanup und Ergebnis-Screen.
- PDFs zusammenführen: mehrere PDFs per SAF auswählen und lokal mergen.
- PDF teilen: Seitenbereiche wie `1-3, 5, 8-10` extrahieren.
- Bilder zu PDF: Android Photo Picker, A4-Ausgabe, lokale Generierung.
- Seiten drehen, Passwort schützen und Passwort entfernen.
- Ergebnis speichern, teilen und öffnen.
- Lokale Historie mit Room, Einstellungen/Pro-Status mit DataStore.
- Share Intents für PDFs und Bilder.
- UMP Consent-Start, AdMob-Test-App-ID, Google-Play-Billing-Abstraktion.
- Release-Konfiguration mit R8, Version `1.0.0`, `versionCode 1`, `targetSdk 35`.

## Build-Anleitung

```bash
gradle wrapper --gradle-version 8.14.4
./gradlew test
./gradlew assembleDebug
./gradlew bundleRelease
```

Für echte Releases eine Keystore-Datei setzen:

```bash
export PDFWZ_KEYSTORE=/path/to/release-keystore.jks
export PDFWZ_KEYSTORE_PASSWORD=...
export PDFWZ_KEY_ALIAS=pdfwerkzeugkasten
export PDFWZ_KEY_PASSWORD=...
./gradlew bundleRelease
```

## Architektur

- `domain/model`: App- und Job-Modelle.
- `domain/usecase`: Seitenbereich-Parser, Dateinamen-Generator, Free/Pro-Limits.
- `data/pdf`: PDF-Engine-Abstraktion auf Basis PDFBox-Android und Android `PdfRenderer`.
- `data/history`: Room-Verlauf ohne PDF-Inhalte.
- `data/settings`: DataStore für Onboarding, Theme und Pro-Status.
- `data/billing`: Google Play Billing Wrapper mit Restore-Flow-Platzhalter.
- `data/ads`: Google UMP/AdMob Initialisierung.
- `theme`/Compose UI: Material-3-Oberfläche mit Onboarding, Home, Result, History, Pro, Settings.

## Library- und Lizenzhinweise

- AndroidX, Jetpack Compose, Room, DataStore, WorkManager: Apache License 2.0.
- Coil: Apache License 2.0.
- PDFBox-Android (`com.tom-roush:pdfbox-android`): Apache License 2.0. Keine AGPL-PDF-Library wird verwendet.
- Google Mobile Ads SDK, UMP SDK, Google Play Billing: Google SDK Terms; für Play-Store-Nutzung vorgesehen.
- Firebase Crashlytics ist nicht aktiviert, um sensible Dokumentinhalte nicht versehentlich in Crashlogs zu senden.

## Datenschutz-Entwurf

PDF Werkzeugkasten verarbeitet ausgewählte PDFs und Bilder lokal auf dem Gerät. Dokumente werden nicht auf eigene Server hochgeladen, nicht inhaltlich analysiert und nicht für Konten synchronisiert. Der Verlauf speichert nur Metadaten wie Tool-Typ, Dateiname, Zeitpunkt und Größenangaben. Fertige Dateien werden erst gespeichert, wenn Nutzer im Android-Speicherdialog einen Speicherort wählen.

Für Werbung können Google AdMob und das Google UMP SDK Gerätekennungen einschließlich Werbe-ID und Consent-Signale verarbeiten. EU-Nutzer erhalten eine Zustimmungsauswahl; nicht-personalisierte Werbung soll angeboten werden, wenn keine Personalisierung gewünscht ist. In-App-Käufe werden über Google Play Billing verarbeitet. Crashlytics ist im MVP deaktiviert. Nutzer können App-Daten über Android-Systemeinstellungen löschen. Kontakt: `privacy@example.com`.

Drittanbieter: Google AdMob, Google User Messaging Platform, Google Play Billing. Optional später Firebase Crashlytics nur nach Datenschutzerklärung-Update.

## Google-Play-Store-Listing

**Kurzbeschreibung:** PDFs komprimieren, zusammenführen, teilen und Bilder in PDFs umwandeln – schnell, einfach und lokal auf deinem Gerät.

**Lange Beschreibung:**
PDF Werkzeugkasten ist deine einfache All-in-One-App für tägliche PDF-Aufgaben. Komprimiere große PDF-Dateien, führe mehrere PDFs zusammen, teile Seitenbereiche auf, wandle Bilder in PDFs um, exportiere PDF-Seiten als Bilder und schütze Dokumente mit Passwort.

Die App ist für schnelle Workflows gebaut: Datei auswählen, Option wählen, Ergebnis speichern oder teilen. Viele Vorgänge laufen lokal auf deinem Gerät, ohne dass deine Dokumente auf Server hochgeladen werden.

Funktionen:
- PDF komprimieren
- Mehrere PDFs zusammenführen
- PDF in einzelne Seiten oder Bereiche teilen
- Bilder zu PDF konvertieren
- PDF-Seiten als Bilder exportieren (Roadmap/Pro)
- Seiten drehen
- PDF mit Passwort schützen
- Passwort entfernen, wenn du das Passwort kennst
- PDF-Vorschau
- Verlauf deiner letzten Vorgänge
- Teilen direkt aus anderen Apps

PDF Werkzeugkasten Pro entfernt Werbung und hebt Limits für Power-User auf.

**Keywords:** PDF komprimieren, PDF zusammenführen, Bilder zu PDF, PDF teilen, PDF konverter, PDF bearbeiten, PDF drehen, PDF schützen, PDF scanner, PDF tools

**Kategorie:** Produktivität oder Tools.  
**Content Rating:** Für alle Altersgruppen geeignet.

## Screenshot-Plan

1. Home Screen mit allen Tools.
2. PDF komprimieren mit Vorher/Nachher-Größe.
3. PDFs zusammenführen mit Reihenfolge.
4. Bilder zu PDF Flow.
5. Ergebnis-Screen mit Speichern/Teilen.
6. Pro Screen.
7. Datenschutz-Hinweis „Dateien bleiben lokal“.

## Play Data Safety Notizen

- Werbung/Ad-ID offenlegen, sobald AdMob aktiv ausgeliefert wird.
- Käufe offenlegen, weil Google Play Billing integriert ist.
- Crashdaten nur offenlegen, falls Crashlytics später aktiviert wird.
- Keine Angabe machen, dass gar keine Daten gesammelt werden, solange Ads/Billing aktiv sind.
- Dokumentinhalte werden nicht gesammelt und nicht auf eigene Server hochgeladen.

## Testplan

- Unit: Seitenbereich-Parser, Dateinamen-Generator, Pro-Limit-Logik, Kompressionsoptionen.
- Instrumentation: App-Paket lädt; manuelle Erweiterung für SAF/Photo-Picker-Flows auf Gerät.
- Manuell: PDF auswählen, komprimieren, Ergebnis speichern/teilen/öffnen.
- Manuell: 2–5 PDFs zusammenführen und Free-Limit mit 6 Dateien prüfen.
- Manuell: Bilder zu PDF bis 10 Bilder und Limitprüfung mit 11 Bildern.
- Manuell: falsches Passwort, beschädigte PDF, leere Auswahl, Offline-Modus, Dark Mode, Rotation.

## Bekannte Einschränkungen

- PDF-Komprimierung ist im MVP konservativ; aggressive Bild-Rekomprimierung ist wegen Qualitätsrisiko als Pro-Roadmap vorbereitet.
- Billing nutzt die offizielle Library, benötigt aber ein reales Play-Console-In-App-Produkt `pdf_toolbox_pro`, bevor Käufe produktiv funktionieren.
- AdMob verwendet eine Test-App-ID; produktive IDs müssen vor Release ersetzt werden.
- PDF-zu-Bilder und detailliertes per-Seite-Drehen sind UI-seitig vorbereitet, aber nicht vollständig ausgebaut.

## Nächste Verbesserungen nach MVP

- Drag-and-drop-Reordering für Merge/Bilder-Flow.
- Batch-Komprimierung mit WorkManager-Fortschrittsnotifications.
- Pro-Produktdetails, Kaufbestätigung und Purchase-Acknowledgement produktiv anbinden.
- Echte Banner/Interstitial-Platzierung mit Frequency-Capping.
- Thumbnail-Grid für Seiten drehen und PDF-Vorschau mit Zoom.
- Lizenz-Screen aus Gradle-Dependency-Report generieren.
