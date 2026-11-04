package grimorio.evaluator;

import grimorio.Messages;
import grimorio.ast.*;
import grimorio.object.*;
import grimorio.token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Evaluador de árbol: recorre el AST y calcula el valor de cada nodo.
 *
 * Dos valores especiales controlan el flujo:
 * <ul>
 *   <li>{@link ReturnValue}: lo produce {@code conjura}; sube por bloques y
 *       ciclos sin ejecutar nada más hasta llegar al llamado del hechizo.</li>
 *   <li>{@link ErrorObj}: un error de ejecución; sube hasta el programa y
 *       detiene todo.</li>
 * </ul>
 * Por eso, después de evaluar un hijo, casi todos los métodos revisan
 * {@link #isError} y, si hay error, lo devuelven de inmediato.
 */
public class Evaluator {
    private final Map<String, BuiltinObj> builtins;

    public Evaluator(PrintStream out) {
        this.builtins = new Builtins(this, out).all();
    }

    public Obj eval(Node node, Environment env) {
        // Instrucciones
        if (node instanceof Program) return evalProgram(((Program) node).statements, env);
        if (node instanceof BlockStatement) return evalBlock((BlockStatement) node, env);
        if (node instanceof ExpressionStatement) return eval(((ExpressionStatement) node).expression, env);
        if (node instanceof LetStatement) return evalLet((LetStatement) node, env);
        if (node instanceof ReturnStatement) return evalReturn((ReturnStatement) node, env);

        // Literales
        if (node instanceof IntegerLiteral) return new IntegerObj(((IntegerLiteral) node).value);
        if (node instanceof FloatLiteral) return new FloatObj(((FloatLiteral) node).value);
        if (node instanceof StringLiteral) return new StringObj(((StringLiteral) node).value);
        if (node instanceof BooleanLiteral) return BooleanObj.of(((BooleanLiteral) node).value);
        if (node instanceof NullLiteral) return NullObj.INSTANCE;
        if (node instanceof ArrayLiteral) return evalArray((ArrayLiteral) node, env);
        if (node instanceof HashLiteral) return evalHash((HashLiteral) node, env);
        if (node instanceof FunctionLiteral) {
            FunctionLiteral fn = (FunctionLiteral) node;
            return new FunctionObj(fn.parameters, fn.body, env); // guarda env: closure
        }

        // Expresiones
        if (node instanceof Identifier) return evalIdentifier((Identifier) node, env);
        if (node instanceof PrefixExpression) return evalPrefix((PrefixExpression) node, env);
        if (node instanceof InfixExpression) return evalInfix((InfixExpression) node, env);
        if (node instanceof LogicalExpression) return evalLogical((LogicalExpression) node, env);
        if (node instanceof AssignExpression) return evalAssign((AssignExpression) node, env);
        if (node instanceof IfExpression) return evalIf((IfExpression) node, env);
        if (node instanceof WhileExpression) return evalWhile((WhileExpression) node, env);
        if (node instanceof ForExpression) return evalFor((ForExpression) node, env);
        if (node instanceof CallExpression) return evalCall((CallExpression) node, env);
        if (node instanceof IndexExpression) return evalIndex((IndexExpression) node, env);

        return error(node.token(), "no sé evaluar " + node.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------
    // Instrucciones y ámbitos
    // ------------------------------------------------------------------

    /** Ejecuta el programa; un conjura en el nivel superior termina el programa. */
    private Obj evalProgram(List<Statement> statements, Environment env) {
        Obj result = NullObj.INSTANCE;
        for (Statement statement : statements) {
            result = eval(statement, env);
            if (result instanceof ReturnValue) return ((ReturnValue) result).value;
            if (isError(result)) return result;
        }
        return result;
    }

    /** Un bloque abre un ámbito nuevo: las pociones declaradas adentro no existen afuera. */
    private Obj evalBlock(BlockStatement block, Environment env) {
        return evalStatements(block.statements, new Environment(env));
    }

    /** Ejecuta instrucciones en {@code env}. Si una produce ReturnValue o error, se detiene y lo devuelve sin desenvolver. */
    private Obj evalStatements(List<Statement> statements, Environment env) {
        Obj result = NullObj.INSTANCE;
        for (Statement statement : statements) {
            result = eval(statement, env);
            if (result instanceof ReturnValue || isError(result)) return result;
        }
        return result;
    }

    private Obj evalLet(LetStatement let, Environment env) {
        Obj value = eval(let.value, env);
        if (isError(value)) return value;
        env.define(let.name.name, value);
        return NullObj.INSTANCE;
    }

    private Obj evalReturn(ReturnStatement ret, Environment env) {
        if (ret.value == null) return new ReturnValue(NullObj.INSTANCE);
        Obj value = eval(ret.value, env);
        if (isError(value)) return value;
        return new ReturnValue(value);
    }

    private Obj evalIdentifier(Identifier identifier, Environment env) {
        Obj value = env.get(identifier.name);
        if (value != null) return value;
        BuiltinObj builtin = builtins.get(identifier.name);
        if (builtin != null) return builtin;
        return error(identifier.token, "la pocion '" + identifier.name + "' no existe");
    }

    // ------------------------------------------------------------------
    // Operadores
    // ------------------------------------------------------------------

    private Obj evalPrefix(PrefixExpression prefix, Environment env) {
        Obj right = eval(prefix.right, env);
        if (isError(right)) return right;
        switch (prefix.operator) {
            case "!":
                return BooleanObj.of(!isTruthy(right));
            case "-":
                if (right instanceof IntegerObj) {
                    long value = ((IntegerObj) right).value;
                    if (value == Long.MIN_VALUE) return error(prefix.token, "desbordamiento de entero");
                    return new IntegerObj(-value);
                }
                if (right instanceof FloatObj) return new FloatObj(-((FloatObj) right).value);
                return error(prefix.token, "no se puede aplicar '-' a un " + right.type().displayName());
            default:
                return error(prefix.token, "operador desconocido '" + prefix.operator + "'");
        }
    }

    private Obj evalInfix(InfixExpression infix, Environment env) {
        Obj left = eval(infix.left, env);
        if (isError(left)) return left;
        Obj right = eval(infix.right, env);
        if (isError(right)) return right;
        return Operators.infix(infix.operator, left, right, infix.token);
    }

    /**
     * {@code &&} y {@code ||} en cortocircuito: si el lado izquierdo ya decide
     * el resultado, el derecho NO se evalúa. Por eso {@code sombra && x()} no
     * llama a x.
     */
    private Obj evalLogical(LogicalExpression logical, Environment env) {
        Obj left = eval(logical.left, env);
        if (isError(left)) return left;
        boolean isAnd = logical.operator.equals("&&");
        if (isAnd && !isTruthy(left)) return BooleanObj.FALSE;
        if (!isAnd && isTruthy(left)) return BooleanObj.TRUE;
        Obj right = eval(logical.right, env);
        if (isError(right)) return right;
        return BooleanObj.of(isTruthy(right));
    }

    private Obj evalAssign(AssignExpression assign, Environment env) {
        if (assign.target instanceof Identifier) {
            String name = ((Identifier) assign.target).name;
            Obj value = eval(assign.value, env);
            if (isError(value)) return value;
            if (!env.assign(name, value)) {
                return error(assign.target.token(), "la pocion '" + name + "' no existe; declárala primero con 'pocion " + name + " = ...'");
            }
            return value;
        }

        IndexExpression target = (IndexExpression) assign.target;
        Obj container = eval(target.left, env);
        if (isError(container)) return container;
        Obj index = eval(target.index, env);
        if (isError(index)) return index;
        Obj value = eval(assign.value, env);
        if (isError(value)) return value;

        if (container instanceof ArrayObj) {
            ArrayObj array = (ArrayObj) container;
            Obj checked = checkArrayIndex(array, index, target.token);
            if (isError(checked)) return checked;
            array.elements.set((int) ((IntegerObj) index).value, value);
            return value;
        }
        if (container instanceof HashObj) {
            if (!(index instanceof Hashable)) {
                return error(target.token, "un " + index.type().displayName() + " no puede ser clave de un diccionario");
            }
            ((HashObj) container).put((Hashable) index, value);
            return value;
        }
        return error(target.token, "no se puede asignar por índice en un " + container.type().displayName());
    }

    // ------------------------------------------------------------------
    // Control de flujo
    // ------------------------------------------------------------------

    private Obj evalIf(IfExpression ifExpression, Environment env) {
        for (IfExpression.Branch branch : ifExpression.branches) {
            Obj condition = eval(branch.condition, env);
            if (isError(condition)) return condition;
            if (isTruthy(condition)) return evalBlock(branch.consequence, env);
        }
        if (ifExpression.alternative != null) return evalBlock(ifExpression.alternative, env);
        return NullObj.INSTANCE;
    }

    private Obj evalWhile(WhileExpression loop, Environment env) {
        while (true) {
            Obj condition = eval(loop.condition, env);
            if (isError(condition)) return condition;
            if (!isTruthy(condition)) return NullObj.INSTANCE;
            Obj result = evalBlock(loop.body, env);
            if (result instanceof ReturnValue || isError(result)) return result;
        }
    }

    /** El inicio vive en un ámbito propio del ciclo: la variable i no existe después del circulo. */
    private Obj evalFor(ForExpression loop, Environment env) {
        Environment loopEnv = new Environment(env);
        if (loop.init != null) {
            Obj init = eval(loop.init, loopEnv);
            if (isError(init)) return init;
        }
        while (true) {
            if (loop.condition != null) {
                Obj condition = eval(loop.condition, loopEnv);
                if (isError(condition)) return condition;
                if (!isTruthy(condition)) return NullObj.INSTANCE;
            }
            Obj result = evalBlock(loop.body, loopEnv);
            if (result instanceof ReturnValue || isError(result)) return result;
            if (loop.update != null) {
                Obj update = eval(loop.update, loopEnv);
                if (isError(update)) return update;
            }
        }
    }

    // ------------------------------------------------------------------
    // Hechizos
    // ------------------------------------------------------------------

    private Obj evalCall(CallExpression call, Environment env) {
        Obj function = eval(call.function, env);
        if (isError(function)) return function;
        List<Obj> args = new ArrayList<>();
        for (Expression argument : call.arguments) {
            Obj value = eval(argument, env);
            if (isError(value)) return value;
            args.add(value);
        }
        return applyFunction(function, args, call.function.token()); // el error apunta al nombre del hechizo
    }

    /**
     * Llama a un hechizo. Para uno del usuario crea un ámbito nuevo cuyo
     * exterior es el entorno donde el hechizo se DEFINIÓ (no donde se llama):
     * eso es el alcance léxico y es lo que hace funcionar los closures.
     */
    public Obj applyFunction(Obj function, List<Obj> args, Token at) {
        if (function instanceof FunctionObj) {
            FunctionObj fn = (FunctionObj) function;
            if (args.size() != fn.parameters.size()) {
                return error(at, "el hechizo espera " + fn.parameters.size() + " argumento(s) y recibió " + args.size());
            }
            Environment callEnv = new Environment(fn.env);
            for (int i = 0; i < args.size(); i++) callEnv.define(fn.parameters.get(i).name, args.get(i));
            Obj result = evalStatements(fn.body.statements, callEnv);
            if (result instanceof ReturnValue) return ((ReturnValue) result).value;
            return result; // sin conjura: el valor de la última instrucción
        }
        if (function instanceof BuiltinObj) {
            return ((BuiltinObj) function).implementation.apply(args, at);
        }
        return error(at, "un " + function.type().displayName() + " no es un hechizo, no se puede llamar");
    }

    // ------------------------------------------------------------------
    // Estructuras de datos
    // ------------------------------------------------------------------

    private Obj evalArray(ArrayLiteral literal, Environment env) {
        List<Obj> elements = new ArrayList<>();
        for (Expression element : literal.elements) {
            Obj value = eval(element, env);
            if (isError(value)) return value;
            elements.add(value);
        }
        return new ArrayObj(elements);
    }

    private Obj evalHash(HashLiteral literal, Environment env) {
        HashObj hash = new HashObj();
        for (int i = 0; i < literal.keys.size(); i++) {
            Obj key = eval(literal.keys.get(i), env);
            if (isError(key)) return key;
            if (!(key instanceof Hashable)) {
                return error(literal.keys.get(i).token(), "un " + key.type().displayName() + " no puede ser clave de un diccionario");
            }
            Obj value = eval(literal.values.get(i), env);
            if (isError(value)) return value;
            hash.put((Hashable) key, value);
        }
        return hash;
    }

    private Obj evalIndex(IndexExpression indexExpression, Environment env) {
        Obj container = eval(indexExpression.left, env);
        if (isError(container)) return container;
        Obj index = eval(indexExpression.index, env);
        if (isError(index)) return index;
        Token at = indexExpression.token;

        if (container instanceof ArrayObj) {
            ArrayObj array = (ArrayObj) container;
            Obj checked = checkArrayIndex(array, index, at);
            if (isError(checked)) return checked;
            return array.elements.get((int) ((IntegerObj) index).value);
        }
        if (container instanceof StringObj) {
            String text = ((StringObj) container).value;
            if (!(index instanceof IntegerObj)) return error(at, "el índice de un texto debe ser un entero");
            long i = ((IntegerObj) index).value;
            if (i < 0 || i >= text.length()) {
                return error(at, "índice " + i + " fuera de rango (el texto tiene " + text.length() + " caracteres)");
            }
            return new StringObj(String.valueOf(text.charAt((int) i)));
        }
        if (container instanceof HashObj) {
            if (!(index instanceof Hashable)) {
                return error(at, "un " + index.type().displayName() + " no puede ser clave de un diccionario");
            }
            HashObj.Pair pair = ((HashObj) container).pairs.get(((Hashable) index).hashKey());
            return pair == null ? NullObj.INSTANCE : pair.value;
        }
        return error(at, "no se puede indexar un " + container.type().displayName());
    }

    private Obj checkArrayIndex(ArrayObj array, Obj index, Token at) {
        if (!(index instanceof IntegerObj)) {
            return error(at, "el índice de una lista debe ser un entero, no un " + index.type().displayName());
        }
        long i = ((IntegerObj) index).value;
        if (i < 0 || i >= array.elements.size()) {
            return error(at, "índice " + i + " fuera de rango (la lista tiene " + array.elements.size() + " elementos)");
        }
        return index;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Convención del lenguaje: solo {@code sombra} y {@code vacio} son falsos; todo lo demás (incluido 0) es verdadero. */
    public static boolean isTruthy(Obj value) {
        if (value == NullObj.INSTANCE) return false;
        if (value instanceof BooleanObj) return ((BooleanObj) value).value;
        return true;
    }

    public static boolean isError(Obj value) {
        return value instanceof ErrorObj;
    }

    public static ErrorObj error(Token at, String detail) {
        return new ErrorObj(Messages.format(Messages.RUNTIME, detail, at));
    }
}
