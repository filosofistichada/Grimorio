package grimorio.ast;

import grimorio.token.Token;

/** Nombre de una variable o hechizo. */
public final class Identifier implements Expression {
    public final Token token;
    public final String name;

    public Identifier(Token token, String name) {
        this.token = token;
        this.name = name;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return name;
    }
}
