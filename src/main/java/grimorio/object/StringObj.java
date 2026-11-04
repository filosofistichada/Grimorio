package grimorio.object;

public final class StringObj implements Obj, Hashable {
    public final String value;

    public StringObj(String value) {
        this.value = value;
    }

    @Override public ObjType type() { return ObjType.STRING; }
    @Override public String inspect() { return value; }
    @Override public String repr() { return "\"" + value + "\""; }
    @Override public HashKey hashKey() { return new HashKey(type(), value); }
}
