# MacroKit – Macros, Menü und Xray für Minecraft 26.2 (Fabric, nur Client)

Reine Client-Mod: Auf dem Server muss nichts installiert werden. Nur auf deinem eigenen Server benutzen,
auf fremden Servern sind Xray, Criticals und AutoTotem in der Regel verboten.

## .jar bauen

Eine fertige `.jar` liegt nicht bei, weil beim Erstellen keine Verbindung zu den Minecraft-Servern möglich war.
Du brauchst **JDK 25**. Dann eine von drei Möglichkeiten:

1. **IntelliJ IDEA**: Ordner öffnen, Gradle-Import abwarten, rechts im Gradle-Fenster `Tasks › build › build`.
2. **Kommandozeile**: `gradle build` (Gradle ≥ 9, aktuelle Version von https://gradle.org). Wer lieber den Wrapper
   will: `gradle wrapper --gradle-version 9.5.1`, danach `./gradlew build`.
3. **GitHub**: Projekt in ein neues Repository hochladen. Der Workflow `.github/workflows/build.yml` baut die Mod bei
   jedem Push. Die fertige Datei steht danach unter *Actions › Lauf › Artifacts › macrokit-jar*.

Ergebnis: `build/libs/macrokit-1.0.0.jar` (nicht die `-sources.jar`). In den `mods`-Ordner legen, dazu
**Fabric Loader ≥ 0.19.3** und **Fabric API für 26.2**. Zum Ausprobieren ohne Installation: `gradle runClient`.

Versionen stehen in `gradle.properties`, aktuelle Werte: https://fabricmc.net/develop

## Bedienung

| Taste (änderbar unter Optionen › Steuerung › MacroKit) | Funktion |
|---|---|
| **Rechte Umschalttaste** | Menü öffnen |
| F6 | Xray an/aus |
| F7 | Aufnahme starten/beenden |
| F8 | Zuletzt benutztes Macro nochmal abspielen |
| F9 | Notbremse: Macro stoppen, Tasten loslassen |
| *(unbelegt)* | Fast Right Click an/aus (Taste selbst wählen, siehe unten) |
| *(unbelegt)* | Criticals an/aus |
| *(unbelegt)* | AutoTotem an/aus |
| *(unbelegt)* | Macro-Slot 1–9 (mit `/macro bind <1-9> <name>` zuweisen) |

### Menü
- **Macros**: Liste mit Start, **Taste setzen**, Loop (endlos) und Löschen (zweimal klicken), Aufnahme starten/beenden, Macro stoppen.
- **Xray**: an/aus, Modus, 8 Erze einzeln (Klick schaltet Aus → Deepslate → Stein → Beides), Reichweite (1–32 Chunks, Standard 6; Klick auf die Zahl springt auf 4, 8, 12, 16, 24, 32), Prüfen und kalibrieren.
- **Fast Right Click**: Schalter an/aus und **Klicks** (Zahl der Klicks pro Klick: 1, 2, 4, … 64).
- **Criticals**: Schalter an/aus.
- **AutoTotem**: Schalter an/aus und **Pause** (Ticks, bevor ein fehlender Totem ersetzt wird: 0, 1, 2, 3, 5, 10, 20).
- **Freunde**: Liste, Anfragen, Freigaben pro Freund (siehe unten).

### Macro direkt mit einer Taste starten
1. Menü öffnen (rechte Umschalttaste) › **Macros**.
2. Beim Macro auf **Taste setzen** klicken und dann die gewünschte Taste drücken. Die Taste steht danach im Knopf.
3. Im Spiel startet diese Taste das Macro sofort, ohne Menü und ohne `/macro run`.

- Dieselbe Taste noch einmal drücken **stoppt** das laufende Macro (praktisch bei Loops).
- Eine Taste gehört nur einem Macro. Wählst du eine belegte Taste, wandert sie zum neuen Macro.
- Entfernen: Beim Taste-Setzen **Entf** oder **Rück** drücken, oder `/macro unkey <name>`. Übersicht: `/macro keys`.
- Nicht möglich: Leertaste, Enter, Tab, Esc, Umschalt/Strg/Alt. Maustasten und Tastenkombinationen gehen nicht.
- Die Taste behält ihre normale Funktion (z. B. W läuft weiter). Nimm deshalb eine Taste, die sonst nichts tut,
  zum Beispiel F10, F12, Einfg, Pos1, Ende, Bild hoch/runter oder den Zahlenblock.
- Im Chat, im Inventar und in anderen Menüs lösen die Tasten nichts aus. Während einer Aufnahme auch nicht.
- Die Tasten werden in `config/macrokit.json` gespeichert. Die Slots 1–9 aus der Tabelle oben gehen weiterhin.

### Fast Right Click (Klick-Salve und Dauerklick)
Ein einzelner Rechtsklick löst mehrere Klicks im selben Tick aus (Standard **32**). Es klickt **nichts von allein**:
ohne Klick passiert nichts, und eine gehaltene Taste wiederholt nur so schnell wie im normalen Spiel.

1. Taste festlegen: Optionen › Steuerung › MacroKit › **Fast Right Click umschalten**. Dort geht jede beliebige Taste
   (sie ist absichtlich unbelegt, damit nichts doppelt belegt ist).
2. Im Spiel die Taste drücken: Im Chat kommt „Fast Right Click: AN (32 Klicks pro Klick)“, oben links steht
   „Fast Right Click x32“.
3. Jeder neue Druck der rechten Maustaste (Benutzen-Taste) macht dann 32 Klicks: einen macht das Spiel, 31 die Mod.
- **Dauerklick (Halten):** Hältst du die rechte Maustaste länger als eine Viertelsekunde (5 Ticks), klickt es **200 Mal
  pro Tick** (4000 pro Sekunde), bis du loslässt. Ein normaler kurzer Klick bleibt eine Salve von 32.
  Zusätzlich gibt es unter Optionen › Steuerung › MacroKit die Belegung **Dauerklick (Taste halten)**: Dort kannst du
  **jede Taste oder Maustaste** wählen. Solange du sie hältst, klickt es sofort mit 200 pro Tick, auch wenn der Schalter
  aus ist. Auch das Dauerklick-Tempo ist einstellbar (Menü: Halten, oder `/macro fastclick hold <0-200>`, 0 = aus).
- Alternativ im Menü (Schalter, **Klicks** und **Halten**) oder per Befehl: `/macro fastclick on|off|toggle`,
  `/macro fastclick count <1-64>` und `/macro fastclick hold <0-200>`.
- Die Zahl wird in `config/macrokit.json` gespeichert. An/Aus **nicht**: nach einem Neustart ist es immer aus.
- Es wirkt nur im Spiel ohne offenen Bildschirm. Während ein Macro abgespielt wird, gibt es keine Salven.
- Mehrere Klicks im selben Tick bringen bei Blöcken nur etwas, wenn der Block danach noch anvisiert werden kann:
  Knöpfe, Hebel, Händler, Werfen, Essen, Behälter und Ähnliches. Ein Block wird nur einmal gesetzt.
- Jeder Klick ist ein Paket zum Server. 200 Klicks pro Tick sind 4000 Pakete pro Sekunde: Die meisten Server (auch ein
  eigener mit Paper) werfen dich dabei wegen zu vieler Pakete raus oder laufen ins Lag. Dann `hold` senken, z. B. auf 20.
  Öffnet ein Klick einen Bildschirm (Kiste, Händler), endet die Serie sofort. Fremde Server können das auch als Cheat erkennen und dich sperren.
- Technisch: Die Mod ruft die Benutzen-Methode des Spiels (`Minecraft.startUseItem`) zusätzlich auf. Findet sie die
  Methode nicht, steht eine Meldung im Chat und Fast Right Click bleibt aus.

### Criticals
Jeder Schlag auf ein Lebewesen wird zum kritischen Treffer (mehr Schaden, wie ein Schlag im Fallen).

1. Taste festlegen: Optionen › Steuerung › MacroKit › **Criticals umschalten** (jede beliebige Taste), oder im Menü,
   oder `/macro criticals on|off|toggle`. Oben links steht dann „Criticals“. An/Aus wird nicht gespeichert.
2. Kurz vor jedem Schlag sendet die Mod zwei kleine Positionspakete (0,0625 Blöcke nach oben, dann zurück, beide
   „nicht am Boden“). Der Server hält dich dadurch für fallend, und die normale Regel für kritische Treffer greift.
- Es passiert nur etwas, wenn du am Boden stehst, nicht im Wasser oder in Lava, nicht an einer Leiter, nicht auf einem
  Reittier und **nicht sprintest**: Beim Sprinten gibt es nach den Spielregeln keinen kritischen Treffer.
  Der Schlag muss außerdem ein Lebewesen treffen, und der Angriffsbalken sollte voll sein (sonst ist der Crit schwach).
- **Anti-Cheat:** Diese Pakete sind genau das, was Anti-Cheat-Plugins als Criticals-Hack erkennen. Auf einem Server mit
  Anti-Cheat ist mit Warnungen, Rückschlägen oder einem Kick zu rechnen. Die Mod versucht nichts zu verstecken.
- Technisch: Die Mod hängt sich über die Fabric API (`AttackEntityCallback`) an jeden Angriff. Das Positionspaket wird
  per Reflection gebaut. Findet sie es nicht, steht eine Meldung im Chat und Criticals bleibt aus.

### AutoTotem
Hält einen **Totem der Unsterblichkeit** in der Zweithand. Fehlt er (zum Beispiel nach dem Aufbrauchen), tauscht die Mod
einen Totem aus dem Inventar oder der Schnellleiste in die Zweithand. Das ist derselbe Klick wie im Inventar mit
der Maus über dem Slot und F.

1. Taste festlegen: Optionen › Steuerung › MacroKit › **AutoTotem umschalten**, oder im Menü, oder
   `/macro autototem on|off|toggle`. Oben links steht „AutoTotem: N Totems“ (rot: „keine Totems!“).
2. **Pause**: so viele Ticks wartet die Mod, nachdem die Zweithand leer ist (Menü oder `/macro autototem delay <0-20>`,
   Standard 0 = sofort). Die Pause wird in `config/macrokit.json` gespeichert, an/aus nicht.
- Totems in der Schnellleiste gehen auch. Das Hauptinventar kommt vor der Schnellleiste. Bei offenem Bildschirm
  (Inventar, Chat, Menü) tut die Mod nichts.
- Lehnt der Server den Tausch ab, versucht sie es frühestens nach 5 Ticks wieder, es gibt also keinen Paket-Regen.
- Auf Servern mit Anti-Cheat können Inventar-Klicks im Spiel auffallen. Auch das versteckt die Mod nicht.
- Technisch: Die Klick-Methode des Spiels wird per Reflection gesucht (Methode mit fünf Parametern, einer davon ein
  Enum mit dem Wert `SWAP`). Findet sie sie nicht, steht eine Meldung im Chat und AutoTotem bleibt aus.

### Freunde (Anfrage, Freigaben, Marker, Leuchten)
Freunde sind andere Spieler **mit der Mod**. Alles ist freiwillig, gegenseitig und standardmäßig aus.

1. **Anfrage:** Menü › **Freunde** › **Freund hinzufügen** (Liste der Spieler auf dem Server) oder
   `/macro friend add <name>`. Der andere sieht im Chat „X möchte dein Freund sein“ und nimmt an mit
   `/macro friend accept <name>` oder im Menü (Annehmen / Ablehnen). Erst dann sind es Freunde.
2. **Was du ihm erlaubst:** Menü › Freunde › Name: **Leuchten**, **Koordinaten**, **Dimension** (je an/aus). Standard ist
   nichts. Es gilt sofort, und du kannst es jederzeit zurücknehmen. Befehl:
   `/macro friend share <name> glow|coords|dim|all on|off`.
3. **Was er dir erlaubt:** Das bestimmt er selbst. Du siehst es im selben Bildschirm („X erlaubt dir: …“) und kannst
   davon nur anzeigen, was er freigegeben hat. Du kannst es zusätzlich bei dir ausschalten: **Leuchten zeigen** und
   **Marker zeigen** (`/macro friend show <name> glow|marker on|off`).

Was du bei einem Freund siehst, wenn er es freigibt:
- **Koordinaten und Dimension:** Liste oben rechts (Name, x y z, Oberwelt/Nether/Ende, Entfernung). Ausschalten:
  Menü › Freunde › Liste oder `/macro friend hud on|off`.
- **Marker durch Wände:** Ein Punkt mit Name und Entfernung auf dem Bildschirm, auch hinter Wänden. Dafür braucht es
  Koordinaten **und** Dimension, und ihr müsst in derselben Dimension sein.
- **Leuchten durch Wände:** Der Freund bekommt den Umriss, den das Spiel sonst bei Leuchten-Effekten zeichnet. Das geht
  nur, wenn er bei dir im geladenen Bereich ist (Sichtweite des Servers).

Wie es funktioniert und was dabei zu beachten ist:
- Eine reine Client-Mod kann dem Server nichts beibringen. Deshalb schicken sich die Mods private Nachrichten
  (`/msg <name> [MK1|...]`). Die Mod des Empfängers erkennt sie und blendet sie im Chat aus. Wer die Server-Logs
  liest (der Betreiber), sieht diese Nachrichten samt Koordinaten. Ein Spieler ohne die Mod sieht eine seltsame
  Flüsternachricht, bei der Anfrage mit einem lesbaren Hinweis.
- Damit der Server nicht wegen Spam wirft, geht höchstens alle 25 Ticks (1,25 Sekunden) eine Nachricht raus. Positionen
  werden alle 5 Sekunden gemeldet, aber nur wenn du dich mehr als 2 Blöcke bewegt hast oder die Dimension wechselst.
  Viele Freunde mit Koordinaten-Freigabe verzögern sich dadurch gegenseitig.
- Es wird nichts an jemanden gesendet, dem du es nicht freigegeben hast. Entfernst du einen Freund oder lehnst ab, geht
  ihm eine Nachricht zu, wenn er online ist. Ist er offline, merkt er es erst, wenn er wieder da ist: Schickt seine Mod dann
  etwas, antwortet deine einmal mit „entfernt“.
- **Ehrlich gesagt:** Die Mod deines Freundes hält sich an deine Freigaben, technisch erzwingen kann das niemand. Jemand
  mit einer veränderten Mod könnte mehr anzeigen. Gib Koordinaten nur Leuten, denen du traust.
- Die Absender werden geprüft (Name steht in der Chat-Zeile des Servers), Fälschungen werden ausgeblendet und ignoriert.
  Die Zuordnung läuft über den Spielernamen: Benennt sich ein Freund um, ist er ein neuer Eintrag.
- **Anfragen: AUS** (Menü › Freunde oder `/macro friend requests on|off`) ignoriert neue Anfragen. Es werden höchstens
  10 offene Anfragen gemerkt.
- Gespeichert wird in `config/macrokit-friends.json`. Wer online ist und die Positionen werden nicht gespeichert.
- Geht nicht, wenn der Server `/msg` sperrt, wenn die Spielerliste nicht lesbar ist (dann kommt eine Meldung im Chat,
  Anfragen gehen trotzdem raus) oder wenn der Chat-Empfang auf der Version nicht angemeldet werden kann
  (Hinweis im Freunde-Menü).

### Macro-Befehle (`/macro ...`, laufen nur lokal)
`add <name> <text>`, `run <name> [anzahl]`, `loop <name>`, `stop`, `record start <name>` / `record stop` / `record cancel`,
`list`, `info`, `delete`, `rename`, `keys`, `unkey <name>`, `bind`, `unbind`, `slots`, `delay <ticks>`, `status`, `reload`,
`fastclick on|off|toggle|count <1-64>|hold <0-200>`, `criticals on|off|toggle`,
`autototem on|off|toggle|delay <0-20>`,
`friend add|accept|decline|remove <name>`, `friend list`, `friend share <name> glow|coords|dim|all on|off`,
`friend show <name> glow|marker on|off`, `friend requests on|off`, `friend hud on|off`.

Text-Macros: Zeilen mit `;;` trennen, `/` am Anfang = Befehl, `@wait 40` / `@wait 2s` / `@wait 500ms` = Pause.
Beispiel: `/macro add heim /home ;; @wait 2s ;; Bin wieder da!`

Aufnahmen spielen **Eingaben** ab (Laufen, Springen, Schleichen, Sprinten, Angreifen/Benutzen, Blickrichtung,
Hotbar-Slot, Chat/Befehle), keine Positionen: an derselben Stelle starten wie bei der Aufnahme.
Vanilla-Server werfen Spieler bei zu schnellem Chat-Spam raus, bei langen Text-Macros `/macro delay 20` nutzen.

## Xray (alle Erze der Oberwelt)

Erze: Kohle, Eisen, Kupfer, Gold, Redstone, Lapis, Diamant, Smaragd. Jedes Erz hat im Menü einen eigenen Knopf,
ein Klick schaltet weiter: **Aus → Deepslate → Stein → Beides**. Standard: Kohle, Redstone, Diamant und Smaragd
als Deepslate, die anderen aus. Die Auswahl wird gespeichert.

**Tracer:** Von der Bildschirmmitte laufen Linien in der Farbe des Erzes zu den nächsten 3 Erzen (Menü: Knopf
„Tracer“ schaltet 1–5 oder aus, Befehl `/macro xray tracer <0-5>`). Liegt ein Erz hinter oder neben dir, zeigt die Linie
zum Bildrand in die Richtung, in die du dich drehen musst. Gezählt werden nur die Erze, die du im Menü eingeschaltet hast.

Die Erze erscheinen als farbige Punkte (Kohle grau, Eisen beige, Kupfer orange, Gold gelb, Redstone rot,
Lapis blau, Diamant cyan, Smaragd grün) mit Entfernung bei den nächsten, auch durch Wände.
Reichweite: Quadrat von 6 Chunks um dich (13 × 13 Chunks), einstellbar von 1 bis 32 (32 = größte Sichtweite im
Spiel). Beide Modi arbeiten nur mit Chunks, die dein Spiel geladen hat: Schickt der Server nur 4 Chunks, gibt es auch
nur dort Markierungen, egal wie hoch die Reichweite steht. Ab etwa 16 Chunks brauchen beide Modi spürbar mehr
Rechenzeit und Arbeitsspeicher, die Liste wird dann seltener neu berechnet. Es werden höchstens
1500 Punkte gezeichnet, die nächsten zuerst (`maxMarkers` in der Einstellungsdatei).
„Deepslate“ zeigt die Erze im Deepslate (unter Y=0, in der Übergangszone Y 0–7 nur wo wirklich Deepslate liegt),
„Stein“ die Erze im normalen Stein (ab Y=0 aufwärts).
Kohle gibt es als Deepslate-Variante praktisch nur ganz knapp unter Y=0, dort sieht man sie selten.

**Modus Seed (Standard)**: rechnet die Erzgenerierung aus dem Weltseed nach, wie OreSim/Seed-Xray.
Der Seed steht auf `6608149111735331168` (ändern: `/macro xray seed <zahl>`). Er funktioniert auch, wenn der
Server Anti-Xray benutzt. Die Daten (Biome, Erzregeln) liest die Mod aus dem Spiel selbst, nichts ist fest eingebaut.
Die Berechnung schaut dabei in die echte Welt (geladene Chunks), so wie das Spiel beim Erzeugen: Eine Ader verbraucht
Zufallszahlen nur dort, wo wirklich Gestein liegt, und Adern, die ganz über dem Gelände liegen, werden gar nicht gesetzt.
Ohne das stimmt nur die erste Ader eines Chunks, alle weiteren liegen falsch. Chunks werden neu berechnet, sobald mehr
Nachbar-Chunks geladen sind (die Adern reichen über den Chunkrand).
Jede berechnete Stelle wird außerdem mit dem echten Block verglichen: Luft, Wasser, Lava und fremde Blöcke werden
ausgeblendet, ebenso Stellen, an denen das Spiel das Erz wegen angrenzender Luft verworfen hätte.
Genauigkeit: Höhlen und andere Erze, die eine Ader in der echten Welt verändert haben, kennt der Seed nicht.
Deshalb gibt es vereinzelt Markierungen ohne Erz, besonders neben Höhlen.

**Modus Scan**: sucht die geladenen Chunks nach echten Erzen ab. Immer korrekt, geht aber nicht
durch Anti-Xray und kennt nur, was der Server schickt. Umschalten im Menü oder `/macro xray mode scan`.

**Prüfen und kalibrieren** (Menü oder `/macro xray verify`): vergleicht die berechneten Positionen mit den echten
Blöcken in den geladenen Chunks (dafür darf der Server kein Anti-Xray haben) und gleicht die Reihenfolge der
Features automatisch an. Die Prüfung läuft über einige Sekunden im Hintergrund, das Ergebnis steht im Chat
(eine Zeile je Erz). Das ist der Test, ob die Berechnung für deine Welt stimmt. Erwartet werden bei den
Erzen `passt` mit grob 50–95 %. Steht überall `keine Übereinstimmung`, stimmt der Seed nicht, oder der Server
verschleiert die Erze, oder die Daten von 26.2 sehen anders aus. Dann den Scan-Modus nehmen.

**Diagnose** (`/macro xray debug`): schreibt `config/macrokit-xray-debug.txt` mit den gelesenen Worldgen-Daten, der
Erz-Reihenfolge, dem Biom an deiner Position und einem Vergleich der berechneten Positionen mit den echten Blöcken im
Chunk, in dem du stehst. Im Chat steht pro Erz „N berechnet, M davon echtes Erz“. Das ist der schnellste Test, ob der
Seed stimmt: Im Singleplayer (kein Anti-Xray) sollte M bei den meisten Erzen nahe bei N liegen. Den Seed deiner Welt
zeigt `/seed`, setzen mit `/macro xray seed <zahl>`.

Weitere Befehle: `/macro xray on|off|range <1-32>|tracer <0-5>|mode seed|scan|verify|debug|status` und
`/macro xray ore <coal|iron|copper|gold|redstone|lapis|diamond|emerald|all> <off|deepslate|stone|both>`,
zum Beispiel `/macro xray ore iron both`.
Einstellungen: `config/macrokit-xray.json`.

Grenzen der Anzeige: Die Punkte werden aus Kamera, Blickrichtung und FOV auf den Bildschirm gerechnet.
Beim Sprinten oder mit Trankeffekten ändert sich das FOV und die Punkte rutschen leicht. In der 3rd-Person-Ansicht
passen sie nicht. Ob die Punkte mit F1 (Oberfläche ausblenden) verschwinden, ist ungetestet; sonst Xray mit F6 ausschalten.
Der Xray gilt nur in der Oberwelt, nicht im Nether oder im Ende.

## Falls der Build Fehler meldet

Das Projekt baut mit Minecraft 26.2 (per GitHub Actions geprüft). Die Rechenlogik des Erzfinders (Zufallszahlen,
Aderform, Reihenfolge) lief getrennt von Minecraft und ergibt plausible Mengen, ist aber nicht mit einer echten Welt
verglichen. Falls der Build nach einem Update von Minecraft oder Fabric API doch Fehler meldet, nennt die
Fehlermeldung die Stelle (Datei und Zeile). Wahrscheinlichste Stellen:

- `InputBits.java`: `KeyMapping.setDown(boolean)`, Felder `Options.keyUp` … `keyUse`
- `Recorder.java`, `MacroPlayer.java`: `Inventory.getSelectedSlot()` / `setSelectedSlot(int)`, `connection.sendChat/sendCommand`
- `MacroKitClient.java`: Paket von `ClientSendMessageEvents`
- `XrayHud.java`: Pakete von `HudElementRegistry` / `VanillaHudElements`, `getEyePosition`
- `FastClick.java`: die private Methode `Minecraft.startUseItem()` (wird per Reflection gesucht, Alternativname `doItemUse`)
- `Criticals.java`: `AttackEntityCallback` (Fabric API), `Entity.getX/getY/getZ`, `ClientPacketListener.send(Packet)`; Konstruktor des Pakets `ServerboundMovePlayerPacket$Pos` wird per Reflection gesucht
- `AutoTotem.java`: `Inventory.getItem(int)`, `LivingEntity.getOffhandItem()`, `ItemStack.is(Item)`, `Items.TOTEM_OF_UNDYING`; der Inventar-Klick (`MultiPlayerGameMode.handleInventoryMouseClick` oder neuerer Name) wird per Reflection gesucht
- `FriendNet.java`, `FriendsHud.java`, `Friends*.java`: `ClientReceiveMessageEvents` (Fabric API, per Reflection angemeldet), `ClientLevel.players()`, `Entity.getName()/getId()`, `LocalPlayer.connection.sendCommand`; die Spielerliste (`getOnlinePlayers`) und das Leuchten (`Entity.setSharedFlag`) werden per Reflection gesucht
- `Hotkeys.java`: Pakete von `HudElementRegistry` / `VanillaHudElements`; Fenster und GLFW werden per Reflection gefunden (`Window.handle()`), gehen die Tasten nicht, steht eine Meldung im Chat und im Log
- `XrayManager.java`: `Blocks.GRASS_BLOCK`, `DIRT`, `SAND`, `TERRACOTTA` und die anderen Geländeblöcke in `Tables`, `ClientLevel.hasChunk(int,int)`, `getBiome(BlockPos)`, `BlockState.getBlock()`, Blöcke wie `Blocks.COPPER_ORE`
- `OreSim.java`: `net.minecraft.util.Mth` (`sin`, `floor`, `ceil`, `lerp`)
- Menü: `Screen`, `Button.builder`, `Minecraft.gui.setScreen`
