package grimorio.object;

import java.util.HashMap;
import java.util.Map;

/**
 * Ámbito de variables. Cada ámbito tiene sus propias variables y un enlace
 * al ámbito exterior ({@code outer}). Buscar una variable sube por la cadena
 * hasta encontrarla, por eso un bloque ve las variables de afuera pero
 * afuera no se ven las del bloque.
 *
 * <pre>
 *   global ← hechizo ← bloque del siProfecia
 * </pre>
 */
public final class Environment {
    private final Map<String, Obj> store = new HashMap<>();
    private final Environment outer;

    public Environment() {
        this(null);
    }

    public Environment(Environment outer) {
        this.outer = outer;
    }

    /** Busca la variable aquí y, si no está, en los ámbitos exteriores. Devuelve null si no existe. */
    public Obj get(String name) {
        Obj value = store.get(name);
        if (value == null && outer != null) return outer.get(name);
        return value;
    }

    /** {@code pocion x = ...}: crea (o reemplaza) la variable en ESTE ámbito, aunque exista afuera. */
    public void define(String name, Obj value) {
        store.put(name, value);
    }

    /** {@code x = ...}: cambia la variable en el ámbito más cercano donde exista. Devuelve false si no existe. */
    public boolean assign(String name, Obj value) {
        if (store.containsKey(name)) {
            store.put(name, value);
            return true;
        }
        return outer != null && outer.assign(name, value);
    }
}
