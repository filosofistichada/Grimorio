package grimorio.ast;

import grimorio.token.Token;

/** {@code luz} (verdadero) o {@code sombra} (falso). */
public final class BooleanLiteral implements Expression {
    public final Token token;
    public final boolean value;

    public BooleanLiteral(Token token, boolean value) {
        this.token = token;
        this.value = value;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return token.literal();
    }
}
