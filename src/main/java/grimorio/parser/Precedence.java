package grimorio.parser;

/**
 * Niveles de precedencia, de menor a mayor. Un operador con más precedencia
 * "agarra" a sus operandos antes: en {@code 1 + 2 * 3} el {@code *} gana y
 * el resultado es {@code 1 + (2 * 3)}.
 *
 * POWER va por encima de PREFIX para que {@code -2 ** 2} sea {@code -(2 ** 2)},
 * como en matemáticas.
 */
public enum Precedence {
    LOWEST,
    ASSIGN,      // =            (asociativo a la derecha)
    OR,          // ||
    AND,         // &&
    EQUALS,      // == !=
    LESSGREATER, // < > <= >=
    SUM,         // + -
    PRODUCT,     // * / %
    PREFIX,      // -x !x
    POWER,       // **           (asociativo a la derecha)
    CALL,        // hechizo(x)
    INDEX        // lista[i]
}
