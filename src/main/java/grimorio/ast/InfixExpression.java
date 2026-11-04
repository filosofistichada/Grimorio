package grimorio.ast;

import grimorio.token.Token;

/** Operador binario aritmético o de comparación: {@code a + b}, {@code a <= b}. */
public final class InfixExpression implements Expression {
    public final Token token;
    public final Expression left;
    public final String operator;
    public final Expression right;

    public InfixExpression(Token token, Expression left, String operator, Expression right) {
        this.token = token;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}
