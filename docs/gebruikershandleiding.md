# GameSide AI gebruikershandleiding

Deze handleiding beschrijft de `1.1.1-companion-hotfix` test-build. Je hebt geen pc of USB-kabel nodig wanneer je de APK vanuit de privé GitHub-repository downloadt.

## 1. Downloaden en installeren op de AYN Thor

1. Open Chrome op de Thor.
2. Log in op GitHub met een account dat toegang heeft tot `bimberlotDEV/AYN-Thor-AI-assistent-second-screen`.
3. Open de repository en ga naar `downloads`.
4. Open `GameSideAI-1.1.1-companion-hotfix-test.apk` en kies **Download raw file**, of gebruik de downloadlink in de README.
5. Bevestig de downloadwaarschuwing uitsluitend als bestandsnaam en repository kloppen.
6. Open het bestand via de downloadmelding of de app Bestanden.
7. Android vraagt mogelijk toestemming om onbekende apps vanuit Chrome of Bestanden te installeren. Sta dit tijdelijk toe.
8. Kies **Installeren** en daarna **Openen**.
9. Trek de tijdelijke installatiepermission desgewenst weer in via Android-instellingen.

Wanneer Android meldt dat de app niet kan worden bijgewerkt, is de bestaande installatie waarschijnlijk met een andere key ondertekend. Exporteer eerst een JSON-back-up, verwijder de oude app, installeer de nieuwe APK, importeer de back-up en voer de API-key opnieuw in.

## 2. Onboarding

De onboarding legt uit:

- dat GameSide AI een companion is en AI-antwoorden fouten kunnen bevatten;
- dat je altijd zelf een actieve game kiest;
- hoe spoilers per game worden beperkt;
- welke vraag- en gamecontext naar DeepSeek en de game-Wiki gaat;
- dat optionele toekomstige invoerfuncties niet continu meeluisteren of opnemen.

Kies **Continue** om de onboarding lokaal als voltooid op te slaan.

## 3. DeepSeek instellen

Open **More → AI**.

1. Plak je persoonlijke DeepSeek API-key in het beveiligde veld.
2. Kies **Encrypt & save**. De sleutel wordt versleuteld opgeslagen en het invoerveld wordt leeggemaakt.
3. Kies een model:
   - **V4 Flash** voor lagere latency en normaal gebruik;
   - **V4 Pro** wanneer je een sterker model wilt gebruiken.
4. Kies een antwoordlimiet:
   - `512` voor korte antwoorden en lager verbruik;
   - `900` als gebalanceerde standaard;
   - `1500` voor meer detail en hoger mogelijk verbruik.
5. Kies **Test DeepSeek connection**. Een geslaagde test toont een bevestiging.

GameSide toont geen geschatte geldprijs, omdat actuele tarieven en je provideraccount buiten de app vallen.

## 4. Een game toevoegen

Open **Games** en tik op `+`.

- **Game title** is verplicht.
- **Android package** is optioneel en alleen nodig om een geïnstalleerde Android-game op het primaire display te starten, bijvoorbeeld `com.example.game`.
- **Game Wiki URL** is optioneel. Laat dit leeg om automatisch een geschikte wiki.gg/Fandom/MediaWiki-bron te zoeken.
- Kies `ANDROID`, `EMULATED` of `OTHER`.
- Kies de gewenste spoilerstand.

Na opslaan wordt de eerste game automatisch actief. Bij meerdere games tik je op de gewenste kaart totdat **ACTIVE GAME** wordt getoond. Met de andere knoppen kun je een game pinnen, bewerken of na bevestiging inclusief gekoppelde data verwijderen.

## 5. Een vraag stellen

Open **Ask**. Bovenaan staan de actieve game, het huidige gesprek, de spoilerstand en outputlimiet.

1. Typ een concrete vraag, bijvoorbeeld `Waar vind ik Moonveil?`.
2. Tik op verzenden.
3. GameSide zoekt eerst relevante game-Wiki-informatie.
4. Het antwoord verschijnt tijdens het genereren in beeld.
5. Gebruik **Stop answer** om generatie af te breken.
6. Open genummerde bronnen onder het antwoord om feiten te controleren.

Antwoordacties:

- **Copy answer** kopieert de tekst naar het Android-klembord.
- **Retry answer** stelt dezelfde voorafgaande vraag opnieuw.
- **Convert answer to checklist** maakt een offline checklist in Saved.
- **Save answer** bewaart vraag, antwoord en bronnen.

De vraag is begrensd op 2.000 tekens. Alleen de veertien recentste berichten uit het gekozen gesprek worden aan DeepSeek meegegeven.

### Vragen stellen met de controller

1. Druk in **Ask** op **X** of kies **Quick**.
2. Kies met D-pad/stick en A een categorie: Navigation, Boss/combat, Item/build, Quest/NPC, Puzzle/mechanic of Settings/performance.
3. Kies de gewenste actie. Vragen die geen naam nodig hebben zijn direct klaar om te verzenden.
4. Als een specifiek onderwerp nodig is, typ je alleen een kort trefwoord zoals `Moonveil`, `Margit` of de naam van een quest.
5. Controleer de samengestelde vraag en kies **Send**.
6. Kies **Favorite** om de volledige vraag onder de actieve game te bewaren.
7. Gebruik de controller-follow-ups onder een antwoord voor een volgende stap, meer detail of een checklist.

Standaardbediening: D-pad/stick navigeert, A bevestigt, B gaat terug, L1/R1 wisselt tabs, X opent Quick Questions en Y opent trefwoordmodus. Touch en gewone tekstinvoer blijven daarnaast werken.

## 6. Gesprekken beheren

- Tik bovenaan op **New conversation** om met een lege context te beginnen. Er wordt pas een database-entry gemaakt wanneer je werkelijk een vraag verzendt.
- Tik op **Conversation history** om eerdere gesprekken van de actieve game te openen.
- Selecteer een gesprek op titel, hernoem het met het potlood of verwijder het na bevestiging.
- Het verwijderen van een gesprek verwijdert de berichten en chatcitations, maar reeds opgeslagen antwoorden blijven bestaan.

## 7. Game-Wiki gebruiken

Open **Wiki**.

1. Controleer de actieve game en de indicatie `ONLINE` of `OFFLINE`.
2. Zoek op item, boss, quest, locatie, mechanic of build.
3. Gevonden pagina's worden lokaal gecachet en verschijnen onder **Downloaded pages · available offline**.
4. Open **Open source** om de originele HTTPS-pagina in de browser te lezen.
5. Gebruik **Clear wiki cache** om alle gedownloade pagina's voor de actieve game te verwijderen.

Bij `NONE` en `MINIMAL` worden automatische previews verborgen, omdat een bronpagina spoilers kan bevatten. Open de externe bron dan bewust.

## 8. Saved, notities en checklists

Open **Saved** om drie soorten lokale informatie te beheren:

- **Saved answers** bewaren de oorspronkelijke vraag, het AI-antwoord en bronmetadata.
- **Notes** zijn vrije persoonlijke tekst voor de actieve game.
- **Checklists** bevatten offline afvinkbare stappen, items, collectibles, builds of boodschappen.

Alle data is aan de actieve game gekoppeld. Verwijder je het volledige gameprofiel, dan verwijdert Room de gekoppelde chat, tools en cache via foreign-key cascades.

## 9. Tweede scherm starten

Open **More → Displays**.

1. GameSide toont alle displays die Android rapporteert, inclusief resolutie, dichtheid, touch- en activity-capability.
2. Een geschikte niet-primaire display wordt als **RECOMMENDED** gemarkeerd.
3. Kies **Launch companion here** bij het onderste scherm.
4. De volledige Ask/Wiki/Saved/Games/More-interface opent in een aparte activity/task op dat display.
5. Als er geen tweede display is, gebruik je **Open single-screen companion**.

Display-ID's kunnen na reboot of reconnect veranderen; de app selecteert op capabilities en gebruikt geen vast Thor-displaynummer.

### Companion actief houden tijdens gamen

1. Open **More → Displays** en schakel via de knop Android Accessibility in voor **GameSide controller shortcut**.
2. Start de companion op het onderste display. Een nieuwe sessie zet **Keep companion active while gaming** standaard aan.
3. Start een game op het bovenste scherm. Als Thor de onderste activity verwijdert, vraagt GameSide na 750 ms een gecontroleerd herstel op het actuele onderste display aan.
4. Onder **Companion session** zie je Accessibility, sessiestatus, doel-display en herstelpogingen.
5. Gebruik **Restore companion now** voor handmatig herstel. Na drie automatische pogingen per minuut stopt de lus; de lange Menu-shortcut blijft een handmatige fallback.
6. Kies **Stop companion session** wanneer GameSide niet meer automatisch terug mag komen.
7. Kies bij een fout **Copy diagnostics** en deel alleen die tekst. Het log bevat maximaal vijftig lifecycle-/displayevents en nooit vragen, API-keys, Wiki-inhoud of schermdata.

Home of Recents opent GameSide niet geforceerd. Zonder Accessibility blijft de companion handmatig bruikbaar, maar is automatisch herstel na een gamestart niet beschikbaar.

### Globale lange Menu-shortcut

1. Open **More → Displays → Controller-first mode**.
2. Kies **Open Android Accessibility settings** en schakel **GameSide controller shortcut** in.
3. Ga terug en kies **Calibrate Menu button**.
4. Houd de gewenste Menu-knop ingedrukt. GameSide bewaart de echte Android-keycode van de Thor en schakelt de shortcut in.
5. Kies desgewenst 650, 800 of 1000 ms; 800 ms is de standaard.
6. Vanuit een game opent een lange druk GameSide. Een korte druk blijft naar de game gaan.

De Accessibility-service vraagt alleen controller-key filtering aan, leest geen scherminhoud en verbruikt geen microfoon. Zonder deze toestemming blijft controllerbediening binnen GameSide werken, maar kan de app niet vanuit een game met een globale knop worden geopend.

## 10. Een Android-game op het primaire scherm starten

1. Vul bij het gameprofiel de exacte package name van een geïnstalleerde Android-game in.
2. Open **Games** op de companion.
3. Tik op de startknop van die game.
4. GameSide vraagt Android de launcheractivity expliciet op display 0 te openen.

Voor een emulator-ROM, pc-stream of console-stream start je normaal de emulator/streamingapp zelf; de MVP kan geen individuele ROM binnen een emulator starten.

## 11. Back-up en import

Open **More → Privacy**.

### Exporteren

1. Kies **Export JSON**.
2. Selecteer een map en bestandsnaam in Androids documentkiezer.
3. Bewaar het bestand buiten de app voordat je de app verwijdert of van signing key wisselt.

De back-up bevat games, package-/Wiki-relaties, gesprekken, berichten, citations, opgeslagen antwoorden, notities, checklists, items en Quick Question-favorieten. De API-key en gedownloade Wiki-tekst ontbreken altijd.

### Importeren

1. Kies **Import JSON**.
2. Selecteer een geldige GameSide-back-up.
3. De app valideert alles voordat een Room-transactie begint.
4. Records worden op stabiele ID samengevoegd; overige lokale data wordt niet gewist.
5. Voer de API-key apart in op het nieuwe apparaat.

## 12. Privacy en data verwijderen

Onder **More → Privacy** zie je aantallen voor games, gesprekken, opgeslagen antwoorden, notities, checklists, Wiki-pagina's en de key-status.

Je kunt na bevestiging afzonderlijk verwijderen:

- alle gesprekken;
- alle persoonlijke tools;
- de Wiki-cache;
- de DeepSeek API-key.

**Delete all local data** wist de Room-database, DataStore-instellingen, encrypted preferences en de app-specifieke Android Keystore-key. Daarna begint onboarding opnieuw. Deze handeling kan niet ongedaan worden gemaakt.

## 13. Updates installeren

- Een nieuwere APK kan over de bestaande app worden geïnstalleerd als application ID, signing key en versionCode compatibel zijn.
- Maak vóór iedere belangrijke testupdate een JSON-back-up.
- Bij een signing-conflict moet de oude app worden verwijderd; daardoor wordt ook de encrypted API-key gewist.
- Bewaar voor echte privé-distributie altijd dezelfde langlevende releasekey. De huidige repository-APK gebruikt een Android-testcertificaat.

## 14. Problemen oplossen

Zie [troubleshooting.md](troubleshooting.md) voor de meest voorkomende fouten. Controleer bij problemen eerst:

1. staat de juiste game op **ACTIVE GAME**;
2. is de API-key opgeslagen en slaagt de verbindingstest;
3. is internet beschikbaar voor nieuwe AI- en Wiki-aanvragen;
4. heeft een handmatige Wiki-URL `https://` en ondersteunt de site MediaWiki;
5. rapporteert Android het onderste scherm onder **More → Displays**;
6. klopt de Android-package name exact wanneer game-launch mislukt.

Rapporteer nooit een API-key, volledige persoonlijke back-up of privé-chat in een bugmelding.
