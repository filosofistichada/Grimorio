package grimorio.ast;

import grimorio.token.Token;

/**
 * Nodo del árbol de sintaxis abstracta (AST). Cada nodo guarda el token que
 * lo originó, que da su posición en el código para los mensajes de error.
 *
 * {@code toString()} reconstruye el código con paréntesis explícitos, lo que
 * deja ver cómo el parser aplicó la precedencia: {@code 1 + 2 * 3} se
 * muestra como {@code (1 + (2 * 3))}.
 */
public interface Node {
    Token token();
}
