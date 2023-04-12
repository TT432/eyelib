package io.github.tt432.eyelib.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.expressions.MolangAssignment;
import io.github.tt432.eyelib.molang.expressions.MolangExpression;
import io.github.tt432.eyelib.molang.expressions.MolangMultiStatement;
import io.github.tt432.eyelib.molang.expressions.MolangResult;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.molang.math.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * MoLang parser
 * This bad boy parses Molang expressions
 * <a href="https://bedrock.dev/docs/1.19.0.0/1.19.30.23/Molang#Math%20Functions">Molang</a>
 */
@Slf4j
public class MolangParser {
    private static final class H {
        private static final MolangParser instance = new MolangParser();
    }

    public static MolangParser getInstance() {
        return H.instance;
    }

    @Getter
    private static final MolangVariableScope globalScope = new MolangVariableScope();
    public static final ScopeStack scopeStack = new ScopeStack();

    private static final Map<String, Class<? extends MolangFunction>> functions = new HashMap<>();
    private static final Map<String, Constructor<? extends MolangFunction>> constructors = new HashMap<>();

    public static final MolangExpression ZERO = new MolangResult(new Constant(0));
    public static final MolangExpression ONE = new MolangResult(new Constant(1));
    public static final String RETURN = "return ";

    public static MolangDataSource getCurrentDataSource() {
        return scopeStack.last().getDataSource();
    }

    private MolangParser() {
        register(new MolangVariable("math.pi", Math.PI));
        register(new MolangVariable("PI", Math.PI));
        register(new MolangVariable("math.e", Math.E));
        register(new MolangVariable("E", Math.E));

        MolangVariableControl.registerAll(globalScope);
    }

    public void register(MolangVariable variable) {
        globalScope.setVariable(variable.getName(), variable);
    }

    public void register(String funcName, Class<? extends MolangFunction> funcClass) {
        functions.put(funcName, funcClass);
    }

    public void loadConstructors() {
        functions.forEach((name, clazz) -> {
            try {
                constructors.put(name, clazz.getConstructor(MolangValue[].class, String.class));
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public MolangValue parse(String expression) throws MolangException {
        return parseSymbols(breakdownChars(breakdown(expression)));
    }

    public void setValue(String name, DoubleSupplier value) {
        scopeStack.last().setValue(name, value);
    }

    public void setValue(String name, DoubleSupplier value, MolangVariableScope scope) {
        if (scope.containsKey(name)) {
            scope.setValue(name, value);
        }
    }

    public MolangVariable getOrCreateVariable(String name, MolangMultiStatement currentStatement) {
        MolangVariable variable;

        if (currentStatement != null) {
            variable = currentStatement.locals.get(name);

            if (variable != null)
                return variable;
        }

        return getOrCreateVariable(name);
    }

    public MolangExpression parseJson(JsonElement element) throws MolangException {
        if (!element.isJsonPrimitive())
            return ZERO;

        JsonPrimitive primitive = element.getAsJsonPrimitive();

        if (primitive.isNumber())
            return new MolangResult(new Constant(primitive.getAsDouble()));

        if (primitive.isString()) {
            String string = primitive.getAsString();

            if (isString(string)) {
                return new MolangResult(new StringValue(splitString(string)));
            }

            try {
                return new MolangResult(new Constant(Double.parseDouble(string)));
            } catch (NumberFormatException ex) {
                return parseExpression(string);
            }
        }

        return ZERO;
    }

    /**
     * Parse a molang expression
     */
    public MolangExpression parseExpression(String expression) throws MolangException {
        MolangMultiStatement result = null;

        for (String split : expression.trim().split(";")) {
            String trimmed = split.trim();

            if (!trimmed.isEmpty()) {
                if (result == null)
                    result = new MolangMultiStatement();

                result.expressions.add(parseOneLine(trimmed, result));
            }
        }

        if (result == null)
            throw new MolangException("Molang expression cannot be blank!");

        return result;
    }

    /**
     * Parse a single Molang statement
     */
    protected MolangExpression parseOneLine(String expression, MolangMultiStatement currentStatement) throws MolangException {
        if (expression.startsWith(RETURN)) {
            try {
                return new MolangResult(parse(expression.substring(RETURN.length()))).addReturn();
            } catch (Exception e) {
                throw new MolangException("Couldn't parse return '" + expression + "' expression!");
            }
        }

        try {
            List<Object> symbols = breakdownChars(this.breakdown(expression));

            /* Assignment it is */
            if (symbols.size() >= 3 && symbols.get(0) instanceof String name && isVariable(symbols.get(0)) && symbols.get(1).equals("=")) {
                symbols = symbols.subList(2, symbols.size());
                MolangVariable variable;

                if (!scopeStack.last().containsKey(name) && !currentStatement.locals.containsKey(name)) {
                    variable = new MolangVariable(name, 0);
                    currentStatement.locals.put(name, variable);
                } else {
                    variable = getOrCreateVariable(name, currentStatement);
                }

                return new MolangAssignment(variable, parseSymbolsMolang(symbols));
            }

            return new MolangResult(parseSymbolsMolang(symbols));
        } catch (Exception e) {
            throw new MolangException("Couldn't parse '" + expression + "' expression!");
        }
    }

    /**
     * Wrapper around {@link #parseSymbols(List)} to throw {@link MolangException}
     */
    private MolangValue parseSymbolsMolang(List<Object> symbols) throws MolangException {
        try {
            return this.parseSymbols(symbols);
        } catch (Exception e) {
            e.printStackTrace();

            throw new MolangException("Couldn't parse an expression!");
        }
    }

    public String[] breakdown(String expression) {
        expression = expression.replaceAll("\\s+", "");
        String[] chars = expression.split("(?!^)");

        int left = 0;
        int right = 0;

        for (String s : chars) {
            if (s.equals("(")) {
                left++;
            } else if (s.equals(")")) {
                right++;
            }
        }

        if (left != right) {
            throw new IllegalArgumentException("Given expression '" + expression + "' has more uneven amount of parenthesis, " +
                    "there are " + left + " open and " + right + " closed!");
        }

        return chars;
    }


    public List<Object> breakdownChars(String[] chars) {
        List<Object> symbols = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int len = chars.length;

        boolean strStart1 = false;
        boolean strStart2 = false;

        for (int i = 0; i < len; i++) {
            String s = chars[i];

            if (strStart2) {
                if (s.equals("'")) {
                    strStart2 = false;
                }

                buffer.append(s);
                continue;
            }

            if (s.equals("'") && !strStart1) {
                if (buffer.length() > 0) {
                    symbols.add(buffer.toString());
                    buffer = new StringBuilder();
                }

                buffer.append(s);
                strStart2 = true;
                continue;
            }

            if (strStart1) {
                if (s.equals("\"")) {
                    strStart1 = false;
                }

                buffer.append(s);
                continue;
            }

            if (s.equals("\"")) {
                if (buffer.length() > 0) {
                    symbols.add(buffer.toString());
                    buffer = new StringBuilder();
                }

                buffer.append(s);
                strStart1 = true;
                continue;
            }

            boolean longOperator = (i > 0 && isOperator(chars[i - 1] + s));

            if (isOperator(s) || longOperator || s.equals(",")) {
                if (s.equals("-")) {
                    int size = symbols.size();
                    boolean isFirst = (size == 0 && (buffer.length() == 0));
                    boolean isOperatorBehind = (size > 0 &&
                            (isOperator(symbols.get(size - 1)) || symbols.get(size - 1).equals(",")) &&
                            (buffer.length() == 0));

                    if (isFirst || isOperatorBehind) {
                        buffer.append(s);

                        continue;
                    }
                }

                if (longOperator) {
                    s = chars[i - 1] + s;
                    buffer = new StringBuilder(buffer.substring(0, buffer.length() - 1));
                }

                if (buffer.length() > 0) {
                    symbols.add(buffer.toString());
                    buffer = new StringBuilder();
                }

                symbols.add(s);
                continue;
            }

            if (s.equals("(")) {
                if (buffer.length() > 0) {
                    symbols.add(buffer.toString());
                    buffer = new StringBuilder();
                }

                int counter = 1;

                for (int j = i + 1; j < len; j++) {
                    String c = chars[j];

                    if (c.equals("(")) {
                        counter++;
                    } else if (c.equals(")")) {
                        counter--;
                    }

                    if (counter == 0) {
                        symbols.add(breakdownChars(buffer.toString().split("(?!^)")));

                        i = j;
                        buffer = new StringBuilder();

                        break;
                    }
                    buffer.append(c);
                }
            } else {
                buffer.append(s);
            }
        }

        if (buffer.length() > 0) {
            symbols.add(buffer.toString());
        }

        return symbols;
    }

    public MolangValue parseSymbols(List<Object> symbols) throws MolangException {
        MolangValue ternary = tryTernary(symbols);

        if (ternary != null) {
            return ternary;
        }

        int size = symbols.size();

        if (size == 1) {
            return valueFromObject(symbols.get(0));
        }

        if (size == 2) {
            Object first = symbols.get(0);
            Object second = symbols.get(1);

            if ((isVariable(first) || first.equals("-")) && second instanceof List list) {
                return createFunction((String) first, list);
            }
        }

        int lastOp = seekLastOperator(symbols);
        int op = lastOp;

        while (op != -1) {
            int leftOp = seekLastOperator(symbols, op - 1);

            if (leftOp != -1) {
                Operation left = operationForOperator((String) symbols.get(leftOp));
                Operation right = operationForOperator((String) symbols.get(op));

                if (right.value > left.value) {
                    MolangValue leftValue = parseSymbols(symbols.subList(0, leftOp));
                    MolangValue rightValue = parseSymbols(symbols.subList(leftOp + 1, size));

                    return new Operator(left, leftValue, rightValue);
                }

                if (left.value > right.value) {
                    Operation initial = operationForOperator((String) symbols.get(lastOp));

                    if (initial.value < left.value) {
                        MolangValue molangValue1 = parseSymbols(symbols.subList(0, lastOp));
                        MolangValue molangValue2 = parseSymbols(symbols.subList(lastOp + 1, size));

                        return new Operator(initial, molangValue1, molangValue2);
                    }

                    MolangValue leftValue = parseSymbols(symbols.subList(0, op));
                    MolangValue rightValue = parseSymbols(symbols.subList(op + 1, size));

                    return new Operator(right, leftValue, rightValue);
                }
            }

            op = leftOp;
        }

        Operation operation = operationForOperator((String) symbols.get(lastOp));

        return new Operator(operation, parseSymbols(symbols.subList(0, lastOp)),
                parseSymbols(symbols.subList(lastOp + 1, size)));
    }

    protected int seekLastOperator(List<Object> symbols) {
        return seekLastOperator(symbols, symbols.size() - 1);
    }

    protected int seekLastOperator(List<Object> symbols, int offset) {
        for (int i = offset; i >= 0; i--) {
            Object o = symbols.get(i);

            if (isOperator(o)) {
                return i;
            }
        }

        return -1;
    }

    protected int seekFirstOperator(List<Object> symbols) {
        return seekFirstOperator(symbols, 0);
    }

    protected int seekFirstOperator(List<Object> symbols, int offset) {
        for (int i = offset, size = symbols.size(); i < size; i++) {
            Object o = symbols.get(i);

            if (isOperator(o)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 解析三元运算符
     *
     * @param symbols 符号
     * @return 三元运算符
     * @throws MolangException 解析错误
     */
    protected MolangValue tryTernary(List<Object> symbols) throws MolangException {
        int question = -1;
        int questions = 0;
        int colon = -1;
        int colons = 0;
        int size = symbols.size();

        for (int i = 0; i < size; i++) {
            Object object = symbols.get(i);

            if (object instanceof String) {
                if (object.equals("?")) {
                    if (question == -1) {
                        question = i;
                    }

                    questions++;
                } else if (object.equals(":")) {
                    if (colons + 1 == questions && colon == -1) {
                        colon = i;
                    }

                    colons++;
                }
            }
        }

        if (questions == colons && question > 0 && question + 1 < colon && colon < size - 1) {
            return new Ternary(parseSymbols(symbols.subList(0, question)),
                    parseSymbols(symbols.subList(question + 1, colon)),
                    parseSymbols(symbols.subList(colon + 1, size)));
        }

        return null;
    }

    protected MolangValue createFunction(String first, List<Object> args) throws MolangException {
        if (first.equals("!")) {
            return new Negate(parseSymbols(args));
        }

        if (first.startsWith("!") && first.length() > 1) {
            return new Negate(createFunction(first.substring(1), args));
        }

        if (first.equals("-")) {
            return new Negative(new Group(parseSymbols(args)));
        }

        if (first.startsWith("-") && first.length() > 1) {
            return new Negative(createFunction(first.substring(1), args));
        }

        if (!functions.containsKey(first)) {
            first = first.toLowerCase();

            if (!functions.containsKey(first))
                throw new MolangException("Function '" + first + "' couldn't be found!");
        }

        List<MolangValue> values = new ArrayList<>();
        List<Object> buffer = new ArrayList<>();

        if (!args.isEmpty() && !args.get(0).equals("+"))
            buffer.add(args.get(0));

        for (int i = 1; i < args.size(); i++) {
            Object o = args.get(i);

            if (o.equals(",")) {
                values.add(parseSymbols(buffer));
                buffer.clear();
                continue;
            }
            buffer.add(o);
        }

        if (!buffer.isEmpty()) {
            values.add(parseSymbols(buffer));
        }

        try {
            return constructors.get(first).newInstance(values.toArray(new MolangValue[0]), first);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.error("construct function failed : {}", first);
            throw new MolangException(e.getMessage());
        }
    }

    public MolangValue valueFromObject(Object object) throws MolangException {
        if (object instanceof String symbol) {
            if (symbol.startsWith("!")) {
                return new Negate(valueFromObject(symbol.substring(1)));
            }

            if (isDecimal(symbol))
                return new Constant(Double.parseDouble(symbol));

            if (isString(symbol))
                return new StringValue(splitString(symbol));

            if (isVariable(symbol)) {
                if (symbol.startsWith("-")) {
                    symbol = symbol.substring(1);
                    MolangVariable value = getOrCreateVariable(symbol);

                    if (value != null) {
                        return new Negative(value);
                    }
                } else {
                    MolangValue value = getOrCreateVariable(symbol);

                    if (value != null) {
                        return value;
                    }
                }
            }
        } else if (object instanceof List list) {
            return new Group(parseSymbols(list));
        }

        throw new MolangException("Given object couldn't be converted to value! " + object);
    }

    public MolangVariable getOrCreateVariable(String name) {
        return scopeStack.last().getOrCreateVariable(name);
    }

    protected Operation operationForOperator(String op) throws MolangException {
        for (Operation operation : Operation.values()) {
            if (operation.sign.equals(op)) {
                return operation;
            }
        }

        throw new MolangException("There is no such operator '" + op + "'!");
    }

    protected boolean isString(String s) {
        return (s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""));
    }

    protected String splitString(String s) {
        return s.substring(1, s.length() - 1);
    }

    protected boolean isVariable(Object o) {
        return (o instanceof String s && !isDecimal(s) && !isString(s) && !isOperator(s));
    }

    protected boolean isOperator(Object o) {
        return (o instanceof String s && isOperator(s));
    }

    protected boolean isOperator(String s) {
        return (Operation.contains(s) || s.equals("?") || s.equals(":") || s.equals("="));
    }

    protected boolean isDecimal(String s) {
        return s.matches("^-?\\d+(\\.\\d+)?$");
    }
}
