package grimorio.object;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Diccionario. Conserva el orden de inserción para que se imprima igual que se escribió. */
public final class HashObj implements Obj {
    /** Par clave-valor; se guarda la clave original para poder mostrarla. */
    public static final class Pair {
        public final Obj key;
        public final Obj value;

        public Pair(Obj key, Obj value) {
            this.key = key;
            this.value = value;
        }
    }

    public final Map<HashKey, Pair> pairs = new LinkedHashMap<>();

    public void put(Hashable key, Obj value) {
        pairs.put(key.hashKey(), new Pair((Obj) key, value));
    }

    @Override public ObjType type() { return ObjType.HASH; }

    @Override
    public String inspect() {
        return "{" + pairs.values().stream()
                .map(p -> p.key.repr() + ": " + p.value.repr())
                .collect(Collectors.joining(", ")) + "}";
    }
}
