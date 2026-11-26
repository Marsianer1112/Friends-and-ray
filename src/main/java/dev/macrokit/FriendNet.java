package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nachrichten zwischen den Mods der Freunde. Ein reiner Client-Mod kann dem Server nichts beibringen, deshalb laufen
 * die Daten als private Nachricht ({@code /msg <name> ...}) von Mod zu Mod. Die Mod des Empfängers erkennt das
 * Muster {@code [MK1|TYP|von|an|daten]}, wertet es aus und blendet die Nachricht im Chat aus.
 *
 * <ul>
 * <li>REQ, ACC, REM: Anfrage, Annahme, Ablehnen/Entfernen/Zurückziehen.</li>
 * <li>PRM: was ich dem Freund erlaube (drei Ziffern: Leuchten, Koordinaten, Dimension).</li>
 * <li>POS: meine Position, nur mit den Teilen, die ich erlaubt habe.</li>
 * </ul>
 *
 * <p>Damit der Server uns nicht wegen Spam wirft, geht höchstens alle {@link #SEND_GAP} Ticks eine Nachricht raus.
 * Wer die Server-Logs liest (also der Betreiber), sieht die privaten Nachrichten samt Koordinaten.
 */
final class FriendNet {
	static final String PREFIX = "[MK1|";
	private static final Pattern TOKEN = Pattern.compile(
		"\\[MK1\\|([A-Z]{3})\\|([A-Za-z0-9_]{1,16})\\|([A-Za-z0-9_]{1,16})\\|([^\\]|]*)\\]");
	private static final Pattern ID = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)\\]?\\s*$");

	/** Mindestabstand zwischen zwei gesendeten Nachrichten (Ticks). */
	static final int SEND_GAP = 25;
	/** So oft wird die Spielerliste geprüft. */
	static final int PRESENCE_TICKS = 20;
	/** So oft wird die eigene Position an Freunde gemeldet (nur bei Änderung). */
	static final int POS_TICKS = 100;
	/** Weniger Bewegung als so viele Blöcke wird nicht gemeldet. */
	static final double MIN_MOVE = 2.0;
	/** Auf unerwünschte Nachrichten von Nicht-Freunden höchstens so oft (Ticks) mit REM antworten. */
	private static final long REM_REPLY_TICKS = 1200;

	private static final class Msg {
		final String to;
		final String type;
		final String data;

		Msg(String to, String type, String data) {
			this.to = to;
			this.type = type;
			this.data = data;
		}
	}

	private static final class Sent {
		final double x;
		final double y;
		final double z;
		final String dim;
		final int give;

		Sent(double x, double y, double z, String dim, int give) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.dim = dim;
			this.give = give;
		}
	}

	static long ticks;
	private static int sinceSend = SEND_GAP;
	private static final LinkedHashMap<String, Msg> QUEUE = new LinkedHashMap<>();
	private static final Map<String, Sent> LAST_SENT = new HashMap<>();
	private static final Map<String, Long> REM_SENT = new HashMap<>();
	/** Namen aller Spieler auf dem Server in Kleinbuchstaben; null = Spielerliste nicht lesbar. */
	private static Set<String> online;
	private static List<String> onlineNames = new ArrayList<>();
	private static boolean inWorld;
	private static boolean warnedPresence;
	private static boolean hooked;

	private FriendNet() {
	}

	// ---------------------------------------------------------------- Empfang

	/**
	 * Hängt die Mod in den Chat-Empfang (Fabric API, ClientReceiveMessageEvents). Das geschieht per Reflection:
	 * Die Parameterlisten der Hooks ändern sich zwischen Versionen, wir brauchen nur den Text der Nachricht.
	 */
	static void register() {
		try {
			Class<?> events = Class.forName("net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents");
			Method register = Class.forName("net.fabricmc.fabric.api.event.Event").getMethod("register", Object.class);
			int ok = 0;
			for (String name : new String[] {"ALLOW_CHAT", "ALLOW_GAME"}) {
				try {
					Field field = events.getField(name);
					Class<?> listener = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
					Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] {listener},
						FriendNet::handle);
					register.invoke(field.get(null), proxy);
					ok++;
				} catch (ReflectiveOperationException | RuntimeException e) {
					MacroKitClient.LOGGER.error("Freunde: Hook {} nicht möglich", name, e);
				}
			}
			hooked = ok > 0;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			MacroKitClient.LOGGER.error("Freunde: Chat-Empfang nicht möglich", e);
		}
	}

	static boolean hooked() {
		return hooked;
	}

	private static Object handle(Object proxy, Method method, Object[] args) {
		if (method.getDeclaringClass() == Object.class) {
			switch (method.getName()) {
				case "equals": return args != null && args.length == 1 && proxy == args[0];
				case "hashCode": return System.identityHashCode(proxy);
				default: return "MacroKitFriends";
			}
		}
		boolean show = true;
		try {
			if (args != null) {
				for (Object a : args) {
					if (a instanceof Component c) {
						show = allow(c.getString());
						break;
					}
				}
			}
		} catch (RuntimeException e) {
			MacroKitClient.LOGGER.error("Freunde: Fehler beim Auswerten einer Nachricht", e);
		}
		return method.getReturnType() == boolean.class ? (Object) show : null;
	}

	/**
	 * Prüft eine eingehende Chat-Nachricht.
	 *
	 * @return true = im Chat anzeigen, false = ausblenden (war für die Mod bestimmt)
	 */
	static boolean allow(String text) {
		Matcher m = TOKEN.matcher(text);
		if (!m.find()) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.player == null) {
			return false;
		}
		String type = m.group(1);
		String from = m.group(2);
		String to = m.group(3);
		String data = m.group(4);
		String me = myName(mc);
		// Das Echo der eigenen Nachricht ("Du flüsterst an ...") und Fremdes werden nur ausgeblendet
		if (!to.equalsIgnoreCase(me) || from.equalsIgnoreCase(me)) {
			return false;
		}
		// Absender prüfen: sein Name muss vor dem Muster in der angezeigten Zeile stehen (so sagt es der Server)
		int at = text.indexOf(from);
		if (at < 0 || at > m.start()) {
			return false;
		}
		dispatch(mc, type, from, data);
		return false;
	}

	private static void dispatch(Minecraft mc, String type, String from, String data) {
		switch (type) {
			case "REQ":
				Friends.onRequest(mc, from);
				break;
			case "ACC":
				Friends.onAccept(mc, from);
				break;
			case "REM":
				Friends.onRemove(mc, from);
				break;
			case "PRM":
			case "POS":
				Friends.Friend f = Friends.find(from);
				if (f == null) {
					// Er hält uns noch für Freunde: einmal Bescheid sagen, damit er aufhört zu senden
					replyRemoved(mc, from);
					break;
				}
				if (!Friends.FRIEND.equals(f.state)) {
					break;
				}
				if (type.equals("PRM")) {
					Friends.onPerm(from, parsePerm(data));
				} else {
					parsePos(from, data);
				}
				break;
			default:
				break;
		}
	}

	private static void replyRemoved(Minecraft mc, String from) {
		String key = from.toLowerCase(Locale.ROOT);
		Long last = REM_SENT.get(key);
		if (last == null || ticks - last > REM_REPLY_TICKS) {
			REM_SENT.put(key, ticks);
			send(mc, from, "REM", "");
		}
	}

	static int parsePerm(String data) {
		int bits = 0;
		if (data.length() >= 3) {
			bits |= data.charAt(0) == '1' ? Friends.GLOW : 0;
			bits |= data.charAt(1) == '1' ? Friends.COORDS : 0;
			bits |= data.charAt(2) == '1' ? Friends.DIM : 0;
		}
		return bits;
	}

	static String formatPerm(int bits) {
		return ((bits & Friends.GLOW) != 0 ? "1" : "0") + ((bits & Friends.COORDS) != 0 ? "1" : "0")
			+ ((bits & Friends.DIM) != 0 ? "1" : "0");
	}

	private static void parsePos(String from, String data) {
		String[] p = data.split(",", -1);
		if (p.length != 4) {
			return;
		}
		try {
			Double x = p[0].equals("~") ? null : Double.valueOf(p[0]);
			Double y = p[1].equals("~") ? null : Double.valueOf(p[1]);
			Double z = p[2].equals("~") ? null : Double.valueOf(p[2]);
			String dim = p[3].equals("~") ? null : expandDim(p[3]);
			Friends.onPos(from, x, y, z, dim);
		} catch (NumberFormatException ignored) {
			// kaputte Nachricht, ignorieren
		}
	}

	// ---------------------------------------------------------------- Dimension

	/** Dimension, in der ich bin, z. B. "minecraft:overworld"; null = unbekannt. */
	static String dimId(Minecraft mc) {
		if (mc.level == null) {
			return null;
		}
		Matcher m = ID.matcher(String.valueOf(mc.level.dimension()));
		return m.find() ? m.group(1) : null;
	}

	static String shortDim(String id) {
		switch (id) {
			case "minecraft:overworld": return "o";
			case "minecraft:the_nether": return "n";
			case "minecraft:the_end": return "e";
			default: return id;
		}
	}

	static String expandDim(String code) {
		switch (code) {
			case "o": return "minecraft:overworld";
			case "n": return "minecraft:the_nether";
			case "e": return "minecraft:the_end";
			default: return code;
		}
	}

	// ---------------------------------------------------------------- Senden

	static String myName(Minecraft mc) {
		return mc.player == null ? "" : mc.player.getName().getString();
	}

	/** Stellt eine Nachricht in die Warteschlange. Eine neuere Nachricht gleicher Art an denselben Freund ersetzt die ältere. */
	static synchronized void send(Minecraft mc, String to, String type, String data) {
		QUEUE.put(to.toLowerCase(Locale.ROOT) + "|" + type, new Msg(to, type, data));
	}

	static void sendPerm(Minecraft mc, Friends.Friend f) {
		send(mc, f.name, "PRM", formatPerm(f.give));
	}

	/** Vergisst, was zuletzt an diesen Freund gemeldet wurde (die Position wird dann neu gesendet). */
	static synchronized void forget(String name) {
		LAST_SENT.remove(name.toLowerCase(Locale.ROOT));
	}

	/** Nachricht als Befehl ausführen. */
	private static void transmit(Minecraft mc, Msg m) {
		String text = PREFIX + m.type + "|" + myName(mc) + "|" + m.to + "|" + m.data + "]";
		if (m.type.equals("REQ")) {
			text += " MacroKit-Freundschaftsanfrage (nur mit der MacroKit-Mod sichtbar)";
		}
		mc.player.connection.sendCommand("msg " + m.to + " " + text);
	}

	// ---------------------------------------------------------------- Spielerliste

	static synchronized boolean isOnline(String name) {
		return online != null && online.contains(name.toLowerCase(Locale.ROOT));
	}

	/** Namen der Spieler auf dem Server (ohne mich), für Vorschläge und die Auswahl im Menü. */
	static synchronized List<String> onlineNames() {
		return new ArrayList<>(onlineNames);
	}

	static synchronized boolean presenceKnown() {
		return online != null;
	}

	private static List<String> readOnline(Minecraft mc) {
		Object conn = mc.player.connection;
		for (String method : new String[] {"getOnlinePlayers", "getListedOnlinePlayers"}) {
			Object r = Refl.call(conn, method);
			if (r instanceof Collection<?> c && !c.isEmpty()) {
				List<String> out = namesOf(c);
				if (!out.isEmpty()) {
					return out;
				}
			}
		}
		Object provider = Refl.call(conn, "getSuggestionsProvider");
		Object r = Refl.call(provider, "getOnlinePlayerNames");
		if (r instanceof Collection<?> c && !c.isEmpty()) {
			List<String> out = namesOf(c);
			if (!out.isEmpty()) {
				return out;
			}
		}
		return null;
	}

	private static List<String> namesOf(Collection<?> c) {
		List<String> out = new ArrayList<>();
		for (Object el : c) {
			if (el instanceof String s) {
				out.add(s);
				continue;
			}
			Object profile = Refl.call(el, "getProfile", "profile");
			Object name = Refl.call(profile, "name", "getName");
			if (name instanceof String s) {
				out.add(s);
			}
		}
		return out;
	}

	private static synchronized void updatePresence(Minecraft mc) {
		List<String> names = readOnline(mc);
		if (names == null) {
			online = null;
			onlineNames = new ArrayList<>();
			if (!warnedPresence) {
				warnedPresence = true;
				Chat.info(mc, "Freunde: Die Spielerliste lässt sich nicht lesen. Anfragen und Freigaben gehen nur, "
					+ "wenn du sie selbst auslöst, Positionen werden nicht automatisch gesendet.");
			}
			Friends.clearRuntime();
			return;
		}
		String me = myName(mc).toLowerCase(Locale.ROOT);
		Set<String> now = new HashSet<>();
		List<String> display = new ArrayList<>();
		for (String n : names) {
			String lower = n.toLowerCase(Locale.ROOT);
			if (!lower.equals(me) && now.add(lower)) {
				display.add(n);
			}
		}
		display.sort(String.CASE_INSENSITIVE_ORDER);
		online = now;
		onlineNames = display;

		for (Friends.Friend f : Friends.list()) {
			if (Friends.IN.equals(f.state)) {
				continue;
			}
			boolean isOn = now.contains(f.name.toLowerCase(Locale.ROOT));
			if (isOn && !f.online) {
				Friends.setOnline(f, true);
				appeared(mc, f);
			} else if (!isOn && f.online) {
				Friends.setOnline(f, false);
				forget(f.name);
			}
		}
	}

	/** Ein Freund ist (wieder) online: Anfrage oder Freigaben erneut senden, Position neu melden. */
	private static void appeared(Minecraft mc, Friends.Friend f) {
		forget(f.name);
		if (Friends.OUT.equals(f.state)) {
			send(mc, f.name, "REQ", "");
		} else if (Friends.FRIEND.equals(f.state)) {
			sendPerm(mc, f);
			queuePosition(mc, f);
		}
	}

	// ---------------------------------------------------------------- Tick

	/** Am Ende jedes Ticks. */
	static void tick(Minecraft mc) {
		if (mc.player == null) {
			if (inWorld) {
				inWorld = false;
				reset();
			}
			return;
		}
		inWorld = true;
		ticks++;
		sinceSend++;
		if (ticks % PRESENCE_TICKS == 0) {
			updatePresence(mc);
		}
		if (ticks % POS_TICKS == 0) {
			for (Friends.Friend f : Friends.list()) {
				if (Friends.FRIEND.equals(f.state) && f.online) {
					queuePosition(mc, f);
				}
			}
		}
		if (sinceSend >= SEND_GAP) {
			flush(mc);
		}
	}

	private static synchronized void reset() {
		QUEUE.clear();
		LAST_SENT.clear();
		online = null;
		onlineNames = new ArrayList<>();
		warnedPresence = false;
		Friends.clearRuntime();
	}

	/** Meldet meine Position (nur erlaubte Teile) an einen Freund, wenn sie sich geändert hat. */
	private static synchronized void queuePosition(Minecraft mc, Friends.Friend f) {
		int give = f.give & (Friends.COORDS | Friends.DIM);
		if (give == 0 || mc.player == null) {
			return;
		}
		double x = Math.floor(mc.player.getX());
		double y = Math.floor(mc.player.getY());
		double z = Math.floor(mc.player.getZ());
		String dim = dimId(mc);
		String key = f.name.toLowerCase(Locale.ROOT);
		Sent last = LAST_SENT.get(key);
		boolean changed = last == null || last.give != give;
		if (!changed && (give & Friends.COORDS) != 0) {
			double dx = x - last.x;
			double dy = y - last.y;
			double dz = z - last.z;
			changed = Math.sqrt(dx * dx + dy * dy + dz * dz) >= MIN_MOVE;
		}
		if (!changed && (give & Friends.DIM) != 0) {
			changed = dim == null ? last.dim != null : !dim.equals(last.dim);
		}
		if (!changed) {
			return;
		}
		String coords = (give & Friends.COORDS) != 0 ? (long) x + "," + (long) y + "," + (long) z : "~,~,~";
		String dimPart = (give & Friends.DIM) != 0 && dim != null ? shortDim(dim) : "~";
		send(mc, f.name, "POS", coords + "," + dimPart);
		LAST_SENT.put(key, new Sent(x, y, z, dim, give));
	}

	/** Schickt die nächste Nachricht aus der Warteschlange. Nachrichten an Spieler, die offline sind, entfallen. */
	private static synchronized void flush(Minecraft mc) {
		Iterator<Msg> it = QUEUE.values().iterator();
		while (it.hasNext()) {
			Msg m = it.next();
			it.remove();
			if (online != null && !online.contains(m.to.toLowerCase(Locale.ROOT))) {
				continue; // nicht online: kommt nicht an
			}
			transmit(mc, m);
			sinceSend = 0;
			return;
		}
	}
}
