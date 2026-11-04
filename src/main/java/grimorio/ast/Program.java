package grimorio.ast;

import grimorio.token.Token;

import java.util.List;
import java.util.stream.Collectors;

/** Raíz del AST: la lista de instrucciones del programa, en orden. */
public final class Program implements Node {
    public final List<Statement> statements;

    public Program(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public Token token() {
        return statements.isEmpty() ? null : statements.get(0).token();
    }

    @Override
    public String toString() {
        return statements.stream().map(Object::toString).collect(Collectors.joining("\n"));
    }
}
