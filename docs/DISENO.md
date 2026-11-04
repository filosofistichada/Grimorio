# Documento de diseño: intérprete Grimorio

**Integrantes:** Sofía Soto · David Giraldo — Compiladores, Universidad EIA, 2026-2

## 1. Gramática (EBNF)

```ebnf
programa      = { instruccion } EOF ;
instruccion   = ( declaracion | retorno | expresion ) [ ";" ] ;
declaracion   = "pocion" IDENT "=" expresion ;
retorno       = "conjura" [ expresion ] ;
bloque        = "{" { instruccion } "}" ;

expresion     = asignacion ;
asignacion    = ( IDENT | indexacion ) "=" asignacion | o_logico ;     (* derecha *)
o_logico      = y_logico { "||" y_logico } ;
y_logico      = igualdad { "&&" igualdad } ;
igualdad      = comparacion { ( "==" | "!=" ) comparacion } ;
comparacion   = suma { ( "<" | ">" | "<=" | ">=" ) suma } ;
suma          = producto { ( "+" | "-" ) producto } ;
producto      = prefijo { ( "*" | "/" | "%" ) prefijo } ;
prefijo       = ( "-" | "!" ) prefijo | potencia ;
potencia      = postfijo [ "**" prefijo ] ;                           (* derecha *)
postfijo      = primario { "(" [ argumentos ] ")" | "[" expresion "]" } ;
argumentos    = expresion { "," expresion } ;

primario      = IDENT | ENTERO | DECIMAL | TEXTO | "luz" | "sombra" | "vacio"
              | "(" expresion ")" | lista | diccionario
              | si | mientras | para | hechizo ;
lista         = "[" [ argumentos ] "]" ;
diccionario   = "{" [ par { "," par } ] "}" ;
par           = expresion ":" expresion ;
si            = "siProfecia" "(" expresion ")" bloque
                { "otraProfecia" "(" expresion ")" bloque }
                [ "maldicion" bloque ] ;
mientras      = "amarre" "(" expresion ")" bloque ;
para          = "circulo" "(" [ declaracion | expresion ] ";" [ expresion ] ";" [ expresion ] ")" bloque ;
hechizo       = "hechizo" "(" [ IDENT { "," IDENT } ] ")" bloque ;
```

**Léxico:** `IDENT = (letra | "_") { letra | dígito | "_" }` con letras ASCII; `ENTERO = dígito { dígito }`; `DECIMAL = ENTERO "." ENTERO`; `TEXTO = '"' { carácter | escape } '"'` con escapes `\n \t \" \\`; los comentarios van de `//` al fin de la línea; los espacios, tabulaciones y saltos de línea se ignoran.

La gramática de expresiones está escrita por niveles para mostrar la precedencia. En el código **no** hay un método por nivel: el parser de Pratt usa la tabla `Parser.PRECEDENCES` y el enum `Precedence`. Cada fila de la tabla de precedencias del README corresponde a un nivel de esta gramática.

## 2. Arquitectura

```text
            ┌────────┐ tokens ┌────────┐  AST   ┌───────────┐
 texto ───▶ │ Lexer  │ ─────▶ │ Parser │ ─────▶ │ Evaluator │ ───▶ valor / salida
            └────────┘        └────────┘        └───────────┘
             errores           errores            ErrorObj
             léxicos           sintácticos        (ejecución)
                  └─────────── Interpreter ───────────┘  ◀── Main / Repl
```

- **`Lexer`** lee el texto carácter por carácter (`ch` es el actual y `peekChar()` el siguiente) y produce un `Token` con tipo, texto, **línea y columna**. Con `peekChar()` distingue los operadores de dos caracteres (`==`, `<=`, `**`, `&&`). Las palabras se leen completas y luego se buscan en `TokenType.lookupIdent`. Un carácter inválido genera un token `ILLEGAL` y un mensaje en `errors()`.
- **`Parser`** pide tokens al lexer bajo demanda y mira dos a la vez (`cur` y `peek`). Las instrucciones (`pocion`, `conjura`) se leen por descenso recursivo. Las expresiones se leen con **Pratt**: cada tipo de token tiene una función prefija (cómo empieza una expresión) y, si es operador, una infija (cómo continúa). En `parseExpression(p)` el ciclo extiende la expresión mientras el siguiente operador tenga más precedencia que `p`. La asociatividad a la derecha de `**` y `=` se logra leyendo el lado derecho con un nivel menos.
- **AST** (`grimorio.ast`): una clase por construcción. Cada nodo guarda su token para ubicar los errores. `toString()` reconstruye el código con paréntesis y `AstPrinter` dibuja el árbol.
- **`Evaluator`** recorre el AST. El método `eval` despacha según la clase del nodo. Los valores son objetos `Obj` (`IntegerObj`, `ArrayObj`, `FunctionObj`, ...). Dos valores especiales controlan el flujo: `ReturnValue` (sube hasta el llamado del hechizo) y `ErrorObj` (sube hasta el programa y lo detiene).
- **`Environment`** implementa los ámbitos como una cadena de mapas con un enlace al ámbito exterior (`outer`). `define` crea una variable en el ámbito actual (`pocion`); `assign` modifica la variable más cercana que exista (`=`); `get` busca subiendo por la cadena.
- **`Interpreter`** encadena las etapas y conserva el entorno global (lo que permite que el REPL recuerde variables). **`Main`** y **`Repl`** son los puntos de entrada.

## 3. Decisiones de diseño

1. **Port fiel del proyecto base.** Se conservó la arquitectura y los nombres del proyecto en Go (`nextToken`, `parseProgram`, `parseExpression`, `eval`, `applyFunction`, `Environment`) para que cada parte se pueda rastrear al material del curso. La temática solo está en el lenguaje, en los mensajes y en el REPL; las clases de Java tienen nombres estándar.
2. **Java 11 sin dependencias externas.** Solo JUnit para las pruebas. Como Java 11 no tiene `record` ni `switch` con patrones, el evaluador despacha con una cadena de `instanceof`. Es más largo, pero explícito y fácil de seguir.
3. **Posición en cada token.** El token guarda línea y columna, y los nodos del AST guardan su token. Así los errores de las tres etapas indican dónde ocurrieron.
4. **Errores por etapa.** Si el lexer encontró errores, se reportan solo esos (los errores sintácticos que causan serían ruido). Si el parser encontró errores, el programa no se ejecuta. Para reportar varios errores sintácticos de una vez, el parser se recupera: `synchronize()` salta hasta el `;` o la `}` que cierra la instrucción fallida.
5. **Errores de ejecución como valores.** `ErrorObj` se propaga como un valor normal en lugar de usar excepciones de Java, igual que en el proyecto base. La única excepción que se atrapa es `StackOverflowError`, que se reporta como "recursión demasiado profunda".
6. **`&&` y `||` en un nodo propio (`LogicalExpression`).** No pueden ser un `InfixExpression` porque el evaluador debe decidir si evalúa el lado derecho (cortocircuito). Devuelven `luz` o `sombra`.
7. **Ámbito de bloque.** Además de los hechizos, cada bloque `{ }` y cada `circulo` abren un ámbito nuevo. Una `pocion` declarada dentro de un `siProfecia` no existe afuera, y la variable de control de un `circulo` desaparece al terminar el ciclo. Asignar con `=` a una variable no declarada es un error (evita crear variables por un error de tipeo).
8. **Closures por alcance léxico.** `FunctionObj` guarda el entorno donde se creó el hechizo, y al llamarlo el nuevo ámbito tiene ese entorno como exterior (no el del lugar del llamado).
9. **Números.** Los enteros son `long` y el desbordamiento es un error (`Math.addExact`, etc.). Los decimales son `double`. Las operaciones entre un entero y un decimal dan decimal; entre dos enteros, la división es entera. `**` tiene más precedencia que el `-` unario, así que `-2 ** 2` es `-4`, como en matemáticas.
10. **Veracidad.** Solo `sombra` y `vacio` son falsos (convención del proyecto base). `0` y `""` son verdaderos.
11. **Índices.** Leer o escribir fuera de los límites de una lista es un error. Leer una clave inexistente de un diccionario da `vacio`.

## 4. Limitaciones conocidas

- No hay `break` ni `continue`. Para salir de un ciclo se usa `conjura` dentro de un hechizo.
- No hay declaración nombrada de hechizos (`hechizo f() {}`); se guardan en una `pocion`.
- Los identificadores solo admiten letras ASCII; los textos sí admiten tildes y ñ.
- Los decimales usan `double`, por lo que `0.1 + 0.2 == 0.3` es `sombra`.
- `==` compara listas por contenido, pero diccionarios y hechizos por identidad.
- El REPL decide si una entrada está incompleta contando delimitadores abiertos. No detecta, por ejemplo, un `siProfecia (x)` al que le falta el bloque; eso lo reporta el parser como error.
- La profundidad máxima de recursión depende de la pila de la JVM (se puede aumentar con `java -Xss16m -jar ...`).

## 5. Uso de inteligencia artificial

Usamos Claude (Anthropic) como asistente para: generar opciones de temática y vocabulario, traducir a Java la estructura del proyecto base en Go, escribir una primera versión de los componentes, de los programas de ejemplo y de las pruebas, y redactar la documentación.

Verificamos el código así: (1) revisamos cada componente comparándolo con el proyecto base y con la gramática de este documento; (2) ejecutamos la batería de pruebas (`mvnw test`: pruebas unitarias del lexer, parser y evaluador más los programas de `tests/` con su salida esperada), donde cada salida esperada se revisó a mano, por ejemplo el AST de `1 + 2 * 3`, `fibonacci(15) = 610` y `ackermann(2, 2) = 7`; (3) probamos a mano el REPL y los casos de error; (4) cada integrante recorrió el flujo completo de un programa (tokens → AST → evaluación) para poder explicarlo en la sustentación.
