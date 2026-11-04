package grimorio.ast;

import grimorio.token.Token;

/** {@code amarre (condicion) { ... }}: repite el bloque mientras la condición sea verdadera. */
public final class WhileExpression implements Expression {
    public final Token token;
    public final Expression condition;
    public final BlockStatement body;

    public WhileExpression(Token token, Expression condition, BlockStatement body) {
        this.token = token;
        this.condition = condition;
        this.body = body;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "amarre " + condition + " " + body;
    }
}
