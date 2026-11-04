package grimorio;

import grimorio.token.Token;

/**
 * Encabezados temáticos de los tres tipos de error. Cada mensaje dice en qué
 * etapa se detectó el error, qué pasó y, si se conoce, la línea y la columna:
 *
 * <pre>
 *   Runa ilegible: carácter '@' no reconocido (línea 1, columna 11)
 * </pre>
 */
public final class Messages {
    public static final String LEXICAL = "Runa ilegible";
    public static final String SYNTAX = "Conjuro mal pronunciado, vaya a estudiar latín";
    public static final String RUNTIME = "El hechizo salió mal, acabas de abrir un portal interdimensional";

    private Messages() {}

    public static String format(String header, String detail, Token at) {
        return at == null ? header + ": " + detail : header + ": " + detail + " " + at.position();
    }
}
