package grimorio.object;

public final class FloatObj implements Obj, Hashable {
    public final double value;

    public FloatObj(double value) {
        this.value = value;
    }

    @Override public ObjType type() { return ObjType.FLOAT; }
    @Override public String inspect() { return Double.toString(value); }
    @Override public HashKey hashKey() { return new HashKey(type(), inspect()); }
}
