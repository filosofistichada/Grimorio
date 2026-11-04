package grimorio.evaluator;

import grimorio.object.*;
import grimorio.token.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Operadores binarios (+ - * / % ** == != < > <= >=) según el tipo de los operandos:
 * <ul>
 *   <li>entero con entero da entero (7 / 2 = 3, división entera);</li>
 *   <li>si alguno es decimal, ambos se tratan como decimal (7 / 2.0 = 3.5);</li>
 *   <li>texto + texto concatena; lista + lista une las listas;</li>
 *   <li>== y != funcionan con cualquier par de valores.</li>
 * </ul>
 * Los enteros son de 64 bits y el desbordamiento es un error, no un resultado silencioso.
 */
final class Operators {
    private Operators() {}

    static Obj infix(String op, Obj left, Obj right, Token at) {
        if (left instanceof IntegerObj && right instanceof IntegerObj) {
            return integers(op, ((IntegerObj) left).value, ((IntegerObj) right).value, at);
        }
        if (isNumber(left) && isNumber(right)) {
            return floats(op, toDouble(left), toDouble(right), at);
        }
        if (left instanceof StringObj && right instanceof StringObj) {
            return strings(op, ((StringObj) left).value, ((StringObj) right).value, at);
        }
        if (left instanceof ArrayObj && right instanceof ArrayObj && op.equals("+")) {
            List<Obj> joined = new ArrayList<>(((ArrayObj) left).elements);
            joined.addAll(((ArrayObj) right).elements);
            return new ArrayObj(joined);
        }
        if (op.equals("==")) return BooleanObj.of(areEqual(left, right));
        if (op.equals("!=")) return BooleanObj.of(!areEqual(left, right));

        String hint = op.equals("+") && (left instanceof StringObj || right instanceof StringObj)
                ? "; usa transmutar(x) para convertir a texto" : "";
        return Evaluator.error(at, "no se puede aplicar '" + op + "' a un " + left.type().displayName()
                + " y un " + right.type().displayName() + hint);
    }

    private static Obj integers(String op, long a, long b, Token at) {
        try {
            switch (op) {
                case "+": return new IntegerObj(Math.addExact(a, b));
                case "-": return new IntegerObj(Math.subtractExact(a, b));
                case "*": return new IntegerObj(Math.multiplyExact(a, b));
                case "/":
                    if (b == 0) return Evaluator.error(at, "división entre cero");
                    if (a == Long.MIN_VALUE && b == -1) return Evaluator.error(at, "desbordamiento de entero");
                    return new IntegerObj(a / b);
                case "%":
                    if (b == 0) return Evaluator.error(at, "módulo entre cero");
                    return new IntegerObj(a % b);
                case "**":
                    if (b < 0) return new FloatObj(Math.pow(a, b));
                    return new IntegerObj(power(a, b));
                case "<": return BooleanObj.of(a < b);
                case ">": return BooleanObj.of(a > b);
                case "<=": return BooleanObj.of(a <= b);
                case ">=": return BooleanObj.of(a >= b);
                case "==": return BooleanObj.of(a == b);
                case "!=": return BooleanObj.of(a != b);
                default: return unknown(op, at);
            }
        } catch (ArithmeticException e) {
            return Evaluator.error(at, "desbordamiento de entero: el resultado no cabe en 64 bits");
        }
    }

    /**
     * Potencia por cuadrados sucesivos (O(log n) multiplicaciones), con
     * detección de desbordamiento: 3 ** 5 = 3 * 3^4, y 3^4 = (3^2)^2.
     */
    private static long power(long base, long exponent) {
        long result = 1;
        while (exponent > 0) {
            if ((exponent & 1) == 1) result = Math.multiplyExact(result, base);
            exponent >>= 1;
            if (exponent > 0) base = Math.multiplyExact(base, base);
        }
        return result;
    }

    private static Obj floats(String op, double a, double b, Token at) {
        switch (op) {
            case "+": return new FloatObj(a + b);
            case "-": return new FloatObj(a - b);
            case "*": return new FloatObj(a * b);
            case "/":
                if (b == 0) return Evaluator.error(at, "división entre cero");
                return new FloatObj(a / b);
            case "%":
                if (b == 0) return Evaluator.error(at, "módulo entre cero");
                return new FloatObj(a % b);
            case "**": return new FloatObj(Math.pow(a, b));
            case "<": return BooleanObj.of(a < b);
            case ">": return BooleanObj.of(a > b);
            case "<=": return BooleanObj.of(a <= b);
            case ">=": return BooleanObj.of(a >= b);
            case "==": return BooleanObj.of(a == b);
            case "!=": return BooleanObj.of(a != b);
            default: return unknown(op, at);
        }
    }

    private static Obj strings(String op, String a, String b, Token at) {
        switch (op) {
            case "+": return new StringObj(a + b);
            case "==": return BooleanObj.of(a.equals(b));
            case "!=": return BooleanObj.of(!a.equals(b));
            case "<": return BooleanObj.of(a.compareTo(b) < 0);
            case ">": return BooleanObj.of(a.compareTo(b) > 0);
            case "<=": return BooleanObj.of(a.compareTo(b) <= 0);
            case ">=": return BooleanObj.of(a.compareTo(b) >= 0);
            default:
                return Evaluator.error(at, "no se puede aplicar '" + op + "' a dos textos");
        }
    }

    /** Igualdad por valor para números, textos, booleanos, vacio y listas; por identidad para lo demás. */
    static boolean areEqual(Obj a, Obj b) {
        if (isNumber(a) && isNumber(b)) {
            if (a instanceof IntegerObj && b instanceof IntegerObj) return ((IntegerObj) a).value == ((IntegerObj) b).value;
            return toDouble(a) == toDouble(b);
        }
        if (a instanceof StringObj && b instanceof StringObj) return ((StringObj) a).value.equals(((StringObj) b).value);
        if (a instanceof ArrayObj && b instanceof ArrayObj) {
            List<Obj> x = ((ArrayObj) a).elements, y = ((ArrayObj) b).elements;
            if (x.size() != y.size()) return false;
            for (int i = 0; i < x.size(); i++) if (!areEqual(x.get(i), y.get(i))) return false;
            return true;
        }
        return a == b; // booleanos y vacio son únicos, así que la identidad basta
    }

    private static boolean isNumber(Obj value) {
        return value instanceof IntegerObj || value instanceof FloatObj;
    }

    private static double toDouble(Obj value) {
        return value instanceof IntegerObj ? ((IntegerObj) value).value : ((FloatObj) value).value;
    }

    private static Obj unknown(String op, Token at) {
        return Evaluator.error(at, "operador desconocido '" + op + "'");
    }
}
