package grimorio.ast;

import grimorio.token.Token;

/**
 * {@code a && b} o {@code a || b}. Es un nodo aparte de {@link InfixExpression}
 * porque se evalúa en cortocircuito: el lado derecho solo se evalúa si hace falta.
 */
public final class LogicalExpression implements Expression {
    public final Token token;
    public final Expression left;
    public final String operator;
    public final Expression right;

    public LogicalExpression(Token token, Expression left, String operator, Expression right) {
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
