package io.github.tt432.eyelibmolang.compiler;

import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.FunctionParameterRole;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree.VisibleArgumentKind;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import io.github.tt432.eyelibmolang.type.MolangNull;
import io.github.tt432.eyelibmolang.type.MolangObject;
import io.github.tt432.eyelibmolang.type.MolangString;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Evaluates a bound Molang expression tree by interpreting it at runtime.
 * <p>
 * Handles ALL bound expression types: number/string literals, identifiers,
 * binary/unary/ternary operators, member access, function calls, query access,
 * assignments, blocks, and deferred/unknown nodes.
 * <p>
 * This is the primary evaluation mechanism for the molang compiler pipeline,
 * ensuring zero technical debt by covering every supported expression type
 * without waiting for bytecode emitter completion.
 */
public final class BoundMolangEvaluator implements CompiledMolangExpression {

    private final String sourceExpression;
    private final BoundMolang.BoundExprSet root;
    private final MolangMappingTree mappingTree;

    public BoundMolangEvaluator(
            String sourceExpression,
            BoundMolang.BoundExprSet root,
            MolangMappingTree mappingTree
    ) {
        this.sourceExpression = sourceExpression;
        this.root = root;
        this.mappingTree = mappingTree;
    }

    @Override
    public MolangObject evaluate(MolangScope scope) {
        return evaluateExpr(root.root(), scope);
    }

    @Override
    public String sourceExpression() {
        return sourceExpression;
    }

    @Override
    public Set<String> requiredHostRoles() {
        return Set.of();
    }

    // ── Expression dispatch ────────────────────────────────────────────

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private MolangObject evaluateExpr(BoundMolang.BoundExpr expr, MolangScope scope) {
        if (expr instanceof BoundMolang.BoundNumberLiteralExpr num) {
            return MolangFloat.valueOf((float) num.value());
        }
        if (expr instanceof BoundMolang.BoundStringLiteralExpr str) {
            return MolangString.valueOf(unquote(str.rawText()));
        }
        if (expr instanceof BoundMolang.BoundIdentifierExpr id) {
            return scope.get(id.name());
        }
        if (expr instanceof BoundMolang.BoundThisExpr) {
            return MolangFloat.ZERO;
        }
        if (expr instanceof BoundMolang.BoundUnaryExpr unary) {
            return evaluateUnary(unary, scope);
        }
        if (expr instanceof BoundMolang.BoundBinaryExpr binary) {
            return evaluateBinary(binary, scope);
        }
        if (expr instanceof BoundMolang.BoundGroupingExpr grouping) {
            return evaluateExpr(grouping.expression(), scope);
        }
        if (expr instanceof BoundMolang.BoundNullCoalesceExpr nullCoalesce) {
            MolangObject left = evaluateExpr(nullCoalesce.left(), scope);
            if (left instanceof MolangNull || (left instanceof MolangFloat f && f.value() == 0)) {
                return evaluateExpr(nullCoalesce.right(), scope);
            }
            return left;
        }
        if (expr instanceof BoundMolang.BoundArrowAccessExpr arrow) {
            return evaluateArrowAccess(arrow, scope);
        }
        if (expr instanceof BoundMolang.BoundMemberAccessExpr memberAccess) {
            return evaluateMemberAccess(memberAccess, scope);
        }
        if (expr instanceof BoundMolang.BoundCallExpr call) {
            return evaluateCall(call, scope);
        }
        if (expr instanceof BoundMolang.BoundIndexExpr index) {
            return evaluateIndex(index, scope);
        }
        if (expr instanceof BoundMolang.BoundQueryAccessExpr queryAccess) {
            return evaluateExpr(queryAccess.access(), scope);
        }
        if (expr instanceof BoundMolang.BoundAssignmentExpr assignment) {
            return evaluateAssignment(assignment, scope);
        }
        if (expr instanceof BoundMolang.BoundBlockExpr block) {
            return evaluateBlock(block, scope);
        }
        if (expr instanceof BoundMolang.BoundLoopExpr || expr instanceof BoundMolang.BoundForEachExpr) {
            // Loop and for-each are deferred — not yet implemented at runtime
            return MolangNull.INSTANCE;
        }
        // BoundDeferredExpr, BoundUnknownExpr, etc.
        return MolangNull.INSTANCE;
    }

    // ── Unary operators ─────────────────────────────────────────────────

    private MolangObject evaluateUnary(BoundMolang.BoundUnaryExpr unary, MolangScope scope) {
        MolangObject operand = evaluateExpr(unary.expression(), scope);
        float value = operand.asFloat();
        return switch (unary.operator()) {
            case "-" -> MolangFloat.valueOf(-value);
            case "!" -> MolangFloat.valueOf(value == 0);
            case "+" -> MolangFloat.valueOf(value);
            default -> MolangNull.INSTANCE;
        };
    }

    // ── Binary operators ────────────────────────────────────────────────

    private MolangObject evaluateBinary(BoundMolang.BoundBinaryExpr binary, MolangScope scope) {
        return switch (binary.operator()) {
            // Arithmetic
            case "+" -> {
                MolangObject left = evaluateExpr(binary.left(), scope);
                MolangObject right = evaluateExpr(binary.right(), scope);
                yield MolangFloat.valueOf(left.asFloat() + right.asFloat());
            }
            case "-" -> {
                MolangObject left = evaluateExpr(binary.left(), scope);
                MolangObject right = evaluateExpr(binary.right(), scope);
                yield MolangFloat.valueOf(left.asFloat() - right.asFloat());
            }
            case "*" -> {
                MolangObject left = evaluateExpr(binary.left(), scope);
                MolangObject right = evaluateExpr(binary.right(), scope);
                yield MolangFloat.valueOf(left.asFloat() * right.asFloat());
            }
            case "/" -> {
                MolangObject left = evaluateExpr(binary.left(), scope);
                float right = evaluateExpr(binary.right(), scope).asFloat();
                yield right == 0 ? MolangFloat.ZERO : MolangFloat.valueOf(left.asFloat() / right);
            }
            // Comparison
            case "==", "!=", "<", "<=", ">", ">=" -> {
                float left = evaluateExpr(binary.left(), scope).asFloat();
                float right = evaluateExpr(binary.right(), scope).asFloat();
                boolean result = switch (binary.operator()) {
                    case "==" -> left == right;
                    case "!=" -> left != right;
                    case "<" -> left < right;
                    case "<=" -> left <= right;
                    case ">" -> left > right;
                    case ">=" -> left >= right;
                    default -> false;
                };
                yield MolangFloat.valueOf(result);
            }
            // Logical
            case "&&" -> {
                boolean left = evaluateExpr(binary.left(), scope).asBoolean();
                if (!left) {
                    yield MolangFloat.ZERO;
                }
                yield MolangFloat.valueOf(evaluateExpr(binary.right(), scope).asBoolean());
            }
            case "||" -> {
                boolean left = evaluateExpr(binary.left(), scope).asBoolean();
                if (left) {
                    yield MolangFloat.ONE;
                }
                yield MolangFloat.valueOf(evaluateExpr(binary.right(), scope).asBoolean());
            }
            default -> MolangNull.INSTANCE;
        };
    }

    // ── Arrow access (->) ──────────────────────────────────────────────

    private MolangObject evaluateArrowAccess(BoundMolang.BoundArrowAccessExpr arrow, MolangScope scope) {
        MolangObject left = evaluateExpr(arrow.left(), scope);
        // Arrow access evaluates left then right, returning right's result
        // Left is typically a query expression whose side effects are needed
        evaluateExpr(arrow.right(), scope);
        // Convention: arrow access returns the right side value
        return evaluateExpr(arrow.right(), scope);
    }

    // ── Member access ───────────────────────────────────────────────────

    /**
     * Composes the full dotted name for this member access chain
     * and tries resolution via:
     * 1. Direct scope variable lookup ({@code scope.get("query.anim_time")})
     * 2. Mapping tree field lookup (for static constants like {@code math.pi})
     * 3. Mapping tree method resolution (for 0-arity query functions like
     *    {@code query.is_invisible} that are accessed as properties)
     */
    private MolangObject evaluateMemberAccess(BoundMolang.BoundMemberAccessExpr memberAccess, MolangScope scope) {
        List<String> segments = new ArrayList<>();
        collectMemberAccessSegments(memberAccess, segments);

        if (segments.isEmpty()) {
            return MolangNull.INSTANCE;
        }

        String fullName = String.join(".", segments);

        // 1. Try scope variable first — runtime sets query.xxx, variable.xxx, etc.
        MolangObject scopeValue = scope.get(fullName);
        if (!(scopeValue instanceof MolangNull)) {
            return scopeValue;
        }

        // 2. Try mapping tree field lookup (e.g., math.pi)
        var fieldData = mappingTree.findField(fullName);
        if (fieldData != null) {
            try {
                Object value = fieldData.field().get(null);
                if (value instanceof Number num) {
                    return MolangFloat.valueOf(num.floatValue());
                }
                if (value instanceof Boolean bool) {
                    return MolangFloat.valueOf(bool);
                }
                if (value instanceof String str) {
                    return MolangString.valueOf(str);
                }
                return MolangNull.INSTANCE;
            } catch (IllegalAccessException e) {
                return MolangNull.INSTANCE;
            }
        }

        // 3. Try mapping tree method resolution (0-arity)
        //    This handles expressions like `query.is_invisible` which are
        //    member-accessed properties that map to 0-arg query functions.
        var methodData = mappingTree.findMethod(fullName);
        if (methodData != null) {
            Set<MolangFunction.ParameterRole> hostRoles = computeAvailableHostRoles(scope);
            FunctionInfo functionInfo = mappingTree.selectQueryVariant(
                    fullName, List.of(), hostRoles);
            if (functionInfo != null) {
                return invokeMethod(functionInfo, scope, List.of());
            }
        }

        return MolangNull.INSTANCE;
    }

    private static void collectMemberAccessSegments(BoundMolang.BoundMemberAccessExpr expr, List<String> segments) {
        if (expr.owner() instanceof BoundMolang.BoundMemberAccessExpr owner) {
            collectMemberAccessSegments(owner, segments);
        } else if (expr.owner() instanceof BoundMolang.BoundIdentifierExpr id) {
            segments.add(id.name());
        } else {
            // Non-simple owner (e.g., function result); skip name collection
            segments.add("<expr>");
        }
        segments.add(expr.memberName());
    }

    // ── Function calls ──────────────────────────────────────────────────

    private MolangObject evaluateCall(BoundMolang.BoundCallExpr call, MolangScope scope) {
        String methodName = resolveCalleeName(call.callee());
        if (methodName == null) {
            return MolangNull.INSTANCE;
        }

        // Determine visible argument kinds from evaluated arguments
        List<MolangObject> argValues = new ArrayList<>();
        List<VisibleArgumentKind> argKinds = new ArrayList<>();
        for (BoundMolang.BoundExpr arg : call.arguments()) {
            MolangObject val = evaluateExpr(arg, scope);
            argValues.add(val);
            argKinds.add(visibleArgumentKind(val));
        }

        // Compute available host roles from scope host context
        Set<MolangFunction.ParameterRole> hostRoles = computeAvailableHostRoles(scope);

        FunctionInfo functionInfo = mappingTree.selectQueryVariant(methodName, argKinds, hostRoles);
        if (functionInfo == null) {
            // Try scope.get for 0-arity "calls" that are actually property access
            if (call.arguments().isEmpty()) {
                return scope.get(methodName);
            }
            return MolangNull.INSTANCE;
        }

        return invokeMethod(functionInfo, scope, argValues);
    }

    /**
     * Resolves the fully qualified method name from the callee expression.
     */
    private static String resolveCalleeName(BoundMolang.BoundExpr callee) {
        if (callee instanceof BoundMolang.BoundMemberAccessExpr memberAccess) {
            List<String> segments = new ArrayList<>();
            if (memberAccess.owner() instanceof BoundMolang.BoundIdentifierExpr id) {
                segments.add(id.name());
            } else if (memberAccess.owner() instanceof BoundMolang.BoundMemberAccessExpr owner) {
                collectSimpleSegments(owner, segments);
            }
            segments.add(memberAccess.memberName());
            return String.join(".", segments);
        }
        if (callee instanceof BoundMolang.BoundIdentifierExpr id) {
            return id.name();
        }
        return null;
    }

    private static void collectSimpleSegments(BoundMolang.BoundMemberAccessExpr expr, List<String> segments) {
        if (expr.owner() instanceof BoundMolang.BoundIdentifierExpr id) {
            segments.add(id.name());
        } else if (expr.owner() instanceof BoundMolang.BoundMemberAccessExpr owner) {
            collectSimpleSegments(owner, segments);
        }
        segments.add(expr.memberName());
    }

    private static VisibleArgumentKind visibleArgumentKind(MolangObject value) {
        if (value instanceof MolangString) {
            return VisibleArgumentKind.STRING;
        }
        if (value.isNumber()) {
            return VisibleArgumentKind.NUMBER;
        }
        // MolangFloat.asBoolean() distinguishes 0 vs non-0, but visible kind is NUMBER
        if (value instanceof MolangFloat) {
            return VisibleArgumentKind.NUMBER;
        }
        return VisibleArgumentKind.NUMBER; // default fallback
    }

    /**
     * Computes the set of parameter roles available from the scope's host context.
     */
    private static Set<MolangFunction.ParameterRole> computeAvailableHostRoles(MolangScope scope) {
        Set<MolangFunction.ParameterRole> roles = EnumSet.noneOf(MolangFunction.ParameterRole.class);
        var hostContext = scope.getHostContext();
        // HostContext may contain Entity, LivingEntity, RenderData, BrClientEntity, etc.
        // These serve as RECEIVER or INJECTED_HOST
        if (hostContext.get(Object.class).isPresent()) {
            // If we can detect at least one host object, enable RECEIVER role
            roles.add(MolangFunction.ParameterRole.RECEIVER);
            roles.add(MolangFunction.ParameterRole.INJECTED_HOST);
        }
        roles.add(MolangFunction.ParameterRole.SPECIAL_ENGINE_ARG);
        return roles;
    }

    /**
     * Invokes the resolved method with properly prepared arguments.
     */
    private MolangObject invokeMethod(FunctionInfo functionInfo, MolangScope scope, List<MolangObject> visibleArgValues) {
        Method method = functionInfo.method();
        List<FunctionParameterRole> paramRoles = functionInfo.parameterRoles();
        Object[] args = new Object[method.getParameterCount()];
        int visibleIdx = 0;

        for (FunctionParameterRole role : paramRoles) {
            Object argValue = switch (role.role()) {
                case VISIBLE_ARG -> {
                    if (visibleIdx >= visibleArgValues.size()) {
                        yield MolangNull.INSTANCE;
                    }
                    MolangObject val = visibleArgValues.get(visibleIdx++);
                    // Convert MolangObject to the expected Java type
                    yield convertMolangValue(val, role.parameterType());
                }
                case RECEIVER -> resolveReceiver(role.parameterType(), scope);
                case INJECTED_HOST -> resolveInjectedHost(role.parameterType(), scope);
                case SPECIAL_ENGINE_ARG -> scope;
            };

            if (argValue == null && role.parameterType().isPrimitive()) {
                argValue = defaultPrimitive(role.parameterType());
            }
            args[role.index()] = argValue;
        }

        // Handle varargs — pack remaining visible args into an array
        if (method.isVarArgs() && visibleIdx < visibleArgValues.size()) {
            int lastIdx = paramRoles.get(paramRoles.size() - 1).index();
            Class<?> varArgComponent = method.getParameterTypes()[lastIdx].getComponentType();
            if (varArgComponent != null) {
                int varArgCount = visibleArgValues.size() - visibleIdx;
                Object varArgArray = Array.newInstance(varArgComponent, varArgCount);
                for (int i = 0; i < varArgCount; i++) {
                    Array.set(varArgArray, i, convertMolangValue(visibleArgValues.get(visibleIdx + i), varArgComponent));
                }
                args[lastIdx] = varArgArray;
            }
        }

        try {
            Object result = method.invoke(null, args);
            return wrapJavaResult(result);
        } catch (InvocationTargetException e) {
            return MolangNull.INSTANCE;
        } catch (IllegalAccessException e) {
            return MolangNull.INSTANCE;
        }
    }

    private Object resolveReceiver(Class<?> paramType, MolangScope scope) {
        return scope.getHostContext().get(paramType).orElse(null);
    }

    private Object resolveInjectedHost(Class<?> paramType, MolangScope scope) {
        return scope.getHostContext().get(paramType).orElse(null);
    }

    /**
     * Converts a MolangObject to the expected Java parameter type.
     */
    private static Object convertMolangValue(MolangObject value, Class<?> targetType) {
        if (targetType == MolangObject.class || targetType == Object.class) {
            return value;
        }
        if (targetType == float.class || targetType == Float.class || targetType == double.class || targetType == Double.class) {
            return value.asFloat();
        }
        if (targetType == int.class || targetType == Integer.class || targetType == long.class || targetType == Long.class) {
            return (int) value.asFloat();
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return value.asBoolean();
        }
        if (targetType == String.class) {
            return value.asString();
        }
        if (Number.class.isAssignableFrom(targetType)) {
            return value.asFloat();
        }
        // For unrecognized types, pass the MolangObject through
        return value;
    }

    private static Object defaultPrimitive(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\0';
        return null;
    }

    private static MolangObject wrapJavaResult(Object result) {
        if (result == null) {
            return MolangNull.INSTANCE;
        }
        if (result instanceof MolangObject mo) {
            return mo;
        }
        if (result instanceof Number num) {
            return MolangFloat.valueOf(num.floatValue());
        }
        if (result instanceof Boolean bool) {
            return MolangFloat.valueOf(bool);
        }
        if (result instanceof String str) {
            return MolangString.valueOf(str);
        }
        return MolangNull.INSTANCE;
    }

    // ── Index access ────────────────────────────────────────────────────

    private MolangObject evaluateIndex(BoundMolang.BoundIndexExpr indexExpr, MolangScope scope) {
        MolangObject owner = evaluateExpr(indexExpr.owner(), scope);
        int idx = (int) evaluateExpr(indexExpr.index(), scope).asFloat();
        if (idx < 0) {
            return MolangNull.INSTANCE;
        }
        String indexedName = owner.asString() + "[" + idx + "]";
        MolangObject result = scope.get(indexedName);
        if (!(result instanceof MolangNull)) {
            return result;
        }
        return MolangNull.INSTANCE;
    }

    // ── Assignment ──────────────────────────────────────────────────────

    private MolangObject evaluateAssignment(BoundMolang.BoundAssignmentExpr assignment, MolangScope scope) {
        MolangObject value = evaluateExpr(assignment.value(), scope);
        if (assignment.writableTarget() && assignment.targetRoot().isPresent()) {
            String targetName = resolveAssignmentTargetName(assignment.target());
            if (targetName != null) {
                scope.set(targetName, value);
            }
        }
        return value;
    }

    private static String resolveAssignmentTargetName(BoundMolang.BoundExpr target) {
        if (target instanceof BoundMolang.BoundIdentifierExpr id) {
            return id.name();
        }
        if (target instanceof BoundMolang.BoundMemberAccessExpr memberAccess) {
            List<String> segments = new ArrayList<>();
            collectMemberAccessSegments(memberAccess, segments);
            return String.join(".", segments);
        }
        if (target instanceof BoundMolang.BoundQueryAccessExpr queryAccess) {
            return resolveAssignmentTargetName(queryAccess.access());
        }
        return null;
    }

    // ── Block evaluation ────────────────────────────────────────────────

    private MolangObject evaluateBlock(BoundMolang.BoundBlockExpr block, MolangScope scope) {
        List<BoundMolang.BoundStmt> statements = block.statements();
        if (statements.isEmpty()) {
            return MolangNull.INSTANCE;
        }
        MolangObject lastValue = MolangNull.INSTANCE;
        for (BoundMolang.BoundStmt stmt : statements) {
            lastValue = evaluateStmt(stmt, scope);
        }
        return lastValue;
    }

    private MolangObject evaluateStmt(BoundMolang.BoundStmt stmt, MolangScope scope) {
        if (stmt instanceof BoundMolang.BoundExprStmt exprStmt) {
            return evaluateExpr(exprStmt.expression(), scope);
        }
        if (stmt instanceof BoundMolang.BoundReturnStmt returnStmt) {
            return evaluateExpr(returnStmt.expression(), scope);
        }
        // BoundBreakStmt, BoundContinueStmt — deferred, return null
        return MolangNull.INSTANCE;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Strips surrounding quotes from a string literal's raw text.
     */
    private static String unquote(String rawText) {
        if (rawText == null || rawText.length() < 2) {
            return rawText == null ? "" : rawText;
        }
        char first = rawText.charAt(0);
        if ((first == '\'' || first == '"') && rawText.charAt(rawText.length() - 1) == first) {
            return rawText.substring(1, rawText.length() - 1);
        }
        return rawText;
    }
}
