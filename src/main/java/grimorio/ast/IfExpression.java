package grimorio.ast;

import grimorio.token.Token;

import java.util.List;

/**
 * {@code siProfecia (c1) { ... } otraProfecia (c2) { ... } maldicion { ... }}.
 * Las ramas se prueban en orden; {@code alternative} (maldicion) es opcional.
 */
public final class IfExpression implements Expression {
    /** Una rama condición → bloque. */
    public static final class Branch {
        public final Expression condition;
        public final BlockStatement consequence;

        public Branch(Expression condition, BlockStatement consequence) {
            this.condition = condition;
            this.consequence = consequence;
        }
    }

    public final Token token;
    public final List<Branch> branches;
    public final BlockStatement alternative; // null si no hay maldicion

    public IfExpression(Token token, List<Branch> branches, BlockStatement alternative) {
        this.token = token;
        this.branches = branches;
        this.alternative = alternative;
    }

    @Override public Token token() { return token; }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            out.append(i == 0 ? "siProfecia " : " otraProfecia ")
               .append(branch.condition).append(" ").append(branch.consequence);
        }
        if (alternative != null) out.append(" maldicion ").append(alternative);
        return out.toString();
    }
}
