package co.edu.unipamplona.logicode.pseint.domain;

import java.util.List;
import java.util.Map;

/** Árbol del programa: instrucciones y expresiones con la línea donde empiezan. */
final class Ast {

    private Ast() {
    }

    /** {@code functions} por nombre normalizado. */
    record Program(List<Statement> main, Map<String, Function> functions) {
    }

    /** {@code returnVar} es nulo en un SubProceso sin valor de retorno. */
    record Function(String name, String returnVar, List<Param> params, List<Statement> body, int line) {
    }

    record Param(String name, boolean byReference) {
    }

    sealed interface Statement permits Define, Dimension, Assign, Write, Read, If, Switch, While, Repeat, For,
            ForEach, CallStatement, Nothing {
        int line();
    }

    record Define(List<String> names, String type, int line) implements Statement {
    }

    record Dimension(String name, List<Expression> sizes, int line) implements Statement {
    }

    record Assign(Variable target, Expression value, int line) implements Statement {
    }

    record Write(List<Expression> values, boolean newLine, int line) implements Statement {
    }

    record Read(List<Variable> targets, int line) implements Statement {
    }

    record If(Expression condition, List<Statement> then, List<Statement> otherwise, int line) implements Statement {
    }

    record Case(List<Expression> values, List<Statement> body) {
    }

    record Switch(Expression value, List<Case> cases, List<Statement> otherwise, int line) implements Statement {
    }

    record While(Expression condition, List<Statement> body, int line) implements Statement {
    }

    /** {@code untilTrue}: «Hasta Que» repite hasta que la condición sea verdadera; «Mientras Que», mientras lo sea. */
    record Repeat(List<Statement> body, Expression condition, boolean untilTrue, int line) implements Statement {
    }

    record For(String variable, Expression from, Expression to, Expression step, List<Statement> body, int line)
            implements Statement {
    }

    /** «Para Cada elemento De arreglo»: {@code array} es una variable sin índices. */
    record ForEach(String variable, Variable array, List<Statement> body, int line) implements Statement {
    }

    record CallStatement(Call call, int line) implements Statement {
    }

    /** Instrucciones sin efecto en una ejecución sin pantalla: Esperar, Borrar Pantalla. */
    record Nothing(int line) implements Statement {
    }

    sealed interface Expression permits Literal, Variable, Unary, Binary, Call {
        int line();
    }

    /** {@code value}: Double, String o Boolean. */
    record Literal(Object value, int line) implements Expression {
    }

    /** {@code indices} vacío para una variable simple. */
    record Variable(String name, String display, List<Expression> indices, int line) implements Expression {
    }

    record Unary(String operator, Expression operand, int line) implements Expression {
    }

    record Binary(String operator, Expression left, Expression right, int line) implements Expression {
    }

    record Call(String name, String display, List<Expression> arguments, int line) implements Expression {
    }
}
