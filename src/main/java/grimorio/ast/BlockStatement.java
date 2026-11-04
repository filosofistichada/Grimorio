package grimorio.ast;

import grimorio.token.Token;

import java.util.List;
import java.util.stream.Collectors;

/** Bloque entre llaves. Cada bloque abre un ámbito nuevo para sus variables. */
public final class BlockStatement implements Statement {
    public final Token token;
    public final List<Statement> statements;

    public BlockStatement(Token token, List<Statement> statements) {
        this.token = token;
        this.statements = statements;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        return "{ " + statements.stream().map(Object::toString).collect(Collectors.joining(" ")) + " }";
    }
}
