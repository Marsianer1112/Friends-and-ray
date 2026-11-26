package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Anzeige für Freunde, aber nur für das, was sie dir freigegeben haben:
 * <ul>
 * <li>Liste oben rechts mit Koordinaten und Dimension (Freigaben "Koordinaten" und "Dimension").</li>
 * <li>Marker durch Wände mit Name und Entfernung (Koordinaten und Dimension, gleiche Dimension wie du).</li>
 * <li>Leuchten durch Wände: Der Freund bekommt den Umriss, den das Spiel sonst für Leuchten-Effekte zeichnet
 *     (Freigabe "Leuchten", er muss in deiner Nähe geladen sein).</li>
 * </ul>
 */
final class FriendsHud {
	private static final int COLOR_NAME = 0xFF7FDFFF;
	private static final int MAX_LINES = 8;
	/** Flag 6 in den gemeinsamen Entity-Flags des Spiels = leuchtet. */
	private static final int FLAG_GLOWING = 6;

	private static final Set<Integer> GLOWING = new HashSet<>();
	private static Method setFlag;
	private static boolean searched;

	private FriendsHud() {
	}

	// ---------------------------------------------------------------- Bild

	static void draw(GuiGraphicsExtractor g, Minecraft mc, float partial) {
		if (mc.player == null || mc.level == null || mc.font == null) {
			return;
		}
		List<Friends.Friend> friends = Friends.list();
		boolean any = false;
		for (Friends.Friend f : friends) {
			if (Friends.FRIEND.equals(f.state) && f.online) {
				any = true;
				break;
			}
		}
		if (!any) {
			return;
		}
		Vec3 eye = mc.player.getEyePosition(partial);
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		Cam cam = Cam.of(eye.x, eye.y, eye.z, mc.player.getYRot(), mc.player.getXRot(),
			mc.options.fov().get(), w, h);
		String myDim = FriendNet.dimId(mc);

		int line = 0;
		for (Friends.Friend f : friends) {
			if (!Friends.FRIEND.equals(f.state) || !f.online) {
				continue;
			}
			Friends.Position p = f.pos;
			boolean here = p != null && p.hasCoords() && p.dim != null && p.dim.equals(myDim);
			double dist = -1;
			int[] screen = null;
			if (here) {
				double tx = p.x + 0.5;
				double ty = p.y + 1.0;
				double tz = p.z + 0.5;
				dist = Math.sqrt((tx - eye.x) * (tx - eye.x) + (ty - eye.y) * (ty - eye.y) + (tz - eye.z) * (tz - eye.z));
				if (f.showMarker) {
					screen = cam.project(tx, ty, tz);
				}
			}

			if (Friends.hud() && line < MAX_LINES) {
				String text = listText(f, dist);
				g.text(mc.font, text, w - 4 - mc.font.width(text), 4 + line * 10, COLOR_NAME, true);
				line++;
			}
			if (screen != null) {
				g.fill(screen[0] - 3, screen[1] - 3, screen[0] + 4, screen[1] + 4, 0xFF000000);
				g.fill(screen[0] - 2, screen[1] - 2, screen[0] + 3, screen[1] + 3, COLOR_NAME);
				g.text(mc.font, f.name + " " + String.format(Locale.ROOT, "%.0fm", dist), screen[0] + 6, screen[1] - 4,
					COLOR_NAME, true);
			}
		}
	}

	/** Zeile in der Liste: Name, was er freigegeben hat und (falls bekannt) die Entfernung. */
	static String listText(Friends.Friend f, double dist) {
		StringBuilder sb = new StringBuilder(f.name);
		Friends.Position p = f.pos;
		if (p != null && p.hasCoords()) {
			sb.append("  ").append(Math.round(p.x)).append(' ').append(Math.round(p.y)).append(' ').append(Math.round(p.z));
		}
		if (p != null && p.dim != null) {
			sb.append("  ").append(Friends.dimLabel(p.dim));
		}
		if (dist >= 0) {
			sb.append("  ").append(String.format(Locale.ROOT, "%.0fm", dist));
		}
		return sb.toString();
	}

	/** Kamera für die Umrechnung von Weltpunkten in Bildschirmpunkte (wie beim Xray-Marker). */
	static final class Cam {
		private double ex;
		private double ey;
		private double ez;
		private double fx;
		private double fy;
		private double fz;
		private double rx;
		private double rz;
		private double ux;
		private double uy;
		private double uz;
		private double tan;
		private double aspect;
		private int w;
		private int h;

		static Cam of(double ex, double ey, double ez, float yawDeg, float pitchDeg, double fovDeg, int w, int h) {
			Cam c = new Cam();
			c.ex = ex;
			c.ey = ey;
			c.ez = ez;
			double yaw = Math.toRadians(yawDeg);
			double pitch = Math.toRadians(pitchDeg);
			double cy = Math.cos(yaw);
			double sy = Math.sin(yaw);
			double cp = Math.cos(pitch);
			double sp = Math.sin(pitch);
			// Blickrichtung, rechts und oben (Minecraft: Yaw 0 = Süden, positiver Pitch = nach unten)
			c.fx = -sy * cp;
			c.fy = -sp;
			c.fz = cy * cp;
			c.rx = -cy;
			c.rz = -sy;
			c.ux = -c.rz * c.fy;
			c.uy = c.rz * c.fx - c.rx * c.fz;
			c.uz = c.rx * c.fy;
			c.tan = Math.tan(Math.toRadians(fovDeg) / 2.0);
			c.aspect = (double) w / (double) h;
			c.w = w;
			c.h = h;
			return c;
		}

		/** Bildschirmpunkt (x, y) oder null, wenn der Punkt hinter dir oder außerhalb des Bildes liegt. */
		int[] project(double x, double y, double z) {
			double dx = x - ex;
			double dy = y - ey;
			double dz = z - ez;
			double depth = dx * fx + dy * fy + dz * fz;
			if (depth < 0.3) {
				return null;
			}
			double nx = (dx * rx + dz * rz) / depth / (tan * aspect);
			double ny = (dx * ux + dy * uy + dz * uz) / depth / tan;
			if (Math.abs(nx) > 1.05 || Math.abs(ny) > 1.05) {
				return null;
			}
			return new int[] {(int) ((nx + 1.0) * 0.5 * w), (int) ((1.0 - ny) * 0.5 * h)};
		}
	}

	// ---------------------------------------------------------------- Leuchten durch Wände

	/** Jeden Tick: Freunde mit Freigabe "Leuchten" bekommen den Umriss, allen anderen wird er wieder genommen. */
	static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			GLOWING.clear();
			return;
		}
		Collection<?> players = players(mc);
		if (players == null || !flagMethod()) {
			return;
		}
		Set<Integer> now = new HashSet<>();
		for (Object o : players) {
			if (!(o instanceof Entity e) || e == mc.player) {
				continue;
			}
			Friends.Friend f = Friends.find(e.getName().getString());
			if (f != null && Friends.FRIEND.equals(f.state) && (f.got & Friends.GLOW) != 0 && f.showGlow) {
				setGlow(e, true);
				now.add(e.getId());
			}
		}
		for (Object o : players) {
			if (o instanceof Entity e && GLOWING.contains(e.getId()) && !now.contains(e.getId())) {
				setGlow(e, false);
			}
		}
		GLOWING.clear();
		GLOWING.addAll(now);
	}

	private static Collection<?> players(Minecraft mc) {
		Object r = Refl.call(mc.level, "players");
		return r instanceof Collection<?> c ? c : null;
	}

	private static void setGlow(Entity e, boolean on) {
		try {
			setFlag.invoke(e, FLAG_GLOWING, on);
		} catch (ReflectiveOperationException | RuntimeException ex) {
			MacroKitClient.LOGGER.error("Leuchten setzen fehlgeschlagen", ex);
			setFlag = null;
		}
	}

	/** Sucht die (geschützte) Methode, die ein gemeinsames Entity-Flag setzt. */
	private static boolean flagMethod() {
		if (!searched) {
			searched = true;
			for (Class<?> c = Entity.class; c != null && setFlag == null; c = c.getSuperclass()) {
				try {
					Method m = c.getDeclaredMethod("setSharedFlag", int.class, boolean.class);
					m.setAccessible(true);
					setFlag = m;
				} catch (ReflectiveOperationException | RuntimeException ignored) {
					// weiter nach oben suchen
				}
			}
			if (setFlag == null) {
				MacroKitClient.LOGGER.error("Methode zum Setzen von Leuchten nicht gefunden");
			}
		}
		return setFlag != null;
	}

	/** Ob das Leuchten auf dieser Version geht (für die Anzeige im Menü). */
	static boolean glowSupported() {
		return flagMethod();
	}
}
