package io.github.tt432.eyelib.molang.mapping.api;

import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionInfo;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.FunctionParameterRole;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.VisibleArgumentKind;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.MethodData;
import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree.PublicationSignature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * 查询函数变体选择策略。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VariantSelector {

    @Nullable
    static FunctionInfo selectQueryVariant(
            MolangMappingTree tree,
            String name,
            List<VisibleArgumentKind> visibleArgumentCallShape,
            Set<MolangFunction.ParameterRole> availableHostRoles
    ) {
        MethodData methodData = tree.findMethod(name);
        if (methodData == null) {
            return null;
        }

        FunctionInfo result = null;
        int bestSpecificity = Integer.MIN_VALUE;
        int bestPriority = Integer.MIN_VALUE;
        int visibleArity = visibleArgumentCallShape.size();

        for (FunctionInfo candidate : methodData.functionInfos()) {
            if (!visibleArityMatches(candidate, visibleArity)) {
                continue;
            }
            if (!visibleArgumentsCompatible(candidate, visibleArgumentCallShape)) {
                continue;
            }
            if (!requiredHostRolesAvailable(candidate, availableHostRoles)) {
                continue;
            }
            int specificity = variantSpecificity(candidate);
            int priority = variantPriority(candidate);
            if (specificity > bestSpecificity) {
                bestSpecificity = specificity;
                bestPriority = priority;
                result = candidate;
            } else if (specificity == bestSpecificity && priority >= bestPriority) {
                bestPriority = priority;
                result = candidate;
            }
        }

        return result;
    }

    private static boolean visibleArityMatches(FunctionInfo functionInfo, int visibleArity) {
        PublicationSignature signature = MolangMappingTree.publicationSignature(functionInfo);
        if (signature.varArgs()) {
            return visibleArity >= signature.visibleArity();
        }
        return visibleArity == signature.visibleArity();
    }

    private static boolean visibleArgumentsCompatible(FunctionInfo functionInfo, List<VisibleArgumentKind> visibleArgumentCallShape) {
        List<? extends Class<?>> visibleParameterTypes = visibleParameterTypes(functionInfo);
        if (!functionInfo.method().isVarArgs() || visibleParameterTypes.isEmpty()) {
            if (visibleParameterTypes.size() != visibleArgumentCallShape.size()) {
                return false;
            }

            for (int i = 0; i < visibleArgumentCallShape.size(); i++) {
                if (!visibleArgumentCompatible(visibleParameterTypes.get(i), visibleArgumentCallShape.get(i))) {
                    return false;
                }
            }
            return true;
        }

        FunctionParameterRole lastRole = functionInfo.parameterRoles().get(functionInfo.parameterRoles().size() - 1);
        if (lastRole.role() != MolangFunction.ParameterRole.VISIBLE_ARG) {
            if (visibleParameterTypes.size() != visibleArgumentCallShape.size()) {
                return false;
            }

            for (int i = 0; i < visibleArgumentCallShape.size(); i++) {
                if (!visibleArgumentCompatible(visibleParameterTypes.get(i), visibleArgumentCallShape.get(i))) {
                    return false;
                }
            }
            return true;
        }

        int fixedVisibleCount = visibleParameterTypes.size() - 1;
        if (visibleArgumentCallShape.size() < fixedVisibleCount) {
            return false;
        }

        for (int i = 0; i < fixedVisibleCount; i++) {
            if (!visibleArgumentCompatible(visibleParameterTypes.get(i), visibleArgumentCallShape.get(i))) {
                return false;
            }
        }

        Class<?> varArgComponentType = visibleParameterTypes.get(visibleParameterTypes.size() - 1).getComponentType();
        if (varArgComponentType == null) {
            return false;
        }

        for (int i = fixedVisibleCount; i < visibleArgumentCallShape.size(); i++) {
            if (!visibleArgumentCompatible(varArgComponentType, visibleArgumentCallShape.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean requiredHostRolesAvailable(FunctionInfo functionInfo, Set<MolangFunction.ParameterRole> availableHostRoles) {
        for (FunctionParameterRole parameterRole : functionInfo.parameterRoles()) {
            MolangFunction.ParameterRole role = parameterRole.role();
            if (role == MolangFunction.ParameterRole.VISIBLE_ARG) {
                continue;
            }
            if (!availableHostRoles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    private static int variantSpecificity(FunctionInfo functionInfo) {
        return functionInfo.molangFunction() == null ? 0 : functionInfo.molangFunction().specificity();
    }

    private static int variantPriority(FunctionInfo functionInfo) {
        return functionInfo.molangFunction() == null ? 0 : functionInfo.molangFunction().priority();
    }

    private static List<? extends Class<?>> visibleParameterTypes(FunctionInfo functionInfo) {
        return functionInfo.parameterRoles().stream()
                .filter(parameterRole -> parameterRole.role() == MolangFunction.ParameterRole.VISIBLE_ARG)
                .map(FunctionParameterRole::parameterType)
                .toList();
    }

    private static boolean visibleArgumentCompatible(Class<?> parameterType, VisibleArgumentKind callShapeKind) {
        VisibleArgumentKind parameterKind = visibleArgumentKind(parameterType);
        return parameterKind == callShapeKind;
    }

    private static @Nullable VisibleArgumentKind visibleArgumentKind(Class<?> parameterType) {
        if (parameterType.isArray()) {
            return visibleArgumentKind(parameterType.getComponentType());
        }

        if (parameterType == boolean.class || parameterType == Boolean.class) {
            return VisibleArgumentKind.BOOLEAN;
        }

        if (parameterType == String.class || parameterType == char.class || parameterType == Character.class) {
            return VisibleArgumentKind.STRING;
        }

        if (parameterType.isPrimitive() || Number.class.isAssignableFrom(parameterType)) {
            return VisibleArgumentKind.NUMBER;
        }

        return null;
    }
}