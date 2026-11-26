package dev.macrokit;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Criticals: Jeder Schlag auf ein Lebewesen wird für den Server zum kritischen Treffer. Kurz vor dem Angriff sendet
 * die Mod zwei Positionspakete (ein winziger Sprung nach oben, dann zurück, beide "nicht am Boden"). Der Server hält
 * den Spieler dadurch für fallend, und die normale Spielregel für kritische Treffer greift.
 *
 * <p>Es passiert nur etwas, wenn du wirklich am Boden stehst, nicht im Wasser oder in Lava, nicht an einer Leiter,
 * nicht auf einem Reittier und nicht sprintest (beim Sprinten gibt es nach den Spielregeln keinen kritischen Treffer).
 * Das Positionspaket wird per Reflection gebaut, weil sich sein Konstruktor zwischen Versionen ändert.
 */
final class Criticals {
	private static final String PACKET_CLASS = "net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos";
	/** So weit geht der kleine Sprung nach oben (Blöcke). */
	private static final double HOP = 0.0625;

	private static boolean enabled;
	private static Constructor<?> posConstructor;
	private static boolean searched;

	private Criticals() {
	}

	static boolean enabled() {
		return enabled;
	}

	/** Meldet sich einmal beim Start für jeden Angriff an. */
	static void register() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			onAttack(player, entity);
			return InteractionResult.PASS;
		});
	}

	static void toggle(Minecraft mc) {
		set(mc, !enabled);
	}

	static void set(Minecraft mc, boolean on) {
		if (on && constructor() == null) {
			Chat.info(mc, "Criticals geht auf dieser Version nicht (Positionspaket nicht gefunden).");
			enabled = false;
			return;
		}
		enabled = on;
		Chat.info(mc, "Criticals: " + (on ? "AN" : "AUS"));
	}

	static String statusText() {
		return enabled ? "Criticals" : null;
	}

	/** Wird vor jedem Angriff des Spielers aufgerufen (auch im Einzelspieler, dort auch vom internen Server). */
	static void onAttack(Player player, Entity target) {
		Minecraft mc = Minecraft.getInstance();
		if (!enabled || mc == null || mc.player == null || player != mc.player) {
			return;
		}
		if (!(target instanceof LivingEntity) || !canCrit(player)) {
			return;
		}
		send(mc, player);
	}

	/** Steht der Spieler so, dass die Spielregel einen kritischen Treffer überhaupt erlaubt? */
	private static boolean canCrit(Player player) {
		Boolean ground = flag(player, "onGround", "isOnGround");
		if (ground == null || !ground) {
			return false;
		}
		for (String name : new String[] {"isInWater", "isInLava", "onClimbable", "isPassenger", "isSprinting"}) {
			if (Boolean.TRUE.equals(flag(player, name))) {
				return false;
			}
		}
		return true;
	}

	/** Ruft eine Ja/Nein-Methode ohne Parameter auf; null = es gibt sie nicht. */
	private static Boolean flag(Object target, String... names) {
		for (String name : names) {
			try {
				Method m = target.getClass().getMethod(name);
				if (m.getReturnType() == boolean.class) {
					return (Boolean) m.invoke(target);
				}
			} catch (ReflectiveOperationException | RuntimeException ignored) {
				// nächsten Namen probieren
			}
		}
		return null;
	}

	private static void send(Minecraft mc, Player player) {
		Constructor<?> c = constructor();
		if (c == null) {
			enabled = false;
			return;
		}
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		try {
			mc.player.connection.send((Packet<?>) make(c, x, y + HOP, z));
			mc.player.connection.send((Packet<?>) make(c, x, y, z));
		} catch (ReflectiveOperationException | RuntimeException e) {
			MacroKitClient.LOGGER.error("Criticals abgeschaltet", e);
			enabled = false;
			Chat.info(mc, "Criticals wurde abgeschaltet (Fehler beim Senden, siehe Log).");
		}
	}

	/** Positionspaket mit "nicht am Boden": drei Zahlen für die Position, alle weiteren Ja/Nein-Werte auf false. */
	private static Object make(Constructor<?> c, double x, double y, double z) throws ReflectiveOperationException {
		Class<?>[] types = c.getParameterTypes();
		Object[] args = new Object[types.length];
		args[0] = x;
		args[1] = y;
		args[2] = z;
		for (int i = 3; i < args.length; i++) {
			args[i] = Boolean.FALSE;
		}
		return c.newInstance(args);
	}

	/** Konstruktor (double x, double y, double z, boolean, ...). */
	private static Constructor<?> constructor() {
		if (!searched) {
			searched = true;
			try {
				Class<?> pos = Class.forName(PACKET_CLASS, true, Minecraft.class.getClassLoader());
				for (Constructor<?> c : pos.getDeclaredConstructors()) {
					Class<?>[] p = c.getParameterTypes();
					if (p.length < 4 || p[0] != double.class || p[1] != double.class || p[2] != double.class) {
						continue;
					}
					boolean rest = true;
					for (int i = 3; i < p.length; i++) {
						rest &= p[i] == boolean.class;
					}
					if (rest) {
						c.setAccessible(true);
						posConstructor = c;
						break;
					}
				}
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				MacroKitClient.LOGGER.error("Positionspaket nicht gefunden", e);
			}
			if (posConstructor == null) {
				MacroKitClient.LOGGER.error("Konstruktor des Positionspakets nicht gefunden");
			}
		}
		return posConstructor;
	}
}
