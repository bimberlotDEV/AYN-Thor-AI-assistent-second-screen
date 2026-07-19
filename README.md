# GameSide AI

GameSide AI is een privé Android gaming-assistent voor de AYN Thor en andere Android-apparaten. Je speelt op het hoofdscherm en gebruikt **Ask, Wiki, opgeslagen antwoorden, notities en checklists** op het tweede scherm. Op een gewone telefoon werkt dezelfde app in single-screenmodus.

> Huidige versie: **1.1.2 Lower-screen test-build**<br>
> Getest op: Huawei P30 Lite, Android 9 en Androids gesimuleerde tweede display<br>
> Fysieke AYN Thor-validatie: automatische start beneden gereed; gelijktijdig GameSide beneden en game boven wordt door de Thor-firmware beëindigd

## Downloaden

De repository is privé. Log daarom op de AYN Thor eerst in op GitHub met een account dat toegang heeft tot deze repository.

### [Download GameSide AI 1.1.2 Lower-screen APK](https://github.com/bimberlotDEV/AYN-Thor-AI-assistent-second-screen/raw/refs/heads/main/downloads/GameSideAI-1.1.2-lower-screen-test.apk)

Bestand: `GameSideAI-1.1.2-lower-screen-test.apk`<br>
SHA-256: `8AFDC3C0AF3D428CA6706CEFF938E05D778D42636624891657BEA7CCC3ADFF11`

Installeren zonder pc of USB:

1. Open deze repository op de Thor en log in op GitHub.
2. Tik op de downloadlink hierboven.
3. Bevestig de APK-download in Chrome.
4. Open het gedownloade bestand.
5. Sta tijdelijk **Install unknown apps / Onbekende apps installeren** toe voor Chrome of Bestanden.
6. Tik op **Installeren** en open daarna GameSide AI.
7. Zet de toestemming voor onbekende apps desgewenst weer uit.

Android kan bij een handmatig gedownloade APK een waarschuwing tonen. Installeer alleen het bestand uit deze eigen privé-repository en controleer bij twijfel de SHA-256.

## Eerste keer gebruiken

1. Doorloop de korte privacy-onboarding.
2. Open **More → AI**.
3. Vul je eigen DeepSeek API-key in en kies **Encrypt & save**.
4. Gebruik **Test DeepSeek connection** om de verbinding te controleren.
5. Open **Games → +** en voeg een game toe.
6. Kies platform en spoilerstand. Een Wiki-URL en Android-package zijn optioneel.
7. Tik op de gamekaart zodat **ACTIVE GAME** verschijnt.
8. Open **Ask**, stel een vraag en controleer de getoonde bronnen.

Een API-key is nooit in de APK of repository opgenomen. Iedere gebruiker voert zijn eigen key rechtstreeks op het apparaat in.

## Wat kun je met de app?

| Onderdeel | Mogelijkheden |
|---|---|
| **Ask** | Gamegerichte vragen typen óf controller-first samenstellen, favorieten en vervolgvragen kiezen, streaming antwoorden stoppen, bronnen openen, antwoorden kopiëren, opnieuw proberen, opslaan of omzetten naar een checklist. |
| **Wiki** | De Wiki van de actieve game doorzoeken, bronpagina's openen en eerder opgehaalde pagina's offline bekijken. |
| **Saved** | Opgeslagen AI-antwoorden, persoonlijke notities en offline checklists beheren. |
| **Games** | Games toevoegen, zoeken, activeren, pinnen, bewerken, verwijderen en optioneel een gemapte Android-game op het primaire scherm starten. |
| **More → Displays** | Controleren welk onderste display automatisch wordt gebruikt en controllerbediening plus de globale Menu-shortcut configureren. |
| **More → AI** | API-key beheren, DeepSeek-model kiezen, verbinding testen en antwoordlengte/kosten begrenzen. |
| **More → Privacy** | Lokale data tellen, categorieën wissen, API-key verwijderen, back-up exporteren/importeren of de app volledig resetten. |

## Slimme gamehulp

- De actieve game wordt automatisch aan iedere vraag toegevoegd; je hoeft de titel niet steeds te herhalen.
- GameSide zoekt gespecialiseerde game-Wiki's zoals wiki.gg en Fandom, niet de algemene Wikipedia.
- Antwoorden met bewijs tonen genummerde, klikbare bronnen.
- Als geen bruikbare bron beschikbaar is, wordt het antwoord herkenbaar gebaseerd op algemene modelkennis.
- Per game kun je kiezen uit `NONE`, `MINIMAL`, `MODERATE` en `FULL` spoilers.
- Strenge spoilerstanden beperken bronfragmenten en verbergen Wiki-previews totdat je bewust de bron opent.
- Alleen de veertien recentste chatberichten worden als context verzonden.
- Met 512, 900 of 1500 maximale outputtokens bepaal je de balans tussen detail, snelheid en API-verbruik.

## Controller-first gebruiken

- D-pad of stick navigeert; **A** bevestigt en **B** gaat terug.
- **L1/R1** wisselt tussen Ask, Wiki, Saved, Games en More.
- **X** opent Quick Questions en **Y** opent dezelfde composer direct in trefwoordmodus.
- Kies een categorie en actie om zonder typen een complete spoilerbewuste vraag te maken.
- Voeg optioneel alleen een korte naam toe, bijvoorbeeld `Moonveil`, en bewaar samengestelde vragen als favoriet per game.
- Open **More → Displays → Controller-first mode** om de globale shortcut in te stellen. Android Accessibility hoeft alleen controllerknoppen te observeren en leest geen scherminhoud.
- Na kalibratie opent een lange druk op de gekozen Menu-knop GameSide op het tweede scherm. De korte druk blijft beschikbaar voor de game.

## Tweede scherm op de AYN Thor

1. Open GameSide AI vanaf eender welk Thor-scherm.
2. Als Android het onderste display beschikbaar stelt, verplaatst GameSide de volledige interface daar automatisch naartoe.
3. Ga naar **More → Displays** om te controleren welk display als **AUTOMATIC TARGET** wordt herkend.
4. Gebruik Ask, Wiki, Saved, Games en More op het onderste scherm.
5. De optionele lange Menu-shortcut opent GameSide eveneens direct beneden.

Als Android geen bruikbaar tweede scherm rapporteert, blijft GameSide automatisch op het huidige scherm. Display-ID's zijn niet hardcoded en mogen na een herstart veranderen.

## Offline, back-up en privacy

- Notities, checklists, opgeslagen antwoorden en eerder gedownloade Wiki-pagina's werken lokaal.
- Chatgeneratie en nieuwe Wiki-resultaten hebben internet nodig.
- De API-key wordt AES/GCM-versleuteld met Android Keystore.
- De app heeft geen advertenties, analytics, microfoon- of screenshottoegang. Alleen de optionele controller-shortcutservice kan op de achtergrond gamepadknoppen observeren; scherminhoud wordt niet gelezen.
- Via **More → Privacy → Export JSON** maak je een overdraagbare back-up van games, gesprekken, bronnen en persoonlijke tools.
- API-keys en gedownloade Wiki-tekst worden nooit geëxporteerd.
- Import controleert eerst de bestandsversie, grootte, waarden, HTTPS-links en onderlinge verwijzingen.

## Volledige handleiding

Voor uitleg per scherm, voorbeelden en probleemoplossing:

### [Open de volledige gebruikershandleiding](docs/gebruikershandleiding.md)

## Roadmap

Bekijk wat al gereed is, welke bugs eerst worden opgelost, hoe de fysieke AYN Thor-beta wordt gevalideerd en wat richting een stabiele privérelease gaat:

### [Open de GameSide AI roadmap](docs/roadmap.md)

## Documentatie

- [Volledige gebruikershandleiding](docs/gebruikershandleiding.md)
- [DeepSeek instellen](docs/ai-provider-setup.md)
- [Privacy en beveiliging](docs/privacy-security.md)
- [Probleemoplossing en bekende beperkingen](docs/troubleshooting.md)
- [AYN Thor en toesteltesten](docs/device-testing.md)
- [Architectuur](docs/architecture.md)
- [Vastgelegde testresultaten](docs/test-results.md)
- [Privé release en distributie](docs/release-and-distribution.md)
- [Releasechecklist](docs/release-checklist.md)
- [Bug-audit](docs/bug-audit.md)
- [Roadmap](docs/roadmap.md)

## Ontwikkelaars

Vereisten: JDK 17, Android SDK 36 en Android build-tools 36.x.

```powershell
./gradlew.bat test lintDebug assembleDebug assembleRelease
```

De debug-APK verschijnt in `app/build/outputs/apk/debug/app-debug.apk`. Release-signing gebruikt uitsluitend externe Gradle-properties of omgevingsvariabelen; zie [release-and-distribution.md](docs/release-and-distribution.md). De application ID is standaard `com.gameside.ai` en kan met `-PGAME_SIDE_APPLICATION_ID=...` worden aangepast.

## Belangrijkste huidige beperking

De Thor-firmware verwijdert GameSide van het onderste scherm wanneer een game op het bovenste scherm start. De automatische herstelfunctie is daarom uit het product verwijderd. Versie 1.1.2 opent GameSide altijd direct beneden wanneer het onderste display beschikbaar is, maar kan deze firmwarebeperking tijdens een actieve game niet omzeilen. GameSide boven met een game beneden werkt volgens de huidige gebruikerstest wel.
