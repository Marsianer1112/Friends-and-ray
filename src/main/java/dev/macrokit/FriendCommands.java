package dev.macrokit;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.List;

/** /macro friend ... */
final class FriendCommands {
	private static final SuggestionProvider<FabricClientCommandSource> ONLINE =
		(context, builder) -> SharedSuggestionProvider.suggest(FriendNet.onlineNames(), builder);
	private static final SuggestionProvider<FabricClientCommandSource> PEOPLE =
		(context, builder) -> SharedSuggestionProvider.suggest(names(), builder);

	private FriendCommands() {
	}

	private static List<String> names() {
		List<String> out = new ArrayList<>();
		for (Friends.Friend f : Friends.list()) {
			out.add(f.name);
		}
		return out;
	}

	/**
	 * <pre>
	 * /macro friend add|accept|decline|remove &lt;name&gt;
	 * /macro friend list
	 * /macro friend share &lt;name&gt; &lt;glow|coords|dim|all&gt; &lt;on|off&gt;   (was du ihm erlaubst)
	 * /macro friend show &lt;name&gt; &lt;glow|marker&gt; &lt;on|off&gt;            (was du von ihm anzeigst)
	 * /macro friend requests|hud &lt;on|off&gt;
	 * </pre>
	 */
	static LiteralArgumentBuilder<FabricClientCommandSource> node() {
		return ClientCommands.literal("friend")
			.then(ClientCommands.literal("add")
				.then(ClientCommands.argument("name", StringArgumentType.word()).suggests(ONLINE)
					.executes(ctx -> MacroCommands.result(ctx, Friends.add(MacroCommands.mc(), name(ctx))))))
			.then(ClientCommands.literal("accept")
				.then(ClientCommands.argument("name", StringArgumentType.word()).suggests(PEOPLE)
					.executes(ctx -> MacroCommands.result(ctx, Friends.accept(MacroCommands.mc(), name(ctx))))))
			.then(ClientCommands.literal("decline")
				.then(ClientCommands.argument("name", StringArgumentType.word()).suggests(PEOPLE)
					.executes(ctx -> MacroCommands.result(ctx, Friends.decline(MacroCommands.mc(), name(ctx))))))
			.then(ClientCommands.literal("remove")
				.then(ClientCommands.argument("name", StringArgumentType.word()).suggests(PEOPLE)
					.executes(ctx -> MacroCommands.result(ctx, Friends.remove(MacroCommands.mc(), name(ctx))))))
			.then(ClientCommands.literal("list").executes(FriendCommands::list))
			.then(share())
			.then(show())
			.then(ClientCommands.literal("requests")
				.then(ClientCommands.literal("on").executes(ctx -> {
					Friends.setAcceptRequests(true);
					MacroCommands.say(ctx, "Freundschaftsanfragen: werden angenommen (zur Auswahl angezeigt)");
					return 1;
				}))
				.then(ClientCommands.literal("off").executes(ctx -> {
					Friends.setAcceptRequests(false);
					MacroCommands.say(ctx, "Freundschaftsanfragen: werden ignoriert");
					return 1;
				})))
			.then(ClientCommands.literal("hud")
				.then(ClientCommands.literal("on").executes(ctx -> {
					Friends.setHud(true);
					MacroCommands.say(ctx, "Freundesliste oben rechts: AN");
					return 1;
				}))
				.then(ClientCommands.literal("off").executes(ctx -> {
					Friends.setHud(false);
					MacroCommands.say(ctx, "Freundesliste oben rechts: AUS");
					return 1;
				})));
	}

	private static String name(CommandContext<FabricClientCommandSource> ctx) {
		return StringArgumentType.getString(ctx, "name");
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> share() {
		RequiredArgumentBuilder<FabricClientCommandSource, String> who =
			ClientCommands.argument("name", StringArgumentType.word()).suggests(PEOPLE);
		who.then(shareBit("glow", Friends.GLOW));
		who.then(shareBit("coords", Friends.COORDS));
		who.then(shareBit("dim", Friends.DIM));
		who.then(shareBit("all", Friends.ALL));
		return ClientCommands.literal("share").then(who);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> shareBit(String word, int bits) {
		return ClientCommands.literal(word)
			.then(ClientCommands.literal("on").executes(ctx -> setGive(ctx, bits, true)))
			.then(ClientCommands.literal("off").executes(ctx -> setGive(ctx, bits, false)));
	}

	private static int setGive(CommandContext<FabricClientCommandSource> ctx, int bits, boolean on) {
		String name = name(ctx);
		String error = null;
		for (int bit : new int[] {Friends.GLOW, Friends.COORDS, Friends.DIM}) {
			if ((bits & bit) != 0 && error == null) {
				error = Friends.setGive(MacroCommands.mc(), name, bit, on);
			}
		}
		if (error == null) {
			Friends.Friend f = Friends.find(name);
			MacroCommands.say(ctx, name + " darf jetzt von dir sehen: " + Friends.describeGot(f == null ? 0 : f.give));
		}
		return MacroCommands.result(ctx, error);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> show() {
		RequiredArgumentBuilder<FabricClientCommandSource, String> who =
			ClientCommands.argument("name", StringArgumentType.word()).suggests(PEOPLE);
		who.then(ClientCommands.literal("glow")
			.then(ClientCommands.literal("on").executes(ctx -> showGlow(ctx, true)))
			.then(ClientCommands.literal("off").executes(ctx -> showGlow(ctx, false))));
		who.then(ClientCommands.literal("marker")
			.then(ClientCommands.literal("on").executes(ctx -> showMarker(ctx, true)))
			.then(ClientCommands.literal("off").executes(ctx -> showMarker(ctx, false))));
		return ClientCommands.literal("show").then(who);
	}

	private static int showGlow(CommandContext<FabricClientCommandSource> ctx, boolean on) {
		return MacroCommands.result(ctx, Friends.setShowGlow(name(ctx), on));
	}

	private static int showMarker(CommandContext<FabricClientCommandSource> ctx, boolean on) {
		return MacroCommands.result(ctx, Friends.setShowMarker(name(ctx), on));
	}

	private static int list(CommandContext<FabricClientCommandSource> ctx) {
		List<Friends.Friend> list = Friends.list();
		if (list.isEmpty()) {
			MacroCommands.say(ctx, "Noch keine Freunde. Hinzufügen: /macro friend add <name> (beide brauchen die Mod)");
			return 1;
		}
		MacroCommands.say(ctx, "Freunde (" + list.size() + "):");
		for (Friends.Friend f : list) {
			String line = "  " + f.name + " – " + Friends.statusText(f);
			if (Friends.FRIEND.equals(f.state)) {
				line += " | er erlaubt dir: " + Friends.describeGot(f.got) + " | du erlaubst ihm: " + Friends.describeGot(f.give);
			}
			MacroCommands.say(ctx, line);
		}
		return 1;
	}
}
