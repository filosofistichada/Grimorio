# Grimorio

Intérprete del lenguaje **Grimorio**, un lenguaje de hechicería escrito en Java para el curso de Compiladores (Universidad EIA). Tomamos como referencia la rama `evaluator` del repositorio del curso (escrito en Go) y lo reconstruimos en Java con lexer, parser de Pratt, AST y evaluador de árbol.

**Integrantes:** Sofía Soto · David Giraldo

```text
Código fuente  →  Lexer  →  Parser  →  Evaluador  →  Resultado
 (.grim)        leer runas  descifrar   lanzar el
                (tokens)    (AST)       hechizo
```

## Requisitos

- **JDK 11 o superior** (`java -version`). No hace falta instalar Maven: el proyecto trae Maven Wrapper (`mvnw`), que descarga Maven la primera vez.
- Conexión a internet la primera vez (para descargar Maven y JUnit).

## Compilar, ejecutar y probar

Desde la carpeta `grimorio/`:

| Acción | Windows (PowerShell / CMD) | Linux / macOS / Git Bash |
| --- | --- | --- |
| Compilar | `.\mvnw.cmd package -DskipTests` | `./mvnw package -DskipTests` |
| Correr **todas** las pruebas | `.\mvnw.cmd test` | `./mvnw test` |
| Abrir el REPL | `java -jar target\grimorio.jar` | `java -jar target/grimorio.jar` |
| Ejecutar un archivo | `java -jar target\grimorio.jar examples\05_hechizos_recursion.grim` | `java -jar target/grimorio.jar examples/05_hechizos_recursion.grim` |
| Ver los tokens de un archivo | `java -jar target\grimorio.jar --tokens archivo.grim` | `java -jar target/grimorio.jar --tokens archivo.grim` |
| Ver el AST de un archivo | `java -jar target\grimorio.jar --ast archivo.grim` | `java -jar target/grimorio.jar --ast archivo.grim` |

> **Tildes en Windows:** si los mensajes muestran caracteres raros (`Ã³`), ejecuta `chcp 65001` en la consola antes de correr el intérprete.

El programa termina con código `0` si todo salió bien, `1` si hubo un error léxico, sintáctico o de ejecución, y `2` si no pudo leer el archivo.

### Modalidad de entrada

El intérprete admite **las dos modalidades**:

- **Archivo:** `java -jar target/grimorio.jar programa.grim` ejecuta el programa completo. Solo se ve lo que imprime `revelar`.
- **REPL:** sin argumentos abre una consola interactiva. Las variables se recuerdan entre líneas, y si una línea deja `(`, `{` o `[` sin cerrar, el REPL sigue leyendo líneas hasta que se cierren (una línea vacía fuerza el final). Muestra el valor de cada expresión.

| Comando del REPL | Acción |
| --- | --- |
| `:lexer` | Muestra los tokens (tipo, línea:columna y texto) de cada entrada |
| `:parser` | Muestra el AST como árbol y el código con paréntesis explícitos |
| `:evaluar` | Ejecuta el código (modo inicial) |
| `:cargar ruta.grim` | Ejecuta un archivo dentro de la sesión actual |
| `:ayuda` / `:salir` | Ayuda / terminar (también Ctrl+D, o Ctrl+Z y Enter en Windows) |

```text
grimorio>> pocion doble = hechizo(x) { conjura x * 2; };
grimorio>> doble(21)
42
grimorio>> :parser
parser>> pocion x = 1 + 2 * 3;
Programa:
pocion x = (1 + (2 * 3));
AST:
Program
└── LetStatement x
    └── valor: InfixExpression +
        ├── izquierda: IntegerLiteral 1
        └── derecha: InfixExpression *
            ├── izquierda: IntegerLiteral 2
            └── derecha: IntegerLiteral 3
```

## El lenguaje en una página

```text
// Comentario hasta el final de la línea
pocion nombre = "Merlin";          // declarar variable (let)
nombre = "Morgana";                // reasignar
pocion fib = hechizo(n) {          // definir función
    siProfecia (n <= 1) { conjura n; }       // if + return
    conjura fib(n - 1) + fib(n - 2);
};
siProfecia (x > 0) { ... } otraProfecia (x == 0) { ... } maldicion { ... }   // if / elif / else
amarre (i < 10) { i = i + 1; }                           // while
circulo (pocion i = 0; i < 10; i = i + 1) { ... }        // for
pocion lista = [1, 2, 3];          lista[0] = 9;
pocion libro = {"titulo": "Grimorio", "paginas": 300};   libro["autor"] = "Anónimo";
```

| Concepto | Grimorio | Concepto | Grimorio |
| --- | --- | --- | --- |
| let | `pocion` | while | `amarre` |
| function | `hechizo` | for | `circulo` |
| return | `conjura` | true / false | `luz` / `sombra` |
| if / elif / else | `siProfecia` / `otraProfecia` / `maldicion` | null | `vacio` |

**Tipos:** enteros de 64 bits (`42`), decimales (`3.14`), textos (`"hola\n"`, con escapes `\n \t \" \\`), booleanos, `vacio`, listas, diccionarios (claves enteras, decimales, de texto o booleanas) y hechizos.

**Operadores**, de menor a mayor precedencia:

| Precedencia | Operadores | Asociatividad |
| --- | --- | --- |
| 1 | `=` | derecha |
| 2 | `\|\|` | izquierda (cortocircuito) |
| 3 | `&&` | izquierda (cortocircuito) |
| 4 | `==` `!=` | izquierda |
| 5 | `<` `>` `<=` `>=` | izquierda |
| 6 | `+` `-` | izquierda |
| 7 | `*` `/` `%` | izquierda |
| 8 | `-x` `!x` (prefijos) | — |
| 9 | `**` | derecha |
| 10 | llamado `f(x)`, índice `l[i]` | izquierda |

Entero con entero da entero (`7 / 2` es `3`); si hay un decimal el resultado es decimal (`7 / 2.0` es `3.5`). `+` también une textos o listas. Solo `sombra` y `vacio` son falsos; todo lo demás, incluido `0`, es verdadero.

**Hechizos integrados:**

| Hechizo | Qué hace |
| --- | --- |
| `revelar(a, b, ...)` | Imprime los valores separados por espacio |
| `medir(x)` | Longitud de un texto, lista o diccionario |
| `transmutar(x)` | Convierte a texto |
| `cristalizar(x)` | Convierte a entero (trunca decimales; acepta textos y booleanos) |
| `destilar(x)` | Convierte a decimal |
| `esencia(x)` | Nombre del tipo: `"entero"`, `"texto"`, `"lista"`, ... |
| `alCaldero(lista, x)` | Agrega `x` al final de la lista y la devuelve |
| `claves(diccionario)` | Lista con las claves |
| `encantar(lista, h)` | Nueva lista con `h(x)` para cada elemento (map) |
| `tamizar(lista, h)` | Nueva lista con los elementos donde `h(x)` es verdadero (filter) |

**Extensiones implementadas:** closures, evaluación en cortocircuito, hechizos de orden superior (`encantar`, `tamizar` y hechizos que reciben o devuelven hechizos), comentarios y línea/columna en todos los errores.

### Errores

Cada error dice en qué etapa se detectó, qué pasó y dónde:

| Etapa | Ejemplo |
| --- | --- |
| Léxica | `Runa ilegible: carácter '@' no reconocido (línea 1, columna 14)` |
| Sintáctica | `Conjuro mal pronunciado, vaya a estudiar latín: se esperaba ')' pero se encontró '{' (línea 2, columna 19)` |
| Ejecución | `El hechizo salió mal, acabas de abrir un portal interdimensional: división entre cero (línea 2, columna 15)` |

Si hay errores léxicos o sintácticos, el programa no se ejecuta. El parser se recupera tras cada error y reporta todos los errores sintácticos sin errores falsos en cascada.

## Estructura del proyecto

| Ruta | Componente |
| --- | --- |
| `src/main/java/grimorio/token/` | `TokenType` (tipos y palabras reservadas) y `Token` (con línea y columna) |
| `src/main/java/grimorio/lexer/` | `Lexer`: texto → tokens, comentarios, errores léxicos |
| `src/main/java/grimorio/ast/` | Nodos del AST (`Program`, `LetStatement`, `IfExpression`, ...) y `AstPrinter` |
| `src/main/java/grimorio/parser/` | `Parser` (descenso recursivo + Pratt) y `Precedence` |
| `src/main/java/grimorio/object/` | Valores en ejecución (`IntegerObj`, `ArrayObj`, `FunctionObj`, ...) y `Environment` (ámbitos) |
| `src/main/java/grimorio/evaluator/` | `Evaluator` (recorre el AST), `Operators` (aritmética y comparación), `Builtins` (hechizos integrados) |
| `src/main/java/grimorio/repl/` | `Repl`: consola interactiva |
| `src/main/java/grimorio/Interpreter.java` | Une las etapas y decide qué errores reportar |
| `src/main/java/grimorio/Main.java` | Punto de entrada: REPL, archivo, `--tokens`, `--ast` |
| `examples/` | Un programa por característica (variables, expresiones, condicionales, ciclos, hechizos, ámbitos, estructuras, integrados, extensiones) y un programa completo |
| `tests/validos/`, `tests/errores/` | Programas de prueba con su salida esperada (`.esperado`) |
| `src/test/java/` | Pruebas JUnit del lexer, el parser, el evaluador y de todos los programas de `tests/` |
| `docs/DISENO.md` | Documento de diseño: gramática EBNF, arquitectura, decisiones y limitaciones |

## Pruebas

`./mvnw test` (o `.\mvnw.cmd test`) corre todo con un solo comando:

- **`LexerTest`, `ParserTest`, `EvaluatorTest`:** pruebas unitarias de cada etapa (tokens, posiciones, precedencia y asociatividad, ámbitos, recursión, closures, errores).
- **`ProgramFilesTest`:** ejecuta cada `tests/**/*.grim` y compara su salida con el `.esperado` del mismo nombre. Los de `tests/errores/` deben terminar con error y los de `tests/validos/` sin error. También comprueba que los programas de `examples/` corran sin errores.

Para agregar un caso de prueba basta con crear `tests/validos/mi_caso.grim` (o en `errores/`) y su `mi_caso.esperado`.
