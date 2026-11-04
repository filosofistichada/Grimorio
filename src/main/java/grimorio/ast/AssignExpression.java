package grimorio.ast;

import grimorio.token.Token;

/**
 * Reasignación: {@code x = valor} o {@code lista[i] = valor}. El destino es un
 * {@link Identifier} o un {@link IndexExpression}; el parser rechaza cualquier otro.
 */
public final class AssignExpression implements Expression {
    public final Token token;
    public final Expression target;
    public final Expression value;

    public AssignExpression(Token token, Expression target, Expression value) {
        this.token = token;
        this.target = target;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "(" + target + " = " + value + ")";
    }
}
