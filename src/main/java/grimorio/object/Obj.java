package grimorio.object;

/**
 * Valor que produce el evaluador al ejecutar el programa. Se llama Obj (y no
 * Object) para no chocar con {@code java.lang.Object}.
 */
public interface Obj {
    ObjType type();

    /** Texto que muestra revelar() y el REPL. */
    String inspect();

    /** Texto del valor dentro de una lista o diccionario (los textos van entre comillas). */
    default String repr() {
        return inspect();
    }
}
