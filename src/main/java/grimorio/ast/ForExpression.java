package grimorio.ast;

import grimorio.token.Token;

/**
 * {@code circulo (inicio; condicion; paso) { ... }}. Las tres partes son
 * opcionales; sin condición el ciclo se repite hasta un {@code conjura}.
 */
public final class ForExpression implements Expression {
    public final Token token;
    public final Statement init;        // puede ser null
    public final Expression condition;  // puede ser null
    public final Expression update;     // puede ser null
    public final BlockStatement body;

    public ForExpression(Token token, Statement init, Expression condition, Expression update, BlockStatement body) {
        this.token = token;
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        String initText = init == null ? ";" : (init instanceof LetStatement ? init.toString() : init + ";");
        return "circulo (" + initText + " " + (condition == null ? "" : condition) + "; "
                + (update == null ? "" : update) + ") " + body;
    }
}
