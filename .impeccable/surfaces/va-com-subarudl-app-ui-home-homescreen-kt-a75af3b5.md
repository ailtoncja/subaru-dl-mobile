---
version: 1
slug: "va-com-subarudl-app-ui-home-homescreen-kt-a75af3b5"
primary_target: "app/src/main/java/com/subarudl/app/ui/home/HomeScreen.kt"
related_targets: ["app/src/main/java/com/subarudl/app/ui/AppRoot.kt","app/src/main/java/com/subarudl/app/ui/home/DownloadList.kt","app/src/main/java/com/subarudl/app/ui/home/LinkSheets.kt","app/src/main/java/com/subarudl/app/ui/home/Failure.kt","app/src/main/java/com/subarudl/app/ui/settings/SettingsScreen.kt","app/src/main/java/com/subarudl/app/ui/theme/Theme.kt"]
---

## Scope

The whole Subaru DL Android interface (Jetpack Compose): one main screen that pastes, configures and follows downloads (it replaces the old Início, Fila, Playlists and Galeria tabs), the option sheets that rise over it (one link, several links, images), and Ajustes with its sub-pages, the login window and the receive-cookies sheet. Mode: Operate. Phone first (compact width); the same layout must stay usable on a tablet.

## Audience and task

General Portuguese-speaking public on Android who do not know yt-dlp; the author passes the APK to friends and intends to publish it. Links arrive by Compartilhar and by copy and paste, equally. Four first-class jobs: video, audio (MP3), playlists and channels, profile images. User-confirmed this round: keep the three Subaru colours and "tudo numa tela" (paste and follow on the same screen); everything else may change; every function of the PC app stays; PT-BR only, dark only, APK sideload. The user chose the category standard on purpose, with Seal and Google's own apps (Files, Photos) as the craft bar.

## Direction contract

THESIS: The category standard done properly: a Material 3 downloader any Android user reads at a glance, link field on top, download cards below, a Colar link button under the thumb. It refuses the incumbent's five-tab "Central de Downloads" with telemetry graphs and rally copy, and refuses any metaphor or costume on a working screen.

OWN-WORLD: A dark Material 3 scheme built from the Subaru colours: navy #0c1430 as the surface, with the surface-container steps climbing in the same navy hue; yellow #ffd200 as primary (FAB, progress, selection, primary buttons) with dark on-primary text; gold #d9b44a as tertiary, meaning finished; Material error roles for failures. Roboto (system) on the Material type scale, tabular figures for sizes, speeds and times. Material shapes: 12dp cards, 28dp pill link field and sheet corners, 16dp FAB, full-round buttons, 8dp chips. Material Symbols outlined icons. Tonal elevation only.

STORY: Someone shares a link from YouTube, Instagram or TikTok, or pastes it; the option sheet rises with cover, title and the real qualities with their sizes; they tap Baixar and the card appears at the top of the list, filling its progress bar; when it finishes the card turns gold-checked and a tap opens the file. Playlists show their items to tick, image profiles show a grid to tick, and failures say what happened in plain Portuguese with the way out (Tentar de novo, Entrar na conta, Baixar como imagens).

FIRST VIEWPORT: Small top app bar: "Subaru DL" at left, search and Ajustes at right. Under it the 56dp pill link field (link icon, "Cole ou compartilhe um link", paste or clear at the end), then single-select chips Vídeo / Áudio / Imagens. Then the list, in sections with their own actions: Baixando (total speed, Pausar tudo), Na fila, Falhas (Limpar), Concluídos (Limpar). Each card: 16:9 thumbnail 104dp wide at left, title in two lines, a 4dp rounded progress bar while active, one meta line (size of total, speed, time left, or format, quality and size), one trailing action (pause or resume, or the overflow menu). Storage line at the foot. Extended FAB "Colar link" bottom right, collapsing to the icon while the list scrolls. Empty state: the app icon, how to share into the app, and the supported sites in one line. Ajustes is its own screen with a back arrow, Material list rows grouped under headers.

FORM: Canon, the category standard played straight (outside my ordered list; the roll had assigned candidate 4, Letreiro de Ônibus); seed:84231190cd0c5ef9. Craft bar: Seal and Google Files/Photos. Signature interaction and motion grammar: the option sheet rising from the bottom over the list, the new card sliding into the top of the list as the sheet settles (list item placement animation), the FAB collapsing while scrolling, a horizontal shared-axis move between the main screen and Ajustes; Material standard easing everywhere, and the system Remove animations setting respected.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Memorable moment

Share from another app, the sheet rises already showing the cover and the real sizes, one tap, and the card is filling on the same screen.

## Unresolved

Whether Imagens should be detected from the link instead of chosen on a chip (for now: chip, plus "Baixar como imagens" when video extraction fails on an image site).

## Adaptations (recorded after the finish review)

- Tablet: the main list and every Ajustes page sit in a centered column of at most 720dp (`ContentMaxWidth`, `sideFor()`), and the top app bar and the search bar take the same side margin through `windowInsets`, so the title and actions line up with the column; sheets keep the Material default width (640dp). Scrolling still works across the whole width.
- FAB "Colar link": collapses to the icon while the list scrolls down and expands again when it scrolls up (Files and Seal behaviour), instead of collapsing only once the link bar leaves the screen.
- Failures: a card shows one line of cause in the error colour and the way out as a text button under it (Baixar como vídeo, Entrar na conta, Baixar como imagens, Tentar sem o Turbo, Tentar de novo); the overflow opens the details sheet, which repeats the full explanation, a short technical detail and both buttons. The link notice names the site ("O YouTube pediu login para este link.") and keeps the English yt-dlp text behind "Ver o detalhe técnico".
- "Limpar" (Falhas, Concluídos) and "Tirar da lista" answer with a snackbar carrying "Desfazer".
- A link with a single quality shows it as a text line under "Qualidade" instead of a lone selected chip; on YouTube a hint explains that signing in usually releases the others (not captured: YouTube blocks the emulator).
- With one active download, the speed lives only in the "Baixando" header and the card shows percent and time left, so the meta line fits at font scale 1.3.
- Images: Photos-style selection (selected tiles inset with a filled yellow check, unselected tiles full-bleed with an empty ring on a small corner scrim); the open/close toggle of each link is a chevron.
- Selection colour: chips and segmented buttons select with the Material tonal navy (`secondaryContainer`), as in Seal and Google's apps; yellow stays for the FAB, progress, primary buttons, switches and the image check, so it keeps meaning "the next thing to do".
- Elevation: surfaces climb by tone only, but the FAB, menus and snackbar keep the Material default shadow, because they float over moving content.
- Engine fixes found while verifying the UI: with Turbo on, aria2 now prints a summary line every second so the card shows live progress, and the Rede avançada speed limit reaches aria2; technical text fields in Ajustes use the URI keyboard without autocorrect.
