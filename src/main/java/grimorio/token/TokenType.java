package grimorio.token;

import java.util.HashMap;
import java.util.Map;

/**
 * Tipos de token del lenguaje Grimorio: las "palabras" que el lexer reconoce
 * y que el parser usa para decidir qué construcción está leyendo.
 *
 * Cada tipo guarda una descripción legible que se usa en los mensajes de
 * error ("se esperaba ')'") y en el modo lexer del REPL.
 */
public enum TokenType {
    // Especiales
    ILLEGAL("ILEGAL"),
    EOF("fin del archivo"),

    // Identificadores y literales
    IDENT("identificador"),
    INT("entero"),
    FLOAT("decimal"),
    STRING("texto"),

    // Operadores
    ASSIGN("="),
    PLUS("+"),
    MINUS("-"),
    ASTERISK("*"),
    SLASH("/"),
    PERCENT("%"),
    POWER("**"),
    BANG("!"),
    AND("&&"),
    OR("||"),

    // Comparación
    EQ("=="),
    NOT_EQ("!="),
    LT("<"),
    GT(">"),
    LTE("<="),
    GTE(">="),

    // Delimitadores
    COMMA(","),
    SEMICOLON(";"),
    COLON(":"),
    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LBRACKET("["),
    RBRACKET("]"),

    // Palabras reservadas
    LET("pocion"),
    FUNCTION("hechizo"),
    RETURN("conjura"),
    IF("siProfecia"),
    ELIF("otraProfecia"),
    ELSE("maldicion"),
    WHILE("amarre"),
    FOR("circulo"),
    TRUE("luz"),
    FALSE("sombra"),
    NULL("vacio");

    private final String description;

    TokenType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /**
     * Palabras reservadas del lenguaje. El lexer lee una palabra completa
     * (letras, dígitos y _) y luego la busca aquí: si está, es reservada;
     * si no, es un identificador. Para agregar una palabra reservada basta
     * con declarar su constante arriba y añadirla a este mapa.
     */
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        for (TokenType type : new TokenType[] {LET, FUNCTION, RETURN, IF, ELIF, ELSE, WHILE, FOR, TRUE, FALSE, NULL}) {
            KEYWORDS.put(type.description, type);
        }
    }

    /** Devuelve el tipo de palabra reservada de {@code word}, o IDENT si no es reservada. */
    public static TokenType lookupIdent(String word) {
        return KEYWORDS.getOrDefault(word, IDENT);
    }
}
