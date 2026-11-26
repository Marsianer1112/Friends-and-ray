package dev.macrokit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Freunde: Liste, Anfragen und Freigaben. Alles ist freiwillig und gegenseitig:
 *
 * <ul>
 * <li>Eine Freundschaft entsteht nur, wenn eine Anfrage angenommen wird.</li>
 * <li>Jeder Freund legt selbst fest, was der andere sehen darf: {@link #GLOW Leuchten durch Wände},
 *     {@link #COORDS Koordinaten} und {@link #DIM Dimension}. Standard ist nichts.</li>
 * <li>Die Mod verschickt Koordinaten und Dimension nur an Freunde, denen sie freigegeben wurden. Jede Freigabe lässt
 *     sich jederzeit wieder entziehen.</li>
 * </ul>
 *
 * <p>Gespeichert wird in {@code config/macrokit-friends.json}. Die Nachrichten zwischen den Mods regelt {@link FriendNet}.
 */
final class Friends {
	/** Ich habe angefragt, die Antwort steht noch aus. */
	static final String OUT = "OUT";
	/** Jemand hat mir eine Anfrage geschickt. */
	static final String IN = "IN";
	static final String FRIEND = "FRIEND";

	static final int GLOW = 1;
	static final int COORDS = 2;
	static final int DIM = 4;
	static final int ALL = GLOW | COORDS | DIM;
	/** So viele unbeantwortete Anfragen werden höchstens gemerkt (Schutz vor Spam). */
	static final int MAX_PENDING_IN = 10;

	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	static final class Friend {
		String name;
		String state = FRIEND;
		/** Was ich diesem Freund zu sehen erlaube (Bits GLOW, COORDS, DIM). */
		int give;
		/** Was dieser Freund mir erlaubt (zuletzt gemeldet). */
		int got;
		/** Meine Anzeige-Einstellungen für diesen Freund (wirken nur, wenn er es erlaubt). */
		boolean showGlow = true;
		boolean showMarker = true;
		transient boolean online;
		transient Position pos;
	}

	/** Zuletzt gemeldete Position eines Freundes; Felder, die er nicht freigegeben hat, sind null. */
	static final class Position {
		Double x;
		Double y;
		Double z;
		/** Dimension, z. B. "minecraft:overworld". */
		String dim;

		boolean hasCoords() {
			return x != null && y != null && z != null;
		}
	}

	private static final class Data {
		boolean acceptRequests = true;
		boolean hud = true;
		List<Friend> friends = new ArrayList<>();
	}

	private static Data data = new Data();

	private Friends() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("macrokit-friends.json");
	}

	static synchronized void load() {
		data = new Data();
		Path f = file();
		if (!Files.isRegularFile(f)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
			Data loaded = GSON.fromJson(reader, Data.class);
			if (loaded != null) {
				data = loaded;
			}
		} catch (Exception e) {
			MacroKitClient.LOGGER.warn("Konnte {} nicht lesen", f, e);
		}
		if (data.friends == null) {
			data.friends = new ArrayList<>();
		}
		data.friends.removeIf(fr -> fr == null || fr.name == null || !NAME.matcher(fr.name).matches());
		for (Friend fr : data.friends) {
			if (!IN.equals(fr.state) && !OUT.equals(fr.state)) {
				fr.state = FRIEND;
			}
			fr.give &= ALL;
			fr.got &= ALL;
		}
	}

	private static void save() {
		try {
			Files.createDirectories(file().getParent());
			try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException e) {
			MacroKitClient.LOGGER.error("Konnte Freunde nicht speichern", e);
		}
	}

	static boolean validName(String name) {
		return name != null && NAME.matcher(name).matches();
	}

	// ---------------------------------------------------------------- Abfragen

	static synchronized Friend find(String name) {
		if (name == null) {
			return null;
		}
		for (Friend f : data.friends) {
			if (f.name.equalsIgnoreCase(name)) {
				return f;
			}
		}
		return null;
	}

	/** Alle Einträge: Anfragen an mich zuerst, dann Freunde (online zuerst), zuletzt meine offenen Anfragen. */
	static synchronized List<Friend> list() {
		List<Friend> copy = new ArrayList<>(data.friends);
		copy.sort(Comparator.<Friend>comparingInt(f -> IN.equals(f.state) ? 0 : FRIEND.equals(f.state) ? 1 : 2)
			.thenComparing(f -> f.online ? 0 : 1)
			.thenComparing(f -> f.name.toLowerCase(Locale.ROOT)));
		return copy;
	}

	static synchronized boolean acceptRequests() {
		return data.acceptRequests;
	}

	static synchronized void setAcceptRequests(boolean on) {
		data.acceptRequests = on;
		save();
	}

	static synchronized boolean hud() {
		return data.hud;
	}

	static synchronized void setHud(boolean on) {
		data.hud = on;
		save();
	}

	static String statusText(Friend f) {
		if (IN.equals(f.state)) {
			return "will dein Freund sein";
		}
		if (OUT.equals(f.state)) {
			return "Anfrage gesendet";
		}
		return f.online ? "online" : "offline";
	}

	static String dimLabel(String dim) {
		if (dim == null) {
			return "?";
		}
		switch (dim) {
			case "minecraft:overworld": return "Oberwelt";
			case "minecraft:the_nether": return "Nether";
			case "minecraft:the_end": return "Ende";
			default: return dim;
		}
	}

	/** Beschreibt in Worten, was der Freund mir gerade erlaubt. */
	static String describeGot(int bits) {
		List<String> parts = new ArrayList<>();
		if ((bits & GLOW) != 0) {
			parts.add("Leuchten");
		}
		if ((bits & COORDS) != 0) {
			parts.add("Koordinaten");
		}
		if ((bits & DIM) != 0) {
			parts.add("Dimension");
		}
		return parts.isEmpty() ? "nichts" : String.join(", ", parts);
	}

	// ---------------------------------------------------------------- meine Aktionen

	/** Anfrage senden. Gibt eine Fehlermeldung zurück oder null. */
	static synchronized String add(Minecraft mc, String name) {
		if (!validName(name)) {
			return "Ungültiger Spielername (nur Buchstaben, Zahlen und _, höchstens 16 Zeichen).";
		}
		if (name.equalsIgnoreCase(FriendNet.myName(mc))) {
			return "Du kannst dich nicht selbst als Freund hinzufügen.";
		}
		Friend f = find(name);
		if (f == null) {
			f = new Friend();
			f.name = name;
			f.state = OUT;
			data.friends.add(f);
			save();
			FriendNet.send(mc, name, "REQ", "");
			Chat.info(mc, "Anfrage an " + name + " gesendet. " + (FriendNet.isOnline(name)
				? "Sie kommt an, wenn " + name + " die Mod hat."
				: name + " ist gerade nicht online, die Anfrage kommt an, sobald er online ist (er braucht die Mod)."));
			return null;
		}
		if (FRIEND.equals(f.state)) {
			return f.name + " ist schon dein Freund.";
		}
		if (IN.equals(f.state)) {
			return accept(mc, name);
		}
		FriendNet.send(mc, f.name, "REQ", "");
		Chat.info(mc, "Anfrage an " + f.name + " noch einmal gesendet.");
		return null;
	}

	static synchronized String accept(Minecraft mc, String name) {
		Friend f = find(name);
		if (f == null || !IN.equals(f.state)) {
			return "Von " + name + " gibt es keine offene Anfrage.";
		}
		f.state = FRIEND;
		save();
		FriendNet.send(mc, f.name, "ACC", "");
		FriendNet.sendPerm(mc, f);
		Chat.info(mc, f.name + " ist jetzt dein Freund. Freigaben: Menü > Freunde > " + f.name + " (standardmäßig wird nichts geteilt).");
		return null;
	}

	static synchronized String decline(Minecraft mc, String name) {
		Friend f = find(name);
		if (f == null || !IN.equals(f.state)) {
			return "Von " + name + " gibt es keine offene Anfrage.";
		}
		drop(mc, f);
		Chat.info(mc, "Anfrage von " + f.name + " abgelehnt.");
		return null;
	}

	/** Freund entfernen oder eigene Anfrage zurückziehen. */
	static synchronized String remove(Minecraft mc, String name) {
		Friend f = find(name);
		if (f == null) {
			return name + " steht nicht in deiner Freundesliste.";
		}
		drop(mc, f);
		Chat.info(mc, f.name + " entfernt. Er sieht nichts mehr von dir"
			+ (FriendNet.isOnline(f.name) ? "." : ", sobald er online ist."));
		return null;
	}

	private static void drop(Minecraft mc, Friend f) {
		data.friends.remove(f);
		save();
		FriendNet.send(mc, f.name, "REM", "");
		FriendNet.forget(f.name);
	}

	/** Erlaubt oder verbietet einem Freund, ein Stück von mir zu sehen. */
	static synchronized String setGive(Minecraft mc, String name, int bit, boolean on) {
		Friend f = find(name);
		if (f == null || !FRIEND.equals(f.state)) {
			return name + " ist (noch) kein Freund.";
		}
		int before = f.give;
		f.give = on ? (f.give | bit) : (f.give & ~bit);
		if (f.give == before) {
			return null;
		}
		save();
		FriendNet.sendPerm(mc, f);
		FriendNet.forget(f.name); // Position beim nächsten Mal neu senden
		return null;
	}

	static synchronized String setShowGlow(String name, boolean on) {
		Friend f = find(name);
		if (f == null) {
			return name + " steht nicht in deiner Freundesliste.";
		}
		f.showGlow = on;
		save();
		return null;
	}

	static synchronized String setShowMarker(String name, boolean on) {
		Friend f = find(name);
		if (f == null) {
			return name + " steht nicht in deiner Freundesliste.";
		}
		f.showMarker = on;
		save();
		return null;
	}

	// ---------------------------------------------------------------- Nachrichten von anderen (nach Prüfung durch FriendNet)

	/** Eine Anfrage von {@code from} ist angekommen. */
	static synchronized void onRequest(Minecraft mc, String from) {
		Friend f = find(from);
		if (f != null) {
			f.name = from;
			if (OUT.equals(f.state)) {
				// beide wollten
				f.state = FRIEND;
				save();
				FriendNet.send(mc, f.name, "ACC", "");
				FriendNet.sendPerm(mc, f);
				Chat.info(mc, f.name + " und du seid jetzt Freunde.");
			} else if (FRIEND.equals(f.state)) {
				// er hat uns vergessen: bestätigen und Freigaben noch einmal schicken
				FriendNet.send(mc, f.name, "ACC", "");
				FriendNet.sendPerm(mc, f);
			}
			return;
		}
		if (!data.acceptRequests || pendingIn() >= MAX_PENDING_IN) {
			return;
		}
		f = new Friend();
		f.name = from;
		f.state = IN;
		data.friends.add(f);
		save();
		Chat.info(mc, from + " möchte dein Freund sein. Annehmen: /macro friend accept " + from
			+ " oder Menü > Freunde. Ablehnen: /macro friend decline " + from);
	}

	static synchronized void onAccept(Minecraft mc, String from) {
		Friend f = find(from);
		if (f != null && OUT.equals(f.state)) {
			f.name = from;
			f.state = FRIEND;
			save();
			FriendNet.sendPerm(mc, f);
			Chat.info(mc, from + " hat deine Anfrage angenommen. Freigaben: Menü > Freunde > " + from);
		}
	}

	static synchronized void onRemove(Minecraft mc, String from) {
		Friend f = find(from);
		if (f != null) {
			data.friends.remove(f);
			save();
			FriendNet.forget(f.name);
			Chat.info(mc, from + " hat die Freundschaft beendet oder die Anfrage zurückgezogen.");
		}
	}

	/** Der Freund hat mitgeteilt, was ich von ihm sehen darf. */
	static synchronized void onPerm(String from, int bits) {
		Friend f = find(from);
		if (f == null || !FRIEND.equals(f.state)) {
			return;
		}
		f.name = from;
		f.got = bits & ALL;
		if (f.pos != null) {
			if ((f.got & COORDS) == 0) {
				f.pos.x = null;
				f.pos.y = null;
				f.pos.z = null;
			}
			if ((f.got & DIM) == 0) {
				f.pos.dim = null;
			}
		}
		save();
	}

	/** Der Freund hat seine Position gemeldet. Es wird nur übernommen, was er mir erlaubt. */
	static synchronized void onPos(String from, Double x, Double y, Double z, String dim) {
		Friend f = find(from);
		if (f == null || !FRIEND.equals(f.state)) {
			return;
		}
		Position p = new Position();
		if ((f.got & COORDS) != 0 && x != null && y != null && z != null) {
			p.x = x;
			p.y = y;
			p.z = z;
		}
		if ((f.got & DIM) != 0) {
			p.dim = dim;
		}
		f.pos = p;
	}

	// ---------------------------------------------------------------- Laufzeit

	static synchronized void setOnline(Friend f, boolean online) {
		f.online = online;
		if (!online) {
			f.pos = null;
		}
	}

	static synchronized void clearRuntime() {
		for (Friend f : data.friends) {
			f.online = false;
			f.pos = null;
		}
	}

	private static int pendingIn() {
		int n = 0;
		for (Friend f : data.friends) {
			if (IN.equals(f.state)) {
				n++;
			}
		}
		return n;
	}
}
