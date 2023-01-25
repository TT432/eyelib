package software.bernie.geckolib3.core.molang;

import software.bernie.geckolib3.core.molang.math.*;
import software.bernie.geckolib3.core.molang.math.functions.Function;
import software.bernie.geckolib3.core.molang.math.functions.classic.*;
import software.bernie.geckolib3.core.molang.math.functions.limit.Clamp;
import software.bernie.geckolib3.core.molang.math.functions.limit.Max;
import software.bernie.geckolib3.core.molang.math.functions.limit.Min;
import software.bernie.geckolib3.core.molang.math.functions.rounding.Ceil;
import software.bernie.geckolib3.core.molang.math.functions.rounding.Floor;
import software.bernie.geckolib3.core.molang.math.functions.rounding.Round;
import software.bernie.geckolib3.core.molang.math.functions.rounding.Trunc;
import software.bernie.geckolib3.core.molang.math.functions.utility.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import software.bernie.geckolib3.core.molang.expressions.MolangAssignment;
import software.bernie.geckolib3.core.molang.expressions.MolangExpression;
import software.bernie.geckolib3.core.molang.expressions.MolangMultiStatement;
import software.bernie.geckolib3.core.molang.expressions.MolangValue;
import software.bernie.geckolib3.core.molang.functions.CosDegrees;
import software.bernie.geckolib3.core.molang.functions.SinDegrees;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * MoLang parser
 * This bad boy parses Molang expressions
 * https://bedrock.dev/docs/1.19.0.0/1.19.30.23/Molang#Math%20Functions
 */
public class MolangParser {
    public static final Map<String, LazyVariable> VARIABLES = new Object2ObjectOpenHashMap<>();

    public Map<String, Class<? extends Function>> functions = new HashMap<>();

    public static final MolangExpression ZERO = new MolangValue(null, new Constant(0));
    public static final MolangExpression ONE = new MolangValue(null, new Constant(1));
    public static final String RETURN = "return ";

    public MolangParser() {
        registerVariables();
        registerFunctions();
    }

    void registerVariables() {
        register(new LazyVariable("PI", Math.PI));
        register(new LazyVariable("E", Math.E));
        register(new LazyVariable("query.anim_time", 0));
        register(new LazyVariable("query.actor_count", 0));
        register(new LazyVariable("query.health", 0));
        register(new LazyVariable("query.max_health", 0));
        register(new LazyVariable("query.distance_from_camera", 0));
        register(new LazyVariable("query.yaw_speed", 0));
        register(new LazyVariable("query.is_in_water_or_rain", 0));
        register(new LazyVariable("query.is_in_water", 0));
        register(new LazyVariable("query.is_on_ground", 0));
        register(new LazyVariable("query.time_of_day", 0));
        register(new LazyVariable("query.is_on_fire", 0));
        register(new LazyVariable("query.ground_speed", 0));
    }

    void registerFunctions() {
        this.functions.put("math.floor", Floor.class);
        this.functions.put("math.round", Round.class);
        this.functions.put("math.ceil", Ceil.class);
        this.functions.put("math.trunc", Trunc.class);

        this.functions.put("math.clamp", Clamp.class);
        this.functions.put("math.max", Max.class);
        this.functions.put("math.min", Min.class);

        this.functions.put("math.abs", Abs.class);
        this.functions.put("math.cos", CosDegrees.class);
        this.functions.put("math.sin", SinDegrees.class);
        this.functions.put("math.acos", ACos.class);
        this.functions.put("math.asin", ASin.class);
        this.functions.put("math.atan", ATan.class);
        this.functions.put("math.atan2", ATan2.class);
        this.functions.put("math.exp", Exp.class);
        this.functions.put("math.ln", Ln.class);
        this.functions.put("math.sqrt", Sqrt.class);
        this.functions.put("math.mod", Mod.class);
        this.functions.put("math.pow", Pow.class);

        this.functions.put("math.lerp", Lerp.class);
        this.functions.put("math.lerprotate", LerpRotate.class);
        this.functions.put("math.hermite_blend", HermiteBlend.class);
        this.functions.put("math.die_roll", DieRoll.class);
        this.functions.put("math.die_roll_integer", DieRollInteger.class);
        this.functions.put("math.random", RandomDouble.class);
        this.functions.put("math.random_integer", RandomInteger.class);
    }

    public void register(LazyVariable variable) {
        VARIABLES.put(variable.getName(), variable);
    }

    public IValue parse(String expression) throws Exception {
        return parseSymbols(breakdownChars(breakdown(expression)));
    }

    public void setValue(String name, DoubleSupplier value) {
        LazyVariable variable = getVariable(name);

        if (variable != null)
            variable.set(value);
    }

    public LazyVariable getVariable(String name, MolangMultiStatement currentStatement) {
        LazyVariable variable;

        if (currentStatement != null) {
            variable = currentStatement.locals.get(name);

            if (variable != null)
                return variable;
        }

        return getVariable(name);
    }

    public MolangExpression parseJson(JsonElement element) throws MolangException {
        if (!element.isJsonPrimitive())
            return ZERO;

        JsonPrimitive primitive = element.getAsJsonPrimitive();

        if (primitive.isNumber())
            return new MolangValue(this, new Constant(primitive.getAsDouble()));

        if (primitive.isString()) {
            String string = primitive.getAsString();

            try {
                return new MolangValue(this, new Constant(Double.parseDouble(string)));
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

        for (String split : expression.toLowerCase().trim().split(";")) {
            String trimmed = split.trim();

            if (!trimmed.isEmpty()) {
                if (result == null)
                    result = new MolangMultiStatement(this);

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
                return new MolangValue(this, parse(expression.substring(RETURN.length()))).addReturn();
            } catch (Exception e) {
                throw new MolangException("Couldn't parse return '" + expression + "' expression!");
            }
        }

        try {
            List<Object> symbols = breakdownChars(this.breakdown(expression));

            /* Assignment it is */
            if (symbols.size() >= 3 && symbols.get(0) instanceof String name && isVariable(symbols.get(0)) && symbols.get(1).equals("=")) {
                symbols = symbols.subList(2, symbols.size());
                LazyVariable variable;

                if (!VARIABLES.containsKey(name) && !currentStatement.locals.containsKey(name)) {
                    variable = new LazyVariable(name, 0);
                    currentStatement.locals.put(name, variable);
                } else {
                    variable = getVariable(name, currentStatement);
                }

                return new MolangAssignment(this, variable, parseSymbolsMolang(symbols));
            }

            return new MolangValue(this, parseSymbolsMolang(symbols));
        } catch (Exception e) {
            throw new MolangException("Couldn't parse '" + expression + "' expression!");
        }
    }

    /**
     * Wrapper around {@link #parseSymbols(List)} to throw {@link MolangException}
     */
    private IValue parseSymbolsMolang(List<Object> symbols) throws MolangException {
        try {
            return this.parseSymbols(symbols);
        } catch (Exception e) {
            e.printStackTrace();

            throw new MolangException("Couldn't parse an expression!");
        }
    }

    public String[] breakdown(String expression) throws Exception {
        if (!expression.matches("^[\\w\\d\\s_+-/*%^&|<>=!?:.,()]+$")) {
            throw new Exception("Given expression '" + expression + "' contains illegal characters!");
        }


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
            throw new Exception("Given expression '" + expression + "' has more uneven amount of parenthesis, there are " + left + " open and " + right + " closed!");
        }


        return chars;
    }


    public List<Object> breakdownChars(String[] chars) {
        List<Object> symbols = new ArrayList<>();
        String buffer = "";
        int len = chars.length;

        for (int i = 0; i < len; i++) {
            String s = chars[i];
            boolean longOperator = (i > 0 && isOperator(chars[i - 1] + s));

            if (isOperator(s) || longOperator || s.equals(",")) {


                if (s.equals("-")) {
                    int size = symbols.size();

                    boolean isFirst = (size == 0 && buffer.isEmpty());


                    boolean isOperatorBehind = (size > 0 && (isOperator(symbols.get(size - 1)) || symbols.get(size - 1).equals(",")) && buffer.isEmpty());

                    if (isFirst || isOperatorBehind) {
                        buffer = buffer + s;

                        continue;
                    }
                }

                if (longOperator) {
                    s = chars[i - 1] + s;
                    buffer = buffer.substring(0, buffer.length() - 1);
                }


                if (!buffer.isEmpty()) {
                    symbols.add(buffer);
                    buffer = "";
                }

                symbols.add(s);
                continue;
            }
            if (s.equals("(")) {

                if (!buffer.isEmpty()) {
                    symbols.add(buffer);
                    buffer = "";
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
                        symbols.add(breakdownChars(buffer.split("(?!^)")));

                        i = j;
                        buffer = "";

                        break;
                    }
                    buffer = buffer + c;
                }
            } else {
                buffer = buffer + s;
            }
        }
        if (!buffer.isEmpty()) {
            symbols.add(buffer);
        }

        return symbols;
    }


    public IValue parseSymbols(List<Object> symbols) throws Exception {
        IValue ternary = tryTernary(symbols);

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
                    IValue leftValue = parseSymbols(symbols.subList(0, leftOp));
                    IValue rightValue = parseSymbols(symbols.subList(leftOp + 1, size));

                    return new Operator(left, leftValue, rightValue);
                }
                if (left.value > right.value) {
                    Operation initial = operationForOperator((String) symbols.get(lastOp));

                    if (initial.value < left.value) {
                        IValue iValue1 = parseSymbols(symbols.subList(0, lastOp));
                        IValue iValue2 = parseSymbols(symbols.subList(lastOp + 1, size));

                        return new Operator(initial, iValue1, iValue2);
                    }

                    IValue leftValue = parseSymbols(symbols.subList(0, op));
                    IValue rightValue = parseSymbols(symbols.subList(op + 1, size));

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


    protected IValue tryTernary(List<Object> symbols) throws Exception {
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


    protected IValue createFunction(String first, List<Object> args) throws Exception {
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

        if (!this.functions.containsKey(first)) {
            throw new Exception("Function '" + first + "' couldn't be found!");
        }

        List<IValue> values = new ArrayList<>();
        List<Object> buffer = new ArrayList();

        for (Object o : args) {
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

        Class<? extends Function> function = this.functions.get(first);
        Constructor<? extends Function> ctor = function.getConstructor(IValue[].class, String.class);
        return ctor.newInstance(values.toArray(new IValue[0]), first);
    }

    public IValue valueFromObject(Object object) throws Exception {
        if (object instanceof String symbol) {
            if (symbol.startsWith("!")) {
                return new Negate(valueFromObject(symbol.substring(1)));
            }

            if (isDecimal(symbol))
                return new Constant(Double.parseDouble(symbol));
            if (isVariable(symbol)) {
                if (symbol.startsWith("-")) {
                    symbol = symbol.substring(1);
                    Variable value = getVariable(symbol);

                    if (value != null) {
                        return new Negative(value);
                    }
                } else {
                    IValue value = getVariable(symbol);


                    if (value != null) {
                        return value;
                    }
                }
            }
        } else if (object instanceof List list) {
            return new Group(parseSymbols(list));
        }

        throw new Exception("Given object couldn't be converted to value! " + object);
    }

    protected LazyVariable getVariable(String name) {
        return VARIABLES.computeIfAbsent(name, key -> new LazyVariable(key, 0));
    }

    protected Operation operationForOperator(String op) throws Exception {
        for (Operation operation : Operation.values()) {
            if (operation.sign.equals(op)) {
                return operation;
            }
        }

        throw new Exception("There is no such operator '" + op + "'!");
    }

    protected boolean isVariable(Object o) {
        return (o instanceof String s && !isDecimal(s) && !isOperator(s));
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
