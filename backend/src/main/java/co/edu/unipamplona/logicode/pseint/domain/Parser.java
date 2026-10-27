package co.edu.unipamplona.logicode.pseint.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import co.edu.unipamplona.logicode.pseint.domain.Lexer.Token;
import co.edu.unipamplona.logicode.pseint.domain.Lexer.Type;

/**
 * Lee un programa PSeInt en su perfil flexible: Algoritmo/Proceso, SubProceso y Funcion, Definir, Dimension,
 * asignación con {@code <-}, {@code =} o {@code :=}, Escribir, Leer, Si, Segun, Mientras, Repetir y Para.
 * «Entonces» y «Hacer» son opcionales y «;» separa instrucciones como un salto de línea.
 */
final class Parser {

    private static final int MAX_DEPTH = 100;
    private static final Set<String> TYPES = Set.of("entero", "real", "numero", "numerico", "caracter", "texto",
            "cadena", "logico");
    /** Palabras de estructura que no pueden usarse como valor. */
    private static final Set<String> RESERVED = Set.of("si", "entonces", "sino", "finsi", "mientras", "hacer",
            "finmientras", "para", "hasta", "finpara", "segun", "finsegun", "repetir", "escribir", "leer", "definir",
            "como", "algoritmo", "finalgoritmo", "proceso", "finproceso", "y", "o", "no", "mod", "dimension",
            "subproceso", "finsubproceso", "funcion", "finfuncion");
    private static final Set<String> COMPARISONS = Set.of("=", "==", "<>", "!=", "<", ">", "<=", ">=");

    private final List<Token> tokens;
    private int position;
    private int depth;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    static Ast.Program parse(String code) {
        return new Parser(Lexer.tokens(code)).program();
    }

    private Ast.Program program() {
        List<Ast.Statement> main = null;
        Map<String, Ast.Function> functions = new LinkedHashMap<>();
        skipLines();
        while (peek().type() != Type.END) {
            Token token = peek();
            if (token.is("algoritmo") || token.is("proceso")) {
                if (main != null) {
                    throw syntax(token, "solo puede haber un Algoritmo (o Proceso) por programa.");
                }
                next();
                word("el nombre del algoritmo");
                main = block(() -> peek().is("finalgoritmo") || peek().is("finproceso"), "FinAlgoritmo");
                next();
                endOfLine();
            } else if (token.is("subproceso") || token.is("funcion") || token.is("subalgoritmo")) {
                Ast.Function function = function();
                if (functions.putIfAbsent(Lexer.normalize(function.name()), function) != null) {
                    throw syntax(token, "ya existe un subproceso llamado «" + function.name() + "».");
                }
            } else {
                throw syntax(token, "se esperaba «Algoritmo» y se encontró «" + token.text() + "».");
            }
            skipLines();
        }
        if (main == null) {
            throw new PseintError(PseintError.Kind.SYNTAX, 0, "Falta el bloque «Algoritmo … FinAlgoritmo».");
        }
        return new Ast.Program(main, functions);
    }

    private Ast.Function function() {
        Token start = next();
        Token first = word("el nombre del subproceso");
        String returnVar = null;
        Token name = first;
        if (peek().is("<-") || peek().is("=")) {
            next();
            returnVar = first.key();
            name = word("el nombre de la función");
        }
        List<Ast.Param> params = new ArrayList<>();
        if (accept("(") && !accept(")")) {
            do {
                Token param = word("el nombre de un parámetro");
                boolean byReference = false;
                if (accept("por")) {
                    if (accept("referencia")) {
                        byReference = true;
                    } else if (!accept("valor")) {
                        throw syntax(peek(), "se esperaba «Por Referencia» o «Por Valor».");
                    }
                }
                params.add(new Ast.Param(param.key(), byReference));
            } while (accept(","));
            expect(")", "«)»");
        }
        List<Ast.Statement> body = block(() -> peek().is("finsubproceso") || peek().is("finfuncion")
                || peek().is("finsubalgoritmo"), "Fin" + start.text());
        next();
        endOfLine();
        return new Ast.Function(name.text(), returnVar, params, body, start.line());
    }

    private List<Ast.Statement> block(BooleanSupplier stop, String closing) {
        enter();
        try {
            List<Ast.Statement> statements = new ArrayList<>();
            while (true) {
                skipLines();
                if (peek().type() == Type.END) {
                    throw new PseintError(PseintError.Kind.SYNTAX, peek().line(), "falta «" + closing + "».");
                }
                if (stop.getAsBoolean()) {
                    return statements;
                }
                statement(statements);
                if (peek().type() == Type.END_OF_LINE) {
                    next();
                } else if (!stop.getAsBoolean()) {
                    throw syntax(peek(), "se esperaba el fin de la línea y se encontró «" + peek().text() + "».");
                }
            }
        } finally {
            depth--;
        }
    }

    private void statement(List<Ast.Statement> out) {
        Token token = peek();
        int line = token.line();
        if (token.type() != Type.WORD) {
            throw syntax(token, "instrucción no válida: «" + token.text() + "».");
        }
        switch (token.key()) {
            case "definir" -> {
                next();
                List<String> names = new ArrayList<>();
                do {
                    names.add(word("el nombre de una variable").key());
                } while (accept(","));
                expect("como", "«Como»");
                Token type = word("el tipo (Entero, Real, Caracter o Logico)");
                if (!TYPES.contains(type.key())) {
                    throw syntax(type, "tipo desconocido «" + type.text() + "».");
                }
                out.add(new Ast.Define(names, type.key(), line));
            }
            case "dimension", "dimensionar" -> {
                next();
                do {
                    Token name = word("el nombre del arreglo");
                    expect("[", "«[»");
                    List<Ast.Expression> sizes = expressions();
                    expect("]", "«]»");
                    out.add(new Ast.Dimension(name.key(), sizes, line));
                } while (accept(","));
            }
            case "escribir", "imprimir", "mostrar" -> {
                next();
                boolean newLine = !withoutNewLine();
                List<Ast.Expression> values = atLineEnd() ? List.of() : expressions();
                if (withoutNewLine()) {
                    newLine = false;
                }
                out.add(new Ast.Write(values, newLine, line));
            }
            case "leer" -> {
                next();
                List<Ast.Variable> targets = new ArrayList<>();
                do {
                    targets.add(variable(word("la variable que se lee")));
                } while (accept(","));
                out.add(new Ast.Read(targets, line));
            }
            case "si" -> {
                next();
                Ast.Expression condition = expression();
                accept("entonces");
                List<Ast.Statement> then = block(() -> peek().is("sino") || peek().is("finsi"), "FinSi");
                List<Ast.Statement> otherwise = List.of();
                if (accept("sino")) {
                    otherwise = block(() -> peek().is("finsi"), "FinSi");
                }
                expect("finsi", "«FinSi»");
                out.add(new Ast.If(condition, then, otherwise, line));
            }
            case "segun" -> out.add(switchStatement(line));
            case "mientras" -> {
                next();
                Ast.Expression condition = expression();
                accept("hacer");
                List<Ast.Statement> body = block(() -> peek().is("finmientras"), "FinMientras");
                next();
                out.add(new Ast.While(condition, body, line));
            }
            case "repetir" -> {
                next();
                List<Ast.Statement> body = block(() -> peek().is("hasta")
                        || peek().is("mientras") && peekAt(1).is("que"), "Hasta Que");
                boolean untilTrue = accept("hasta");
                if (!untilTrue) {
                    next();
                }
                expect("que", "«Que»");
                out.add(new Ast.Repeat(body, expression(), untilTrue, line));
            }
            case "para" -> {
                next();
                if (peek().is("cada")) {
                    throw new PseintError(PseintError.Kind.UNSUPPORTED, line,
                            "«Para Cada» aún no se puede ejecutar en LogiCode; usa «Para i <- 1 Hasta n».");
                }
                Token variable = word("la variable del ciclo");
                assignment();
                Ast.Expression from = expression();
                expect("hasta", "«Hasta»");
                Ast.Expression to = expression();
                Ast.Expression step = null;
                if (accept("con")) {
                    expect("paso", "«Paso»");
                    step = expression();
                }
                accept("hacer");
                List<Ast.Statement> body = block(() -> peek().is("finpara"), "FinPara");
                next();
                out.add(new Ast.For(variable.key(), from, to, step, body, line));
            }
            case "esperar" -> {
                while (!atLineEnd()) {
                    next();
                }
                out.add(new Ast.Nothing(line));
            }
            case "borrar", "limpiar" -> {
                next();
                expect("pantalla", "«Pantalla»");
                out.add(new Ast.Nothing(line));
            }
            default -> {
                if (RESERVED.contains(token.key()) || token.key().startsWith("fin")) {
                    throw syntax(token, "«" + token.text() + "» no corresponde aquí.");
                }
                next();
                if (peek().is("(") || atLineEnd()) {
                    out.add(new Ast.CallStatement(call(token), line));
                } else {
                    Ast.Variable target = variable(token);
                    assignment();
                    out.add(new Ast.Assign(target, expression(), line));
                }
            }
        }
    }

    private Ast.Switch switchStatement(int line) {
        next();
        Ast.Expression value = expression();
        accept("hacer");
        List<Ast.Case> cases = new ArrayList<>();
        List<Ast.Statement> otherwise = List.of();
        while (true) {
            skipLines();
            if (accept("finsegun")) {
                break;
            }
            if (peek().is("de") && peekAt(1).is("otro")) {
                next();
                next();
                expect("modo", "«Modo»");
                expect(":", "«:»");
                otherwise = block(() -> peek().is("finsegun"), "FinSegun");
                next();
                break;
            }
            if (peek().type() == Type.END) {
                throw new PseintError(PseintError.Kind.SYNTAX, peek().line(), "falta «FinSegun».");
            }
            accept("caso");
            List<Ast.Expression> values = expressions();
            expect(":", "«:» después de la opción");
            List<Ast.Statement> body = block(() -> peek().is("finsegun")
                    || peek().is("de") && peekAt(1).is("otro") || caseLabelAhead(), "FinSegun");
            cases.add(new Ast.Case(values, body));
        }
        return new Ast.Switch(value, cases, otherwise, line);
    }

    /** Una línea con «:» fuera de un texto es una opción de Segun («1, 2:»). */
    private boolean caseLabelAhead() {
        for (int i = position; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type() == Type.END_OF_LINE || token.type() == Type.END) {
                return false;
            }
            if (token.is(":")) {
                return true;
            }
        }
        return false;
    }

    private boolean withoutNewLine() {
        if (peek().is("sin") && (peekAt(1).is("saltar") || peekAt(1).is("bajar"))) {
            next();
            next();
            return true;
        }
        return false;
    }

    private void assignment() {
        if (!(accept("<-") || accept("=") || accept(":="))) {
            throw syntax(peek(), "se esperaba «<-» y se encontró «" + peek().text() + "».");
        }
    }

    private Ast.Variable variable(Token name) {
        List<Ast.Expression> indices = List.of();
        if (accept("[")) {
            indices = expressions();
            expect("]", "«]»");
        }
        return new Ast.Variable(name.key(), name.text(), indices, name.line());
    }

    private Ast.Call call(Token name) {
        List<Ast.Expression> arguments = List.of();
        if (accept("(") && !accept(")")) {
            arguments = expressions();
            expect(")", "«)»");
        }
        return new Ast.Call(name.key(), name.text(), arguments, name.line());
    }

    private List<Ast.Expression> expressions() {
        List<Ast.Expression> list = new ArrayList<>();
        do {
            list.add(expression());
        } while (accept(","));
        return list;
    }

    private Ast.Expression expression() {
        enter();
        try {
            return or();
        } finally {
            depth--;
        }
    }

    private Ast.Expression or() {
        Ast.Expression left = and();
        while (peek().is("o") || peek().is("|") || peek().is("||")) {
            int line = next().line();
            left = new Ast.Binary("o", left, and(), line);
        }
        return left;
    }

    private Ast.Expression and() {
        Ast.Expression left = not();
        while (peek().is("y") || peek().is("&") || peek().is("&&")) {
            int line = next().line();
            left = new Ast.Binary("y", left, not(), line);
        }
        return left;
    }

    private Ast.Expression not() {
        if (peek().is("no") || peek().is("~")) {
            int line = next().line();
            enter();
            try {
                return new Ast.Unary("no", not(), line);
            } finally {
                depth--;
            }
        }
        return comparison();
    }

    private Ast.Expression comparison() {
        Ast.Expression left = additive();
        if (peek().type() == Type.SYMBOL && COMPARISONS.contains(peek().key())) {
            Token operator = next();
            String key = switch (operator.key()) {
                case "==" -> "=";
                case "!=" -> "<>";
                default -> operator.key();
            };
            left = new Ast.Binary(key, left, additive(), operator.line());
        }
        return left;
    }

    private Ast.Expression additive() {
        Ast.Expression left = multiplicative();
        while (peek().is("+") || peek().is("-")) {
            Token operator = next();
            left = new Ast.Binary(operator.key(), left, multiplicative(), operator.line());
        }
        return left;
    }

    private Ast.Expression multiplicative() {
        Ast.Expression left = unary();
        while (peek().is("*") || peek().is("/") || peek().is("%") || peek().is("mod")) {
            Token operator = next();
            left = new Ast.Binary(operator.is("mod") ? "%" : operator.key(), left, unary(), operator.line());
        }
        return left;
    }

    private Ast.Expression unary() {
        if (peek().is("-") || peek().is("+")) {
            Token operator = next();
            enter();
            try {
                return new Ast.Unary(operator.key(), unary(), operator.line());
            } finally {
                depth--;
            }
        }
        return power();
    }

    private Ast.Expression power() {
        Ast.Expression base = primary();
        if (peek().is("^")) {
            int line = next().line();
            enter();
            try {
                return new Ast.Binary("^", base, unary(), line);
            } finally {
                depth--;
            }
        }
        return base;
    }

    private Ast.Expression primary() {
        Token token = next();
        switch (token.type()) {
            case NUMBER -> {
                return new Ast.Literal(Double.parseDouble(token.text()), token.line());
            }
            case TEXT -> {
                return new Ast.Literal(token.text(), token.line());
            }
            case WORD -> {
                if (token.is("verdadero") || token.is("falso")) {
                    return new Ast.Literal(token.is("verdadero"), token.line());
                }
                if (RESERVED.contains(token.key())) {
                    throw syntax(token, "se esperaba un valor y se encontró «" + token.text() + "».");
                }
                return peek().is("(") ? call(token) : variable(token);
            }
            default -> {
                if (token.is("(")) {
                    Ast.Expression inner = expression();
                    expect(")", "«)»");
                    return inner;
                }
                throw syntax(token, token.type() == Type.END_OF_LINE || token.type() == Type.END
                        ? "falta un valor al final de la línea."
                        : "se esperaba un valor y se encontró «" + token.text() + "».");
            }
        }
    }

    private void enter() {
        if (++depth > MAX_DEPTH) {
            throw new PseintError(PseintError.Kind.LIMIT, peek().line(), "el programa está demasiado anidado.");
        }
    }

    private void skipLines() {
        while (peek().type() == Type.END_OF_LINE) {
            position++;
        }
    }

    private void endOfLine() {
        if (peek().type() != Type.END_OF_LINE && peek().type() != Type.END) {
            throw syntax(peek(), "se esperaba el fin de la línea y se encontró «" + peek().text() + "».");
        }
    }

    private boolean atLineEnd() {
        return peek().type() == Type.END_OF_LINE || peek().type() == Type.END;
    }

    private Token word(String what) {
        Token token = peek();
        if (token.type() != Type.WORD) {
            throw syntax(token, "se esperaba " + what + ".");
        }
        return next();
    }

    private void expect(String key, String display) {
        if (!accept(key)) {
            Token found = peek();
            throw syntax(found, "se esperaba " + display + (atLineEnd() ? " antes del fin de la línea."
                    : " y se encontró «" + found.text() + "»."));
        }
    }

    private boolean accept(String key) {
        if (peek().is(key)) {
            position++;
            return true;
        }
        return false;
    }

    private Token peek() {
        return tokens.get(position);
    }

    private Token peekAt(int offset) {
        return tokens.get(Math.min(position + offset, tokens.size() - 1));
    }

    private Token next() {
        Token token = tokens.get(position);
        if (token.type() != Type.END) {
            position++;
        }
        return token;
    }

    private static PseintError syntax(Token token, String message) {
        return new PseintError(PseintError.Kind.SYNTAX, token.line(), message);
    }
}
