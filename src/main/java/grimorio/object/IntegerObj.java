package grimorio.object;

public final class IntegerObj implements Obj, Hashable {
    public final long value;

    public IntegerObj(long value) {
        this.value = value;
    }

    @Override public ObjType type() { return ObjType.INTEGER; }
    @Override public String inspect() { return Long.toString(value); }
    @Override public HashKey hashKey() { return new HashKey(type(), inspect()); }
}
