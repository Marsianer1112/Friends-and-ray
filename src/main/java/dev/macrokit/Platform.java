package dev.macrokit;

/**
 * Unterschiede zwischen den Minecraft-Versionen. Diese Datei ist in der Version für 26.2 und der für 26.3 verschieden,
 * der übrige Code ist gleich.
 */
final class Platform {
	/** true ab Minecraft 26.3: Die Eingabe läuft über SDL statt GLFW. */
	static final boolean SDL = false;

	private Platform() {
	}
}
