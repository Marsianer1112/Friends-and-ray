package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Freundesliste: Anfragen annehmen oder ablehnen, Freund öffnen, neuen Freund hinzufügen. */
final class FriendsScreen extends Screen {
	private static final int ROWS = 6;

	private final Screen parent;
	private final int page;

	FriendsScreen(Screen parent, int page) {
		super(Component.literal("MacroKit – Freunde"));
		this.parent = parent;
		this.page = page;
	}

	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		int cx = this.width / 2;
		int left = cx - 150;

		List<Friends.Friend> list = Friends.list();
		int pages = Math.max(1, (list.size() + ROWS - 1) / ROWS);
		int current = Math.min(this.page, pages - 1);
		int y = 30;
		for (int i = current * ROWS; i < Math.min(list.size(), (current + 1) * ROWS); i++) {
			Friends.Friend f = list.get(i);
			String name = f.name;
			String label = name + " – " + Friends.statusText(f);
			if (Friends.IN.equals(f.state)) {
				this.addRenderableWidget(Button.builder(Component.literal(label),
					b -> mc.gui.setScreen(new FriendScreen(this, name))).bounds(left, y, 168, 20).build());
				this.addRenderableWidget(Button.builder(Component.literal("Annehmen"), b -> {
					Chat.infoIfError(mc, Friends.accept(mc, name));
					mc.gui.setScreen(new FriendsScreen(this.parent, current));
				}).bounds(left + 170, y, 64, 20).build());
				this.addRenderableWidget(Button.builder(Component.literal("Ablehnen"), b -> {
					Chat.infoIfError(mc, Friends.decline(mc, name));
					mc.gui.setScreen(new FriendsScreen(this.parent, current));
				}).bounds(left + 236, y, 64, 20).build());
			} else {
				this.addRenderableWidget(Button.builder(Component.literal(label),
					b -> mc.gui.setScreen(new FriendScreen(this, name))).bounds(left, y, 300, 20).build());
			}
			y += 24;
		}

		int rowY = 30 + ROWS * 24 + 4;
		this.addRenderableWidget(Button.builder(Component.literal("Freund hinzufügen"),
			b -> mc.gui.setScreen(new FriendAddScreen(this, 0))).bounds(left, rowY, 98, 20).build());
		this.addRenderableWidget(Button.builder(requestsLabel(), b -> {
			Friends.setAcceptRequests(!Friends.acceptRequests());
			b.setMessage(requestsLabel());
		}).bounds(left + 101, rowY, 98, 20).build());
		this.addRenderableWidget(Button.builder(hudLabel(), b -> {
			Friends.setHud(!Friends.hud());
			b.setMessage(hudLabel());
		}).bounds(left + 202, rowY, 98, 20).build());

		int navY = rowY + 24;
		Button prev = Button.builder(Component.literal("<"),
			b -> mc.gui.setScreen(new FriendsScreen(this.parent, current - 1))).bounds(left, navY, 30, 20).build();
		prev.active = current > 0;
		this.addRenderableWidget(prev);
		Button next = Button.builder(Component.literal(">"),
			b -> mc.gui.setScreen(new FriendsScreen(this.parent, current + 1))).bounds(left + 270, navY, 30, 20).build();
		next.active = current < pages - 1;
		this.addRenderableWidget(next);
		this.addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> this.onClose())
			.bounds(cx - 50, navY, 100, 20).build());
	}

	Screen parentScreen() {
		return this.parent;
	}

	private static Component requestsLabel() {
		return Component.literal("Anfragen: " + (Friends.acceptRequests() ? "AN" : "AUS"));
	}

	private static Component hudLabel() {
		return Component.literal("Liste: " + (Friends.hud() ? "AN" : "AUS"));
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		g.text(this.font, "Freunde", cx - this.font.width("Freunde") / 2, 10, 0xFFFFFFFF, true);
		if (Friends.list().isEmpty()) {
			String hint = "Noch keine Freunde. Beide brauchen die Mod. Hinzufügen: Knopf unten oder /macro friend add <name>";
			g.text(this.font, hint, cx - this.font.width(hint) / 2, 40, 0xFFAAAAAA, true);
		}
		int y = 30 + ROWS * 24 + 4 + 24 + 26;
		String[] hints = {
			"Jeder Freund legt selbst fest, was du von ihm siehst. Standard: nichts.",
			FriendNet.hooked() ? "Nachrichten laufen als /msg von Mod zu Mod (im Server-Log sichtbar)."
				: "Chat-Empfang geht auf dieser Version nicht: Anfragen kommen nicht an.",
			FriendNet.presenceKnown() ? "Spieler auf dem Server: " + FriendNet.onlineNames().size()
				: "Spielerliste noch unbekannt (nur auf einem Server)."
		};
		for (int i = 0; i < hints.length; i++) {
			g.text(this.font, hints[i], cx - this.font.width(hints[i]) / 2, y + i * 11, 0xFF888888, true);
		}
	}
}
