// Generated from .\Molang.g4 by ANTLR 4.9.1
package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MolangParser}.
 */
public interface MolangListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link MolangParser#exprSet}.
     *
     * @param ctx the parse tree
     */
    void enterExprSet(MolangParser.ExprSetContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#exprSet}.
     *
     * @param ctx the parse tree
     */
    void exitExprSet(MolangParser.ExprSetContext ctx);

    /**
     * Enter a parse tree produced by the {@code ternaryConditionalOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code ternaryConditionalOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code orOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterOrOperator(MolangParser.OrOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code orOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitOrOperator(MolangParser.OrOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code singleSignedAtom}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterSingleSignedAtom(MolangParser.SingleSignedAtomContext ctx);

    /**
     * Exit a parse tree produced by the {@code singleSignedAtom}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitSingleSignedAtom(MolangParser.SingleSignedAtomContext ctx);

    /**
     * Enter a parse tree produced by the {@code comparisonOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterComparisonOperator(MolangParser.ComparisonOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code comparisonOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitComparisonOperator(MolangParser.ComparisonOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code assignmentOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterAssignmentOperator(MolangParser.AssignmentOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code assignmentOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code mulOrDiv}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterMulOrDiv(MolangParser.MulOrDivContext ctx);

    /**
     * Exit a parse tree produced by the {@code mulOrDiv}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitMulOrDiv(MolangParser.MulOrDivContext ctx);

    /**
     * Enter a parse tree produced by the {@code addOrSub}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterAddOrSub(MolangParser.AddOrSubContext ctx);

    /**
     * Exit a parse tree produced by the {@code addOrSub}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitAddOrSub(MolangParser.AddOrSubContext ctx);

    /**
     * Enter a parse tree produced by the {@code neExpr}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterNeExpr(MolangParser.NeExprContext ctx);

    /**
     * Exit a parse tree produced by the {@code neExpr}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitNeExpr(MolangParser.NeExprContext ctx);

    /**
     * Enter a parse tree produced by the {@code andOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterAndOperator(MolangParser.AndOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code andOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitAndOperator(MolangParser.AndOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code returnOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterReturnOperator(MolangParser.ReturnOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code returnOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitReturnOperator(MolangParser.ReturnOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code binaryConditionalOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code binaryConditionalOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx);

    /**
     * Enter a parse tree produced by the {@code equalsOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void enterEqualsOperator(MolangParser.EqualsOperatorContext ctx);

    /**
     * Exit a parse tree produced by the {@code equalsOperator}
     * labeled alternative in {@link MolangParser#expr}.
     *
     * @param ctx the parse tree
     */
    void exitEqualsOperator(MolangParser.EqualsOperatorContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#funcParam}.
     *
     * @param ctx the parse tree
     */
    void enterFuncParam(MolangParser.FuncParamContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#funcParam}.
     *
     * @param ctx the parse tree
     */
    void exitFuncParam(MolangParser.FuncParamContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#signedAtom}.
     *
     * @param ctx the parse tree
     */
    void enterSignedAtom(MolangParser.SignedAtomContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#signedAtom}.
     *
     * @param ctx the parse tree
     */
    void exitSignedAtom(MolangParser.SignedAtomContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#atom}.
     *
     * @param ctx the parse tree
     */
    void enterAtom(MolangParser.AtomContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#atom}.
     *
     * @param ctx the parse tree
     */
    void exitAtom(MolangParser.AtomContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#scientific}.
     *
     * @param ctx the parse tree
     */
    void enterScientific(MolangParser.ScientificContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#scientific}.
     *
     * @param ctx the parse tree
     */
    void exitScientific(MolangParser.ScientificContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#function}.
     *
     * @param ctx the parse tree
     */
    void enterFunction(MolangParser.FunctionContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#function}.
     *
     * @param ctx the parse tree
     */
    void exitFunction(MolangParser.FunctionContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#variable}.
     *
     * @param ctx the parse tree
     */
    void enterVariable(MolangParser.VariableContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#variable}.
     *
     * @param ctx the parse tree
     */
    void exitVariable(MolangParser.VariableContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#funcname}.
     *
     * @param ctx the parse tree
     */
    void enterFuncname(MolangParser.FuncnameContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#funcname}.
     *
     * @param ctx the parse tree
     */
    void exitFuncname(MolangParser.FuncnameContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#string}.
     *
     * @param ctx the parse tree
     */
    void enterString(MolangParser.StringContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#string}.
     *
     * @param ctx the parse tree
     */
    void exitString(MolangParser.StringContext ctx);

    /**
     * Enter a parse tree produced by {@link MolangParser#assignment}.
     *
     * @param ctx the parse tree
     */
    void enterAssignment(MolangParser.AssignmentContext ctx);

    /**
     * Exit a parse tree produced by {@link MolangParser#assignment}.
     *
     * @param ctx the parse tree
     */
    void exitAssignment(MolangParser.AssignmentContext ctx);
}