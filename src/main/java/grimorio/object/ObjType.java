package grimorio.object;

/** Tipos de valor en tiempo de ejecución, con el nombre que se muestra al usuario. */
public enum ObjType {
    INTEGER("entero"),
    FLOAT("decimal"),
    STRING("texto"),
    BOOLEAN("booleano"),
    NULL("vacio"),
    ARRAY("lista"),
    HASH("diccionario"),
    FUNCTION("hechizo"),
    BUILTIN("hechizo integrado"),
    RETURN_VALUE("retorno"),
    ERROR("error");

    private final String displayName;

    ObjType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
