package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * AutoTotem: Hält einen Totem der Unsterblichkeit in der Zweithand. Fehlt er (z. B. weil er gerade aufgebraucht
 * wurde), tauscht die Mod einen Totem aus dem Inventar oder der Schnellleiste per Tausch-Klick in die Zweithand.
 * Das ist derselbe Klick, den das Spiel sendet, wenn du im Inventar mit der Maus über einem Slot F drückst.
 *
 * <p>Der Klick passiert nur im Spiel ohne offenen Bildschirm. Die Klick-Methode des Spiels wird per Reflection
 * gesucht (Methode mit fünf Parametern, davon einer ein Enum mit dem Wert SWAP), damit der Build nicht von ihrem
 * Namen abhängt.
 */
final class AutoTotem {
	/** Taste 40 beim Tausch-Klick bedeutet: Tausch mit der Zweithand. */
	private static final int OFFHAND_BUTTON = 40;
	/** Nach einem Klick so viele Ticks warten, bevor es noch einmal versucht wird (falls der Server ablehnt). */
	private static final int RETRY_TICKS = 5;
	/** Hauptinventar (9-35) und Schnellleiste (0-8) im Spielerinventar. */
	private static final int INVENTORY_SLOTS = 36;
	private static final int HOTBAR_SLOTS = 9;
	/** Die Schnellleiste liegt im Inventar-Menü auf den Slots 36 bis 44. */
	private static final int MENU_HOTBAR_START = 36;

	private static boolean enabled;
	private static int waited;
	private static int retry;
	private static Method clickMethod;
	private static Object swapConstant;
	private static Field gameModeField;
	private static Method gameModeGetter;
	private static boolean searched;

	private AutoTotem() {
	}

	static boolean enabled() {
		return enabled;
	}

	static void toggle(Minecraft mc) {
		set(mc, !enabled);
	}

	static void set(Minecraft mc, boolean on) {
		if (on && !prepare()) {
			Chat.info(mc, "AutoTotem geht auf dieser Version nicht (Inventar-Klick nicht gefunden).");
			enabled = false;
			return;
		}
		enabled = on;
		waited = 0;
		retry = 0;
		Chat.info(mc, "AutoTotem: " + (on ? "AN (Pause " + MacroStore.autoTotemDelay() + " Ticks)" : "AUS"));
	}

	/** Am Ende jedes Ticks: Totem nachlegen, falls die Zweithand keinen hat. */
	static void tick(Minecraft mc) {
		if (!enabled) {
			return;
		}
		if (mc.player == null || mc.gui.screen() != null) {
			waited = 0;
			return;
		}
		if (retry > 0) {
			retry--;
		}
		if (isTotem(mc.player.getOffhandItem())) {
			waited = 0;
			return;
		}
		int slot = totemSlot(mc.player.getInventory());
		if (slot < 0 || retry > 0) {
			return;
		}
		if (waited < MacroStore.autoTotemDelay()) {
			waited++;
			return;
		}
		waited = 0;
		retry = RETRY_TICKS;
		click(mc, slot);
	}

	/** Slot im Inventar-Menü, in dem ein Totem liegt, sonst -1. Das Hauptinventar kommt vor der Schnellleiste. */
	static int totemSlot(Inventory inv) {
		for (int i = HOTBAR_SLOTS; i < INVENTORY_SLOTS; i++) {
			if (isTotem(inv.getItem(i))) {
				return i;
			}
		}
		for (int i = 0; i < HOTBAR_SLOTS; i++) {
			if (isTotem(inv.getItem(i))) {
				return MENU_HOTBAR_START + i;
			}
		}
		return -1;
	}

	/** Alle Totems in Inventar, Schnellleiste und Zweithand. */
	static int count(Minecraft mc) {
		if (mc.player == null) {
			return 0;
		}
		int n = 0;
		Inventory inv = mc.player.getInventory();
		for (int i = 0; i < INVENTORY_SLOTS; i++) {
			if (isTotem(inv.getItem(i))) {
				n += inv.getItem(i).getCount();
			}
		}
		ItemStack off = mc.player.getOffhandItem();
		if (isTotem(off)) {
			n += off.getCount();
		}
		return n;
	}

	static String statusText(int totems) {
		return totems > 0 ? "AutoTotem: " + totems + (totems == 1 ? " Totem" : " Totems") : "AutoTotem: keine Totems!";
	}

	private static boolean isTotem(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
	}

	private static void click(Minecraft mc, int menuSlot) {
		Object gameMode = gameMode(mc);
		if (gameMode == null || !prepare()) {
			enabled = false;
			Chat.info(mc, "AutoTotem wurde abgeschaltet (Inventar-Klick nicht möglich).");
			return;
		}
		try {
			// Inventar-Menü hat immer die Nummer 0
			clickMethod.invoke(gameMode, 0, menuSlot, OFFHAND_BUTTON, swapConstant, mc.player);
		} catch (ReflectiveOperationException | RuntimeException e) {
			MacroKitClient.LOGGER.error("AutoTotem abgeschaltet", e);
			enabled = false;
			Chat.info(mc, "AutoTotem wurde abgeschaltet (Fehler beim Klicken, siehe Log).");
		}
	}

	/** Das Objekt, über das das Spiel Inventar-Klicks an den Server schickt (Feld oder Getter in Minecraft). */
	private static Object gameMode(Minecraft mc) {
		try {
			if (gameModeField != null) {
				return gameModeField.get(mc);
			}
			if (gameModeGetter != null) {
				return gameModeGetter.invoke(mc);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			// fällt auf null zurück
		}
		return null;
	}

	/** Sucht das Feld (oder die Methode) in Minecraft, die das Spielmodus-Objekt liefert, und gibt dessen Klasse zurück. */
	private static Class<?> findGameModeClass() {
		for (Field f : Minecraft.class.getDeclaredFields()) {
			if (f.getType().getName().endsWith(".MultiPlayerGameMode")) {
				f.setAccessible(true);
				gameModeField = f;
				return f.getType();
			}
		}
		for (Method m : Minecraft.class.getMethods()) {
			if (m.getParameterCount() == 0 && m.getReturnType().getName().endsWith(".MultiPlayerGameMode")) {
				gameModeGetter = m;
				return m.getReturnType();
			}
		}
		return null;
	}

	/** Sucht die Klick-Methode: (int Menü, int Slot, int Taste, Enum Klick-Art mit SWAP, Spieler). */
	private static boolean prepare() {
		if (!searched) {
			searched = true;
			try {
				Class<?> gameModeClass = findGameModeClass();
				for (Method m : gameModeClass == null ? new Method[0] : gameModeClass.getMethods()) {
					Class<?>[] p = m.getParameterTypes();
					if (p.length != 5 || p[0] != int.class || p[1] != int.class || p[2] != int.class || !p[3].isEnum()
						|| !p[4].isAssignableFrom(LocalPlayer.class)) {
						continue;
					}
					for (Object constant : p[3].getEnumConstants()) {
						if (constant instanceof Enum<?> e && e.name().equals("SWAP")) {
							clickMethod = m;
							swapConstant = constant;
							break;
						}
					}
					if (clickMethod != null) {
						break;
					}
				}
			} catch (RuntimeException e) {
				MacroKitClient.LOGGER.error("AutoTotem: Suche nach der Klick-Methode fehlgeschlagen", e);
			}
			if (clickMethod == null) {
				MacroKitClient.LOGGER.error("Inventar-Klick (Tausch) nicht gefunden");
			}
		}
		return clickMethod != null;
	}
}
