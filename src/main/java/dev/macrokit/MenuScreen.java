package dev.macrokit;

import dev.macrokit.xray.XrayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hauptmenü der Mod (Standardtaste: rechte Umschalttaste). */
final class MenuScreen extends Screen {
	private final Screen parent;

	MenuScreen(Screen parent) {
		super(Component.literal("MacroKit"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 - 76;
		this.addRenderableWidget(Button.builder(Component.literal("Macros"),
			b -> Minecraft.getInstance().gui.setScreen(new MacrosScreen(this, 0)))
			.bounds(cx - 100, y, 98, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Xray"),
			b -> Minecraft.getInstance().gui.setScreen(new XrayScreen(this)))
			.bounds(cx + 2, y, 98, 20).build());
		// Freunde: Anfragen, Freigaben (Leuchten, Koordinaten, Dimension)
		this.addRenderableWidget(Button.builder(Component.literal("Freunde"),
			b -> Minecraft.getInstance().gui.setScreen(new FriendsScreen(this, 0)))
			.bounds(cx - 100, y + 24, 200, 20).build());
		// Fast Right Click: Schalter und Zahl der Klicks pro Klick (1, 2, 4, 8, 16, 32, 64)
		this.addRenderableWidget(Button.builder(fastLabel(), b -> {
			FastClick.toggle(Minecraft.getInstance());
			b.setMessage(fastLabel());
		}).bounds(cx - 100, y + 48, 130, 20).build());
		this.addRenderableWidget(Button.builder(countLabel(), b -> {
			int c = MacroStore.fastClickCount();
			MacroStore.setFastClickCount(c >= MacroStore.FAST_CLICK_MAX ? 1 : c * 2);
			b.setMessage(countLabel());
		}).bounds(cx + 34, y + 48, 66, 20).build());
		// Halten: so viele Klicks pro Tick, solange die Rechtsklick-Taste (oder die Dauerklick-Taste) gehalten wird
		this.addRenderableWidget(Button.builder(holdLabel(), b -> {
			MacroStore.setFastClickHold(nextHold(MacroStore.fastClickHold()));
			b.setMessage(holdLabel());
		}).bounds(cx - 100, y + 72, 200, 20).build());
		// Criticals: jeder Schlag wird für den Server ein kritischer Treffer
		this.addRenderableWidget(Button.builder(criticalsLabel(), b -> {
			Criticals.toggle(Minecraft.getInstance());
			b.setMessage(criticalsLabel());
		}).bounds(cx - 100, y + 96, 200, 20).build());
		// AutoTotem: Schalter und Pause in Ticks (0, 1, 2, 3, 5, 10, 20)
		this.addRenderableWidget(Button.builder(totemLabel(), b -> {
			AutoTotem.toggle(Minecraft.getInstance());
			b.setMessage(totemLabel());
		}).bounds(cx - 100, y + 120, 130, 20).build());
		this.addRenderableWidget(Button.builder(delayLabel(), b -> {
			MacroStore.setAutoTotemDelay(nextDelay(MacroStore.autoTotemDelay()));
			b.setMessage(delayLabel());
		}).bounds(cx + 34, y + 120, 66, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Schließen"), b -> this.onClose())
			.bounds(cx - 100, y + 148, 200, 20).build());
	}

	private static Component fastLabel() {
		return Component.literal("Fast Right Click: " + (FastClick.enabled() ? "AN" : "AUS"));
	}

	/** Durchlauf der Halten-Werte: aus, 10, 50, 100, 200, wieder aus. */
	private static int nextHold(int current) {
		if (current <= 0) {
			return 10;
		}
		if (current < 50) {
			return 50;
		}
		if (current < 100) {
			return 100;
		}
		if (current < MacroStore.FAST_HOLD_MAX) {
			return MacroStore.FAST_HOLD_MAX;
		}
		return 0;
	}

	private static Component holdLabel() {
		int h = MacroStore.fastClickHold();
		return Component.literal("Halten: " + (h == 0 ? "AUS" : h + " Klicks pro Tick"));
	}

	private static Component criticalsLabel() {
		return Component.literal("Criticals: " + (Criticals.enabled() ? "AN" : "AUS"));
	}

	private static Component totemLabel() {
		return Component.literal("AutoTotem: " + (AutoTotem.enabled() ? "AN" : "AUS"));
	}

	private static Component delayLabel() {
		return Component.literal("Pause: " + MacroStore.autoTotemDelay());
	}

	/** Durchlauf der Pause-Werte: 0, 1, 2, 3, 5, 10, 20, wieder 0. */
	private static int nextDelay(int current) {
		int[] steps = {0, 1, 2, 3, 5, 10, MacroStore.AUTO_TOTEM_MAX_DELAY};
		for (int s : steps) {
			if (s > current) {
				return s;
			}
		}
		return 0;
	}

	private static Component countLabel() {
		return Component.literal("Klicks: " + MacroStore.fastClickCount());
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int top = this.height / 2 - 76;
		g.text(this.font, "MacroKit", cx - this.font.width("MacroKit") / 2, top - 30, 0xFFFFFFFF, true);

		String state;
		if (Recorder.isRecording()) {
			state = "Aufnahme läuft: " + Recorder.currentName();
		} else if (MacroPlayer.isActive()) {
			state = "Macro läuft: " + MacroPlayer.currentName();
		} else {
			state = "Macros: bereit";
		}
		g.text(this.font, state, cx - this.font.width(state) / 2, top - 16, 0xFFAAAAAA, true);

		String xray = XrayManager.status();
		g.text(this.font, xray, cx - this.font.width(xray) / 2, top + 174, 0xFFAAAAAA, true);
		String tip = "Tasten für die Schalter: Optionen > Steuerung > MacroKit";
		g.text(this.font, tip, cx - this.font.width(tip) / 2, top + 186, 0xFF888888, true);
	}
}
