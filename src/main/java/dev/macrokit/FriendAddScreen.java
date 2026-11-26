package dev.macrokit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Auswahl eines Spielers vom Server, dem eine Freundschaftsanfrage geschickt wird. */
final class FriendAddScreen extends Screen {
	private static final int ROWS = 7;

	private final Screen parent;
	private final int page;

	FriendAddScreen(Screen parent, int page) {
		super(Component.literal("MacroKit – Freund hinzufügen"));
		this.parent = parent;
		this.page = page;
	}

	/** Spieler auf dem Server, die noch nicht in der Liste stehen. */
	private static List<String> candidates() {
		List<String> out = new ArrayList<>();
		for (String n : FriendNet.onlineNames()) {
			if (Friends.find(n) == null) {
				out.add(n);
			}
		}
		return out;
	}

	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		int cx = this.width / 2;
		List<String> names = candidates();
		int pages = Math.max(1, (names.size() + ROWS - 1) / ROWS);
		int current = Math.min(this.page, pages - 1);
		int y = 30;
		for (int i = current * ROWS; i < Math.min(names.size(), (current + 1) * ROWS); i++) {
			String name = names.get(i);
			this.addRenderableWidget(Button.builder(Component.literal(name + " – Anfrage senden"), b -> {
				Chat.infoIfError(mc, Friends.add(mc, name));
				mc.gui.setScreen(this.parent instanceof FriendsScreen ? new FriendsScreen(((FriendsScreen) this.parent).parentScreen(), 0)
					: this.parent);
			}).bounds(cx - 100, y, 200, 20).build());
			y += 24;
		}
		int navY = 30 + ROWS * 24 + 4;
		Button prev = Button.builder(Component.literal("<"),
			b -> mc.gui.setScreen(new FriendAddScreen(this.parent, current - 1))).bounds(cx - 100, navY, 30, 20).build();
		prev.active = current > 0;
		this.addRenderableWidget(prev);
		Button next = Button.builder(Component.literal(">"),
			b -> mc.gui.setScreen(new FriendAddScreen(this.parent, current + 1))).bounds(cx + 70, navY, 30, 20).build();
		next.active = current < pages - 1;
		this.addRenderableWidget(next);
		this.addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> this.onClose())
			.bounds(cx - 50, navY, 100, 20).build());
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int cx = this.width / 2;
		g.text(this.font, "Freund hinzufügen", cx - this.font.width("Freund hinzufügen") / 2, 10, 0xFFFFFFFF, true);
		String hint;
		if (!FriendNet.presenceKnown()) {
			hint = "Die Spielerliste ist nicht lesbar. Nutze /macro friend add <name>.";
		} else if (candidates().isEmpty()) {
			hint = "Keine weiteren Spieler online.";
		} else {
			hint = "Der Spieler braucht auch die Mod und muss die Anfrage annehmen.";
		}
		g.text(this.font, hint, cx - this.font.width(hint) / 2, 30 + ROWS * 24 + 4 + 26, 0xFF888888, true);
	}
}
