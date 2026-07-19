# Bug-audit — GameSide AI 1.0.0 MVP

Auditdatum: 18 juli 2026

Update 19 juli 2026: de fysieke AYN Thor verwijdert bij iedere bovenste gamestart de onderste GameSide-activity. De defensieve `1.1.1`-herstelfunctie bleek dit niet betrouwbaar te kunnen omzeilen. Op gebruikersverzoek verwijdert `1.1.2-lower-screen` de companion-sessie volledig en start GameSide voortaan automatisch beneden. Gelijktijdig GameSide beneden plus game boven blijft een vastgelegde Thor-firmwarebeperking; de omgekeerde indeling werkt wel.

Update 20 juli 2026: gebruikersfeedback verfijnde het patroon: de sluiting is vooral mogelijk wanneer GameSide als eerste beneden opent; eerst een app boven openen voorkomt het probleem. `1.1.3-game-detection` houdt daarom een lege primaire task achter de bovenste content en creëert dezelfde anchor bij een lower-origin launch. Dit is lokaal compile-/lint-/unitgetest maar blijft **in fysieke acceptatietest**.

Gecontroleerde codebasis: commit `47bd4bc` (`main`)

Testtoestel: Huawei P30 Lite / MAR-LX1A / Android 9 (API 28)

## Samenvatting

De app compileert, start en bewaart bestaande gebruikersdata correct. Er zijn **geen kritieke bugs, crashes, buildfouten of direct aangetroffen geheimen** gevonden. De audit vond wel **3 middelzware en 5 lichte bugs**. Geen van deze blokkeert het huidige basisgebruik — een game toevoegen, een DeepSeek-sleutel opslaan, vragen stellen, game-wiki-bronnen ophalen, antwoorden bewaren en de companion-interface openen werken.

| Ernst | Aantal | Betekenis |
|---|---:|---|
| Kritiek | 0 | Geen blokkade, datalek of onherstelbaar algemeen dataverlies gevonden |
| Hoog | 0 | Geen algemene crash of kernfunctie-uitval gevonden |
| Middel | 3 | Randgevallen met onjuiste status, chatgeschiedenis of back-upimport |
| Laag | 5 | Beperkte UX-, foutmelding- of geïmporteerde-data-problemen |

## Gevonden bugs

### GS-01 — API-sleutelstatus loopt niet altijd gelijk tussen AI en Privacy

**Ernst:** middel

**Bestanden:** `ProviderSettingsViewModel.kt`, `RoomPrivacyRepository.kt`

De twee schermen houden elk een eigen, niet-gedeelde sleutelstatus bij. Als de sleutel onder **Privacy** wordt verwijderd, kan **More > AI** tot een procesherstart nog steeds `Stored` tonen en de verbindingstest inschakelen. Opslaan of verwijderen onder **AI** kan andersom kort een verouderde status onder **Privacy** tonen.

**Impact:** de sleutel zelf blijft Keystore-beveiligd, maar de interface kan ten onrechte melden dat hij aanwezig of verwijderd is. Een verbindingstest geeft daarna pas de werkelijke fout.

**Aanbevolen oplossing:** maak de aanwezigheid van de credential één gedeelde observeerbare bron en laat beide ViewModels die bron gebruiken. Controleer daarnaast het resultaat van `SharedPreferences.commit()` bij verwijderen.

### GS-02 — Stoppen van generatie laat een onbeantwoorde gebruikersvraag in de geschiedenis staan

**Ernst:** middel

**Bestand:** `ChatViewModel.kt` (`sendQuestion`, `stop`)

De gebruikersvraag wordt in Room opgeslagen voordat bronzoekwerk en AI-generatie starten. **Stop** annuleert de taak en wist alleen de tijdelijke antwoordtekst; de opgeslagen vraag wordt niet verwijderd of als geannuleerd gemarkeerd.

**Impact:** de afgebroken vraag blijft in de conversatie en wordt bij een volgende aanvraag als eerdere context naar DeepSeek gestuurd. Dit kan antwoorden verwarren en kost onnodige contexttokens.

**Reproductie:** verstuur een vraag, druk tijdens genereren op Stop en verstuur daarna een andere vraag in dezelfde conversatie.

**Aanbevolen oplossing:** verwijder de zojuist opgeslagen vraag bij annulering, of bewaar vraag en definitief antwoord atomair met een expliciete status `pending/cancelled/completed`.

### GS-03 — Een geldige samenvoegback-up kan botsen op een reeds bewaard antwoord

**Ernst:** middel

**Bestanden:** `BackupDao.kt`, `PersonalToolsEntity.kt`

`saved_answers` heeft een unieke index op `sourceMessageId`, terwijl `@Upsert` conflicten op de primaire `id` afhandelt. Twee toestelkopieën kunnen hetzelfde bronbericht met verschillende saved-answer-ID's bewaren. Bij samenvoegen ontstaat dan een unieke-indexconflict en wordt de volledige importtransactie teruggedraaid.

**Impact:** er gaat door de transactie geen bestaande data verloren, maar een verder geldige back-up kan niet worden geïmporteerd.

**Aanbevolen oplossing:** vóór de upsert dedupliceren op `sourceMessageId`, of een gerichte conflictstrategie/DAO-query gebruiken en dit scenario instrumenteel testen.

### GS-04 — ‘Retry answer’ voegt de vraag opnieuw toe en gebruikt het oude antwoord als context

**Ernst:** laag

**Bestand:** `ChatViewModel.kt` (`retryAnswer`)

Retry zoekt de oorspronkelijke vraag en roept daarna het normale verzendpad aan. Daardoor wordt dezelfde gebruikersvraag opnieuw aan het einde opgeslagen. De context bevat dan ook het antwoord dat juist opnieuw gegenereerd moest worden.

**Impact:** de chat krijgt dubbele vragen en de nieuwe uitkomst is geen onafhankelijke regeneratie.

**Aanbevolen oplossing:** maak een apart regenerate-pad dat het gekozen antwoord vervangt of een zichtbare antwoordvariant maakt, zonder een tweede user-turn toe te voegen.

### GS-05 — Mislukte wiki-paginadownload kan zonder melding eindigen

**Ernst:** laag

**Bestand:** `WikiViewModel.kt`

Fouten bij het ophalen van elk zoekresultaat worden met `runCatching(...).getOrNull()` onderdrukt. Alleen nul zoekresultaten geven een melding. Als er wel resultaten zijn maar geen enkele pagina kan worden gedownload, eindigt de laadstatus zonder pagina en zonder fouttekst.

**Impact:** de gebruiker ziet geen resultaat en weet niet of opnieuw proberen zin heeft.

**Aanbevolen oplossing:** tel geslaagde downloads en toon een fout wanneer dat aantal nul is; meld bij gedeeltelijk succes hoeveel pagina's niet konden worden opgehaald.

### GS-06 — More-subtab springt na configuratiewijziging terug naar Displays

**Ernst:** laag

**Bestand:** `MoreScreen.kt`

De keuze **Displays / AI / Privacy** gebruikt `remember` in plaats van `rememberSaveable`.

**Impact:** bij bijvoorbeeld rotatie of een activity-recreatie wordt de gekozen sectie vergeten.

**Aanbevolen oplossing:** gebruik `rememberSaveable { mutableIntStateOf(0) }` of navigeer met een opgeslagen route.

### GS-07 — Bewerken kan geavanceerde geïmporteerde profielvelden verwijderen

**Ernst:** laag

**Bestanden:** `GameLibraryViewModel.kt`, `GameLibraryScreen.kt`

Het editformulier toont één package en één wiki-URL en bevat geen velden voor cover, spelerprogressie of custom system prompt. Opslaan bouwt het profiel opnieuw op en zet die niet-getoonde waarden op `null`; extra packages/wiki-bronnen worden teruggebracht tot één.

**Impact:** normaal in de huidige UI gemaakte profielen hebben deze extra waarden niet. Data uit een uitgebreid of handmatig samengesteld back-upbestand kan bij bewerken wel stil verloren gaan.

**Aanbevolen oplossing:** kopieer onbewerkte velden uit het bestaande profiel en wijzig alleen waarden die het formulier daadwerkelijk beheert.

### GS-08 — Wisselen van actieve game tijdens generatie kan tijdelijk het verkeerde antwoord tonen

**Ernst:** laag

**Bestand:** `ChatViewModel.kt`

De lopende generatie gebruikt terecht de oorspronkelijke game en sessie, maar `streaming`, `generating` en citations zijn globale ViewModel-state. Als de actieve game tijdens de aanvraag verandert, kunnen de tijdelijke antwoordtekst en laadstatus onder de nieuwe game verschijnen. Het definitieve antwoord wordt wel in de oorspronkelijke sessie opgeslagen.

**Impact:** tijdelijke verwarring en mogelijk zichtbare spoilertekst bij het verkeerde gameprofiel; geen permanente cross-game opslag aangetroffen.

**Aanbevolen oplossing:** koppel generation-state aan game- en sessie-ID, annuleer bij gamewissel of blokkeer wisselen zolang een antwoord loopt.

## Uitgevoerde controles en resultaten

| Controle | Resultaat |
|---|---|
| Schone Gradle-build | Geslaagd |
| Debug APK | Geslaagd |
| Geminificeerde R8 release-build | Geslaagd |
| `lintVitalRelease` | Geslaagd |
| JVM-tests | 6/6 geslaagd |
| Huawei instrumentatietests | 7/7 geslaagd |
| Android Lint | 0 fouten, 21 waarschuwingen |
| Cold launch Huawei | Geslaagd; `MainActivity` resumed |
| Crashlog na cold launch | Geen `AndroidRuntime`, Room- of SQLite-fout |
| Bestaande lokale data na tests | Behouden; bestaande Elden Ring-chat zichtbaar |
| Repository-secret scan | Geen API-key of private key aangetroffen |
| APK-checksum in repository | Komt overeen met gepubliceerde SHA-256 |

De 21 lintwaarschuwingen bestaan uit beschikbare dependency-updates en niet-blokkerende KTX/SharedPreferences-stijladviezen. Ze zijn niet als appbug geteld.

## Security- en privacybevindingen

- Alleen internet- en netwerkstatusrechten zijn aangevraagd.
- Alleen de launcher-activity is geëxporteerd; de companion-activity is niet geëxporteerd.
- Cleartext-verkeer is uitgeschakeld en provider- en wiki-aanvragen vereisen HTTPS.
- De DeepSeek-sleutel staat niet in broncode, APK-downloaddocumentatie of JSON-back-ups en wordt lokaal met Android Keystore/AES-GCM versleuteld.
- De meegeleverde APK is met een lokale test/debugcertificaatketen ondertekend. Dat is geschikt voor privétesten, maar vóór bredere distributie is een duurzaam offline bewaarde release-key nodig.

## Nog ontbrekende dekking

Dit zijn geen bevestigde bugs, maar blijven acceptance-risico's:

- geen geautomatiseerde Compose UI/end-to-end-tests voor alle knoppen en rotaties;
- geen `MigrationTestHelper`-test die elk opgeslagen Room-schema 1→5 opent en migreert;
- geen geautomatiseerde DeepSeek- of live game-wiki-contracttest (netwerk en kosten maken die bewust apart);
- fysieke dual-screen touch, focus, lid/sleep en firmwaregedrag op de AYN Thor zijn nog niet gevalideerd;
- gelijktijdig een echte game op het primaire AYN-scherm en GameSide AI op het tweede scherm is nog niet als hardware-acceptatietest uitgevoerd;
- installatie van toekomstige updates moet nog met een definitieve release-key en verhoogde `versionCode` worden gevalideerd.

## Conclusie

`1.0.0-mvp` is bruikbaar voor privétesten op de Huawei en AYN Thor. De drie middelzware bugs horen als eerste in een onderhoudsrelease: gedeelde credentialstatus, correcte annulering van chatgeneratie en back-updeduplicatie. Daarna zijn retrygedrag en wiki-foutfeedback de nuttigste UX-verbeteringen.
