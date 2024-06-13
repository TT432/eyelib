// Generated from Molang.g4 by ANTLR 4.13.1
 package io.github.tt432.eyelib.molang.grammer;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MolangParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MolangVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link MolangParser#exprSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprSet(MolangParser.ExprSetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ternaryConditionalOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryConditionalOperator(MolangParser.TernaryConditionalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleSignedAtom}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleSignedAtom(MolangParser.SingleSignedAtomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparisonOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(MolangParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignmentOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(MolangParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mulOrDiv}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulOrDiv(MolangParser.MulOrDivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addOrSub}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddOrSub(MolangParser.AddOrSubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code neExpr}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNeExpr(MolangParser.NeExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comment}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment(MolangParser.CommentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnOperator(MolangParser.ReturnOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code logicOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicOperator(MolangParser.LogicOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code binaryConditionalOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryConditionalOperator(MolangParser.BinaryConditionalOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code equalsOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualsOperator(MolangParser.EqualsOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code function}
	 * labeled alternative in {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(MolangParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code variable}
	 * labeled alternative in {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(MolangParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code number}
	 * labeled alternative in {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(MolangParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesesPrecedence}
	 * labeled alternative in {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesesPrecedence(MolangParser.ParenthesesPrecedenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#funcParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncParam(MolangParser.FuncParamContext ctx);
}