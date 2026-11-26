package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ein Freund im Detail: was du ihm erlaubst, was er dir erlaubt, und was du davon anzeigen willst.
 * Freigaben gelten sofort und lassen sich jederzeit zurücknehmen.
 */
final class FriendScreen extends Screen {
	private final Screen parent;
	private final String name;
	private boolean confirmRemove;

	FriendScreen(Screen parent, String name) {
		super(Component.literal("MacroKit – " + name));
		this.parent = parent;
		this.name = name;
	}

	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		int cx = this.width / 2;
		int left = cx - 150;
		Friends.Friend f = Friends.find(this.name);
		if (f == null) {
			this.addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> this.onClose())
				.bounds(cx - 50, 40, 100, 20).build());
			return;
		}

		if (Friends.IN.equals(f.state)) {
			this.addRenderableWidget(Button.builder(Component.literal("Annehmen"), b -> {
				Chat.infoIfError(mc, Friends.accept(mc, this.name));
				mc.gui.setScreen(new FriendScreen(this.parent, this.name));
			}).bounds(left, 50, 148, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("Ablehnen"), b -> {
				Chat.infoIfError(mc, Friends.decline(mc, this.name));
				this.onClose();
			}).bounds(left + 152, 50, 148, 20).build());
		} else if (Friends.OUT.equals(f.state)) {
			this.addRenderableWidget(Button.builder(Component.literal("Anfrage zurückziehen"), b -> {
				Chat.infoIfError(mc, Friends.remove(mc, this.name));
				this.onClose();
			}).bounds(left, 50, 300, 20).build());
		} else {
			// Du erlaubst ihm ...
			addGive(mc, left, 64, "Leuchten", Friends.GLOW);
			addGive(mc, left + 101, 64, "Koordinaten", Friends.COORDS);
			addGive(mc, left + 202, 64, "Dimension", Friends.DIM);
			// Meine Anzeige
			Button glow = Button.builder(showLabel("Leuchten zeigen", f.showGlow), b -> {
				Friends.Friend now = Friends.find(this.name);
				if (now != null) {
					Friends.setShowGlow(this.name, !now.showGlow);
					b.setMessage(showLabel("Leuchten zeigen", !now.showGlow));
				}
			}).bounds(left, 136, 148, 20).build();
			glow.active = (f.got & Friends.GLOW) != 0;
			this.addRenderableWidget(glow);
			Button marker = Button.builder(showLabel("Marker zeigen", f.showMarker), b -> {
				Friends.Friend now = Friends.find(this.name);
				if (now != null) {
					Friends.setShowMarker(this.name, !now.showMarker);
					b.setMessage(showLabel("Marker zeigen", !now.showMarker));
				}
			}).bounds(left + 152, 136, 148, 20).build();
			marker.active = (f.got & Friends.COORDS) != 0 && (f.got & Friends.DIM) != 0;
			this.addRenderableWidget(marker);
		}

		this.addRenderableWidget(Button.builder(Component.literal(this.confirmRemove ? "Sicher?" : "Freund entfernen"), b -> {
			if (this.confirmRemove) {
				Chat.infoIfError(mc, Friends.remove(mc, this.name));
				this.onClose();
			} else {
				this.confirmRemove = true;
				b.setMessage(Component.literal("Sicher?"));
			}
		}).bounds(left, 164, 148, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> this.onClose())
			.bounds(left + 152, 164, 148, 20).build());
	}

	private void addGive(Minecraft mc, int x, int y, String label, int bit) {
		Friends.Friend f = Friends.find(this.name);
		boolean on = f != null && (f.give & bit) != 0;
		this.addRenderableWidget(Button.builder(showLabel(label, on), b -> {
			Friends.Friend now = Friends.find(this.name);
			if (now == null) {
				return;
			}
			boolean next = (now.give & bit) == 0;
			Chat.infoIfError(mc, Friends.setGive(mc, this.name, bit, next));
			b.setMessage(showLabel(label, next));
		}).bounds(x, y, 98, 20).build());
	}

	private static Component showLabel(String label, boolean on) {
		return Component.literal(label + ": " + (on ? "AN" : "AUS"));
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		int left = cx - 150;
		g.text(this.font, this.name, cx - this.font.width(this.name) / 2, 10, 0xFFFFFFFF, true);
		Friends.Friend f = Friends.find(this.name);
		if (f == null) {
			center(g, "Nicht mehr in deiner Liste.", 24, 0xFFAAAAAA);
			return;
		}
		center(g, Friends.statusText(f), 22, f.online ? 0xFF7CFC9A : 0xFFAAAAAA);
		if (Friends.IN.equals(f.state)) {
			center(g, "Er möchte dein Freund sein. Es wird erst etwas geteilt, wenn du Freigaben setzt.", 78, 0xFF888888);
			return;
		}
		if (Friends.OUT.equals(f.state)) {
			center(g, "Warte auf die Antwort. Er braucht die MacroKit-Mod.", 78, 0xFF888888);
			return;
		}
		g.text(this.font, "Du erlaubst " + f.name + " zu sehen:", left, 52, 0xFFFFE070, true);
		g.text(this.font, f.name + " erlaubt dir: " + Friends.describeGot(f.got), left, 92, 0xFFFFE070, true);
		Friends.Position p = f.pos;
		String coords = (f.got & Friends.COORDS) == 0 ? "nicht freigegeben"
			: p != null && p.hasCoords() ? Math.round(p.x) + " " + Math.round(p.y) + " " + Math.round(p.z)
			: f.online ? "wartet auf Meldung" : "offline";
		String dim = (f.got & Friends.DIM) == 0 ? "nicht freigegeben"
			: p != null && p.dim != null ? Friends.dimLabel(p.dim) : f.online ? "wartet auf Meldung" : "offline";
		g.text(this.font, "Koordinaten: " + coords, left, 104, 0xFFCCCCCC, true);
		g.text(this.font, "Dimension: " + dim, left, 116, 0xFFCCCCCC, true);
		String note = "Leuchten: " + ((f.got & Friends.GLOW) == 0 ? "nicht freigegeben"
			: FriendsHud.glowSupported() ? "freigegeben (nur wenn er in deiner Nähe geladen ist)" : "freigegeben, geht aber auf dieser Version nicht");
		g.text(this.font, note, left, 128 + 38, 0xFF888888, true);
	}

	private void center(GuiGraphicsExtractor g, String text, int y, int color) {
		g.text(this.font, text, this.width / 2 - this.font.width(text) / 2, y, color, true);
	}
}
