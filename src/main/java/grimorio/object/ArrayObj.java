package grimorio.object;

import java.util.List;
import java.util.stream.Collectors;

/** Lista mutable: se puede cambiar con lista[i] = x o alCaldero(lista, x). */
public final class ArrayObj implements Obj {
    public final List<Obj> elements;

    public ArrayObj(List<Obj> elements) {
        this.elements = elements;
    }

    @Override public ObjType type() { return ObjType.ARRAY; }

    @Override
    public String inspect() {
        return "[" + elements.stream().map(Obj::repr).collect(Collectors.joining(", ")) + "]";
    }
}
