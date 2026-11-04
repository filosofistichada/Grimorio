package grimorio.object;

/** Solo existen dos booleanos; se comparten para poder compararlos por referencia. */
public final class BooleanObj implements Obj, Hashable {
    public static final BooleanObj TRUE = new BooleanObj(true);
    public static final BooleanObj FALSE = new BooleanObj(false);

    public final boolean value;

    private BooleanObj(boolean value) {
        this.value = value;
    }

    public static BooleanObj of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override public ObjType type() { return ObjType.BOOLEAN; }
    @Override public String inspect() { return value ? "luz" : "sombra"; }
    @Override public HashKey hashKey() { return new HashKey(type(), inspect()); }
}
