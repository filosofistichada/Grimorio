package grimorio.ast;

import grimorio.token.Token;

/** Acceso por índice o clave: {@code lista[0]}, {@code libro["titulo"]}. */
public final class IndexExpression implements Expression {
    public final Token token;
    public final Expression left;
    public final Expression index;

    public IndexExpression(Token token, Expression left, Expression index) {
        this.token = token;
        this.left = left;
        this.index = index;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "(" + left + "[" + index + "])";
    }
}
