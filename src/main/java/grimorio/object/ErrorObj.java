package grimorio.object;

/**
 * Error de ejecución. Como ReturnValue, sube por el árbol deteniendo la
 * ejecución hasta llegar al programa, que lo muestra.
 */
public final class ErrorObj implements Obj {
    public final String message;

    public ErrorObj(String message) {
        this.message = message;
    }

    @Override public ObjType type() { return ObjType.ERROR; }
    @Override public String inspect() { return message; }
}
