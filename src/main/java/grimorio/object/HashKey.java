package grimorio.object;

import java.util.Objects;

/**
 * Clave interna de un diccionario. Dos valores distintos en memoria pero
 * iguales en contenido (dos textos "a") producen la misma HashKey.
 */
public final class HashKey {
    private final ObjType type;
    private final String value;

    public HashKey(ObjType type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof HashKey)) return false;
        HashKey key = (HashKey) other;
        return type == key.type && value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }
}
