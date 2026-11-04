package grimorio.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja el AST como un árbol de texto, para el modo parser del REPL y la
 * opción {@code --ast}. Por ejemplo, {@code pocion x = 1 + 2 * 3;} se ve así:
 *
 * <pre>
 * Program
 * └── LetStatement x
 *     └── valor: InfixExpression +
 *         ├── izquierda: IntegerLiteral 1
 *         └── derecha: InfixExpression *
 *             ├── izquierda: IntegerLiteral 2
 *             └── derecha: IntegerLiteral 3
 * </pre>
 */
public final class AstPrinter {
    private final StringBuilder out = new StringBuilder();

    private AstPrinter() {}

    public static String print(Node node) {
        AstPrinter printer = new AstPrinter();
        printer.out.append(label(node)).append('\n');
        printer.children(children(node), "");
        return printer.out.toString();
    }

    /** Un hijo con nombre: "condición", "cuerpo", etc. */
    private static final class Child {
        final String name;
        final Node node;

        Child(String name, Node node) {
            this.name = name;
            this.node = node;
        }
    }

    private void children(List<Child> kids, String indent) {
        for (int i = 0; i < kids.size(); i++) {
            boolean last = i == kids.size() - 1;
            Child kid = kids.get(i);
            out.append(indent).append(last ? "└── " : "├── ");
            if (kid.name != null) out.append(kid.name).append(": ");
            out.append(kid.node == null ? "(nada)" : label(kid.node)).append('\n');
            if (kid.node != null) children(children(kid.node), indent + (last ? "    " : "│   "));
        }
    }

    private static String label(Node node) {
        String name = node.getClass().getSimpleName();
        if (node instanceof LetStatement) return name + " " + ((LetStatement) node).name.name;
        if (node instanceof Identifier) return name + " " + ((Identifier) node).name;
        if (node instanceof IntegerLiteral || node instanceof FloatLiteral || node instanceof BooleanLiteral)
            return name + " " + node.token().literal();
        if (node instanceof StringLiteral) return name + " \"" + ((StringLiteral) node).value + "\"";
        if (node instanceof PrefixExpression) return name + " " + ((PrefixExpression) node).operator;
        if (node instanceof InfixExpression) return name + " " + ((InfixExpression) node).operator;
        if (node instanceof LogicalExpression) return name + " " + ((LogicalExpression) node).operator;
        if (node instanceof FunctionLiteral) {
            List<String> params = new ArrayList<>();
            for (Identifier p : ((FunctionLiteral) node).parameters) params.add(p.name);
            return name + " (" + String.join(", ", params) + ")";
        }
        return name;
    }

    private static List<Child> children(Node node) {
        List<Child> kids = new ArrayList<>();
        if (node instanceof Program) {
            for (Statement s : ((Program) node).statements) kids.add(new Child(null, s));
        } else if (node instanceof LetStatement) {
            kids.add(new Child("valor", ((LetStatement) node).value));
        } else if (node instanceof ReturnStatement) {
            ReturnStatement r = (ReturnStatement) node;
            if (r.value != null) kids.add(new Child("valor", r.value));
        } else if (node instanceof ExpressionStatement) {
            kids.add(new Child(null, ((ExpressionStatement) node).expression));
        } else if (node instanceof BlockStatement) {
            for (Statement s : ((BlockStatement) node).statements) kids.add(new Child(null, s));
        } else if (node instanceof PrefixExpression) {
            kids.add(new Child("operando", ((PrefixExpression) node).right));
        } else if (node instanceof InfixExpression) {
            InfixExpression n = (InfixExpression) node;
            kids.add(new Child("izquierda", n.left));
            kids.add(new Child("derecha", n.right));
        } else if (node instanceof LogicalExpression) {
            LogicalExpression n = (LogicalExpression) node;
            kids.add(new Child("izquierda", n.left));
            kids.add(new Child("derecha", n.right));
        } else if (node instanceof AssignExpression) {
            AssignExpression n = (AssignExpression) node;
            kids.add(new Child("destino", n.target));
            kids.add(new Child("valor", n.value));
        } else if (node instanceof IfExpression) {
            IfExpression n = (IfExpression) node;
            for (int i = 0; i < n.branches.size(); i++) {
                String prefix = i == 0 ? "siProfecia" : "otraProfecia";
                kids.add(new Child(prefix + " condición", n.branches.get(i).condition));
                kids.add(new Child(prefix + " bloque", n.branches.get(i).consequence));
            }
            if (n.alternative != null) kids.add(new Child("maldicion", n.alternative));
        } else if (node instanceof WhileExpression) {
            WhileExpression n = (WhileExpression) node;
            kids.add(new Child("condición", n.condition));
            kids.add(new Child("cuerpo", n.body));
        } else if (node instanceof ForExpression) {
            ForExpression n = (ForExpression) node;
            kids.add(new Child("inicio", n.init));
            kids.add(new Child("condición", n.condition));
            kids.add(new Child("paso", n.update));
            kids.add(new Child("cuerpo", n.body));
        } else if (node instanceof FunctionLiteral) {
            kids.add(new Child("cuerpo", ((FunctionLiteral) node).body));
        } else if (node instanceof CallExpression) {
            CallExpression n = (CallExpression) node;
            kids.add(new Child("hechizo", n.function));
            for (int i = 0; i < n.arguments.size(); i++) kids.add(new Child("argumento " + (i + 1), n.arguments.get(i)));
        } else if (node instanceof ArrayLiteral) {
            List<Expression> elements = ((ArrayLiteral) node).elements;
            for (int i = 0; i < elements.size(); i++) kids.add(new Child("[" + i + "]", elements.get(i)));
        } else if (node instanceof HashLiteral) {
            HashLiteral n = (HashLiteral) node;
            for (int i = 0; i < n.keys.size(); i++) {
                kids.add(new Child("clave", n.keys.get(i)));
                kids.add(new Child("valor", n.values.get(i)));
            }
        } else if (node instanceof IndexExpression) {
            IndexExpression n = (IndexExpression) node;
            kids.add(new Child("colección", n.left));
            kids.add(new Child("índice", n.index));
        }
        return kids;
    }
}
