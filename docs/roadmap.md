# GameSide AI roadmap

Deze roadmap beschrijft de geplande ontwikkelrichting na `1.1.0-controller`. Versienummers geven de gewenste volgorde aan, niet een vaste releasedatum. Een fase is pas klaar wanneer de bijbehorende releasecriteria aantoonbaar zijn geslaagd.

## Status

| Status | Betekenis |
|---|---|
| ✅ Gereed | Gebouwd, gedocumenteerd en beschikbaar in de repository |
| 🛠 Volgende | Hoogste ontwikkelprioriteit |
| 🧪 Gepland | Ontwerp staat vast, uitvoering volgt na de vorige releasegate |
| 💡 Later | Gewenste richting, nog niet ingepland |

## ✅ 1.1.0 — Controller-first test-build

Beschikbaar als huidige testversie:

- volledige Ask, game-Wiki, Saved, Games, display-, AI- en privacy-interface;
- DeepSeek-streaming met gamecontext, spoilerbeleid en klikbare bronnen;
- dedicated game-Wiki's via wiki.gg, Fandom en andere MediaWiki-bronnen;
- lokale gesprekken, notities, checklists, opgeslagen antwoorden en Wiki-cache;
- JSON-back-up zonder API-key of gedownloade Wiki-tekst;
- tweede-displaylaunch en single-screen fallback;
- controllerbediening met D-pad/stick, A/B, L1/R1, X en Y;
- Quick Questions, korte trefwoordinvoer, vervolgvragen en favorieten per game;
- optionele, gekalibreerde lange Menu-shortcut zonder schermuitlezing;
- Room-schema 6 en een direct downloadbare, test-key-signed APK.

## 🛠 1.1.1 — Stabiliteit en databetrouwbaarheid

Eerst worden de open punten uit de [bug-audit](bug-audit.md) afgehandeld:

- API-sleutelstatus tussen AI en Privacy vanuit één observeerbare bron synchroniseren;
- afgebroken AI-generatie zonder verweesde gebruikersvraag opslaan;
- back-upimport dedupliceren op `sourceMessageId`;
- Retry als echte regeneratie uitvoeren zonder dubbele vraag of oud antwoord als ongewenste context;
- duidelijke foutfeedback tonen wanneer Wiki-resultaten niet kunnen worden gedownload;
- onbekende geïmporteerde gamevelden en meerdere package-/Wiki-relaties behouden bij bewerken;
- generatie-state aan game en sessie koppelen zodat wisselen nooit tijdelijke cross-game inhoud toont;
- regressietests toevoegen voor ieder opgelost probleem.

Releasecriteria:

- nul open kritieke of hoge bugs;
- alle middelzware auditbevindingen opgelost en getest;
- schema-5→6-upgrade op een bestaand toestel uitgevoerd zonder verlies van games, chat, Saved-data of credential;
- debug, R8 release, lint, JVM- en instrumentatietests volledig groen.

## 🧪 1.2.0 — AYN Thor hardware-beta

Deze fase sluit de fysieke hardwaregate:

- beide Thor-displays en hun capabilities vastleggen;
- companion betrouwbaar op het onderste scherm openen en herstellen;
- controllerkeycodes en Xbox-/Nintendo-layout correct herkennen;
- focusnavigatie, zichtbare focusringen en scrollgedrag op ieder scherm nalopen;
- lange Menu-shortcut tijdens een echte game kalibreren en testen;
- korte Menu-druk, gamepauze en terugkeer naar de game controleren;
- game boven en GameSide onder gelijktijdig gebruiken;
- display-uit/aan, slaapstand, klepgedrag, rotatie, procesherstart en geheugenstress testen;
- remapping en een veilige fallback aanbieden als firmware een knop of background launch blokkeert.

Releasecriteria:

- volledige bediening zonder touch mogelijk voor de kernflow vraag → antwoord → vervolgactie;
- minimaal drie echte games getest, waaronder Android en emulatie/streaming;
- geen crash, taakverplaatsing of dataverlies tijdens de volledige Thor-testmatrix;
- alle resultaten toegevoegd aan [test-results.md](test-results.md).

## 🧪 1.3.0 — Snellere game-assistent

Na hardwarestabiliteit wordt de dagelijkse bediening verfijnd:

- Quick Question-favorieten hernoemen, herschikken en dupliceren tussen games;
- recente trefwoorden en lokale Wiki-pagina's als controller-vriendelijke suggesties tonen;
- configureerbare controllerbindings en knopglyphs per controllerlayout;
- compact command-menu voor game wisselen, nieuw gesprek, geschiedenis, Wiki, Saved en checklists;
- antwoordweergave verbeteren met nette Markdown, compacte bronkaarten en inklapbare details;
- kosteninzicht op basis van gerapporteerde tokenaantallen, zonder veranderlijke prijsclaims;
- offline status en herprobeergedrag voor AI en Wiki duidelijker maken.

## 💡 2.0 — Stabiele privérelease

Voor een niet-testrelease zijn gepland:

- duurzame privé-releasekey met herstelprocedure en twee beveiligde back-ups;
- reproduceerbare releaseworkflow met automatische build-, lint-, test-, signing- en checksumcontrole;
- GitHub Releases met versiehistorie, changelog en één duidelijke actuele APK;
- migratietests vanaf ieder bewaard Room-schema naar de nieuwste versie;
- volledige Compose end-to-end-tests voor touch én controller;
- privacyvriendelijke diagnostiek die alleen na een bewuste exportactie wordt gedeeld;
- toegankelijkheidscontrole voor focus, contrast, tekstschaling en controller-only gebruik;
- definitieve gebruikershandleiding en releasechecklist voor normale installatie en updates.

## 💡 Mogelijke latere uitbreidingen

- meerdere AI-providers achter dezelfde beveiligde providerinterface;
- uitgebreidere lokale gameprofielen met progressie, builds en eigen context;
- optionele import/export van Quick Question-pakketten;
- game-specifieke templatepacks zonder een hardcoded gamelijst;
- betere offline zoekfunctie over reeds gedownloade game-Wiki-pagina's;
- tablet-, tv- en andere dual-screenlayouts zodra de Thor-versie stabiel is.

## Bewust niet gepland

De huidige productrichting blijft controller-first en volledig op de Thor. Daarom staan deze functies niet op de actieve roadmap:

- microfoon- of spraakbediening;
- een telefoon als verplichte remote;
- continu scherm opnemen, meeluisteren of achtergrondanalyse;
- advertenties, tracking of automatische analytics;
- een publieke Play Store-release voordat de privérelease en Thor-hardwaregate stabiel zijn.

## Prioriteitsregel

Betrouwbaarheid gaat vóór nieuwe functies. Nieuwe mogelijkheden schuiven niet vóór een open dataverlies-, privacy-, update- of hardwareprobleem. Iedere release werkt de handleiding, testresultaten, APK-checksum en bekende beperkingen tegelijk bij.
