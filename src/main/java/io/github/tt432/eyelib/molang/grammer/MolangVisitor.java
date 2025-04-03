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
	 * Visit a parse tree produced by the {@code oneExpr}
	 * labeled alternative in {@link MolangParser#exprSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOneExpr(MolangParser.OneExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code base}
	 * labeled alternative in {@link MolangParser#exprSet}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBase(MolangParser.BaseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code signedAtom}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignedAtom(MolangParser.SignedAtomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code objectRef}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectRef(MolangParser.ObjectRefContext ctx);
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
	 * Visit a parse tree produced by the {@code stringValue}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringValue(MolangParser.StringValueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code loop}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop(MolangParser.LoopContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mulOrDiv}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulOrDiv(MolangParser.MulOrDivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullCoalescing}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullCoalescing(MolangParser.NullCoalescingContext ctx);
	/**
	 * Visit a parse tree produced by the {@code scopedExprSet}
	 * labeled alternative in {@link MolangParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScopedExprSet(MolangParser.ScopedExprSetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code value}
	 * labeled alternative in {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(MolangParser.ValueContext ctx);
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
	 * Visit a parse tree produced by the {@code this}
	 * labeled alternative in {@link MolangParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThis(MolangParser.ThisContext ctx);
	/**
	 * Visit a parse tree produced by the {@code function}
	 * labeled alternative in {@link MolangParser#values}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(MolangParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code variable}
	 * labeled alternative in {@link MolangParser#values}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(MolangParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code accessArray}
	 * labeled alternative in {@link MolangParser#values}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAccessArray(MolangParser.AccessArrayContext ctx);
}