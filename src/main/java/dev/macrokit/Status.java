package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Kleine Anzeige oben links für alle eingeschalteten Funktionen (Fast Right Click, AutoTotem, Criticals). */
final class Status {
	private static final int COLOR_ON = 0xFFFFE070;
	private static final int COLOR_OK = 0xFF7CFC9A;
	private static final int COLOR_WARN = 0xFFFF5555;

	private Status() {
	}

	static void draw(GuiGraphicsExtractor g, Minecraft mc) {
		if (mc.player == null || mc.font == null) {
			return;
		}
		int y = 4;
		String fast = FastClick.statusText();
		if (fast != null) {
			y = line(g, mc, y, fast, COLOR_ON);
		}
		if (AutoTotem.enabled()) {
			int totems = AutoTotem.count(mc);
			y = line(g, mc, y, AutoTotem.statusText(totems), totems > 0 ? COLOR_OK : COLOR_WARN);
		}
		String crit = Criticals.statusText();
		if (crit != null) {
			line(g, mc, y, crit, COLOR_ON);
		}
	}

	private static int line(GuiGraphicsExtractor g, Minecraft mc, int y, String text, int color) {
		g.text(mc.font, text, 4, y, color, true);
		return y + 10;
	}
}
