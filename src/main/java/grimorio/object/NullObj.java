package grimorio.object;

/** El único valor {@code vacio}. */
public final class NullObj implements Obj {
    public static final NullObj INSTANCE = new NullObj();

    private NullObj() {}

    @Override public ObjType type() { return ObjType.NULL; }
    @Override public String inspect() { return "vacio"; }
}
