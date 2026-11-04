package grimorio.ast;

import grimorio.token.Token;

/** Operador antes de su operando: {@code -x} o {@code !listo}. */
public final class PrefixExpression implements Expression {
    public final Token token;
    public final String operator;
    public final Expression right;

    public PrefixExpression(Token token, String operator, Expression right) {
        this.token = token;
        this.operator = operator;
        this.right = right;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "(" + operator + right + ")";
    }
}
