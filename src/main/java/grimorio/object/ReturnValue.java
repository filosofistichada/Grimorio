package grimorio.object;

/**
 * Envoltorio que marca un valor como "retornado con conjura". El evaluador lo
 * deja subir por los bloques y ciclos sin seguir ejecutando, y lo desenvuelve
 * al llegar al llamado del hechizo.
 */
public final class ReturnValue implements Obj {
    public final Obj value;

    public ReturnValue(Obj value) {
        this.value = value;
    }

    @Override public ObjType type() { return ObjType.RETURN_VALUE; }
    @Override public String inspect() { return value.inspect(); }
}
