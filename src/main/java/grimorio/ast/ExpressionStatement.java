package grimorio.ast;

import grimorio.token.Token;

/** Una expresión usada como instrucción, por ejemplo {@code revelar(x);} o {@code x = 3;}. */
public final class ExpressionStatement implements Statement {
    public final Token token;
    public final Expression expression;

    public ExpressionStatement(Token token, Expression expression) {
        this.token = token;
        this.expression = expression;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return expression.toString();
    }
}
