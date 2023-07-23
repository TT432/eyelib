// Generated from C:/Users/q2437/Desktop/idea项目/antlr-molang\Molang.g4 by ANTLR 4.12.0
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
	 * Visit a parse tree produced by the {@code orOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrOperator(MolangParser.OrOperatorContext ctx);
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
	 * Visit a parse tree produced by the {@code andOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndOperator(MolangParser.AndOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code returnOperator}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnOperator(MolangParser.ReturnOperatorContext ctx);
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
	 * Visit a parse tree produced by {@link MolangParser#funcParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncParam(MolangParser.FuncParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#signedAtom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignedAtom(MolangParser.SignedAtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom(MolangParser.AtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#scientific}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScientific(MolangParser.ScientificContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(MolangParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(MolangParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#funcname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncname(MolangParser.FuncnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(MolangParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by {@link MolangParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(MolangParser.AssignmentContext ctx);
}