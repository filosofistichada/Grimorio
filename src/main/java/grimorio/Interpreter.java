package grimorio;

import grimorio.ast.Program;
import grimorio.evaluator.Evaluator;
import grimorio.lexer.Lexer;
import grimorio.object.Environment;
import grimorio.object.ErrorObj;
import grimorio.object.Obj;
import grimorio.parser.Parser;

import java.io.PrintStream;
import java.util.List;

/**
 * Une las etapas: código fuente → Lexer → Parser → Evaluador → resultado.
 *
 * Los errores se detectan en la etapa que corresponde y se detienen ahí: si
 * hay errores léxicos no se reportan los sintácticos que causan, y si hay
 * errores léxicos o sintácticos el programa no se ejecuta.
 *
 * El entorno global se conserva entre llamados a {@link #run}, así el REPL
 * recuerda las pociones de líneas anteriores.
 */
public final class Interpreter {
    private final PrintStream out;
    private final Evaluator evaluator;
    private final Environment globals = new Environment();

    public Interpreter(PrintStream out) {
        this.out = out;
        this.evaluator = new Evaluator(out);
    }

    /** Ejecuta el código. Devuelve el valor final, o un ErrorObj con el/los mensajes de error. */
    public Obj run(String source) {
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();

        if (!lexer.errors().isEmpty()) return new ErrorObj(String.join("\n", lexer.errors()));
        if (!parser.errors().isEmpty()) return new ErrorObj(String.join("\n", parser.errors()));

        try {
            return evaluator.eval(program, globals);
        } catch (StackOverflowError e) {
            return new ErrorObj(Messages.RUNTIME + ": recursión demasiado profunda (¿falta un caso base?)");
        }
    }

    /** Ejecuta un programa completo e imprime el error si lo hay. Devuelve true si terminó sin errores. */
    public boolean runProgram(String source) {
        Obj result = run(source);
        if (result instanceof ErrorObj) {
            out.println(result.inspect());
            return false;
        }
        return true;
    }

    /** Para el modo parser: el AST como árbol, o los errores. */
    public static String describeAst(String source) {
        Lexer lexer = new Lexer(source);
        Parser parser = new Parser(lexer);
        Program program = parser.parseProgram();
        List<String> errors = !lexer.errors().isEmpty() ? lexer.errors() : parser.errors();
        if (!errors.isEmpty()) return String.join("\n", errors);
        return "Programa:\n" + program + "\nAST:\n" + grimorio.ast.AstPrinter.print(program);
    }
}
