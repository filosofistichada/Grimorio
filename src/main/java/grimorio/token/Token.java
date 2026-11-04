package grimorio.token;

/**
 * Unidad mínima que produce el lexer: su tipo, el texto exacto que se leyó
 * y la posición (línea y columna, desde 1) donde empieza en el código fuente.
 *
 * La posición viaja con el token hasta el AST, y así el parser y el
 * evaluador pueden decir en qué parte del programa ocurrió un error.
 */
public final class Token {
    private final TokenType type;
    private final String literal;
    private final int line;
    private final int column;

    public Token(TokenType type, String literal, int line, int column) {
        this.type = type;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    public TokenType type() { return type; }
    public String literal() { return literal; }
    public int line() { return line; }
    public int column() { return column; }

    /** Texto "(línea L, columna C)" para anexar a los mensajes de error. */
    public String position() {
        return "(línea " + line + ", columna " + column + ")";
    }

    @Override
    public String toString() {
        return String.format("%d:%d\t%-10s %s", line, column, type.name(), literal);
    }
}
