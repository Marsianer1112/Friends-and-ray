package dev.macrokit;

import java.lang.reflect.Method;

/** Kleine Hilfe für Reflection: ruft öffentliche Methoden ohne Parameter auf, ohne vom genauen Namen abzuhängen. */
final class Refl {
	private Refl() {
	}

	/** Ruft die erste vorhandene Methode aus den Namen auf. Gibt null zurück, wenn es keine gibt oder sie fehlschlägt. */
	static Object call(Object target, String... names) {
		if (target == null) {
			return null;
		}
		for (String name : names) {
			try {
				Method m = target.getClass().getMethod(name);
				try {
					m.setAccessible(true);
				} catch (RuntimeException ignored) {
					// öffentliche Methoden gehen meistens auch ohne
				}
				return m.invoke(target);
			} catch (ReflectiveOperationException | RuntimeException ignored) {
				// nächsten Namen probieren
			}
		}
		return null;
	}
}
