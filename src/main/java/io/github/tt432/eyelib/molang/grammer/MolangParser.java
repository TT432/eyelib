// Generated from Molang.g4 by ANTLR 4.13.1
 package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class MolangParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, Assignment_Operator=2, COMPARISON_OPERATOR=3, EQUALS_OPERATOR=4, 
		AND_OPERATOR=5, OR_OPERATOR=6, BCO=7, TCO0=8, S_OPERATOR=9, RETURN=10, 
		CONSTANT=11, SCIENTIFIC_NUMBER=12, STRING=13, LPAREN=14, RPAREN=15, DQUOT=16, 
		QUOT=17, E1=18, E2=19, ID=20, WS=21, ADD=22, SUB=23, MUL=24, DIV=25, COMMA=26, 
		SIEM=27;
	public static final int
		RULE_exprSet = 0, RULE_expr = 1, RULE_funcParam = 2, RULE_signedAtom = 3, 
		RULE_atom = 4, RULE_scientific = 5, RULE_function = 6, RULE_variable = 7, 
		RULE_funcname = 8, RULE_string = 9, RULE_assignment = 10;
	private static String[] makeRuleNames() {
		return new String[] {
			"exprSet", "expr", "funcParam", "signedAtom", "atom", "scientific", "function", 
			"variable", "funcname", "string", "assignment"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'.'", "'='", null, null, "'&&'", "'||'", "'?'", "':'", "'!'", 
			"'return'", null, null, null, "'('", "')'", "'\"'", "'''", null, "'e'", 
			null, null, "'+'", "'-'", "'*'", "'/'", "','", "';'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, "Assignment_Operator", "COMPARISON_OPERATOR", "EQUALS_OPERATOR", 
			"AND_OPERATOR", "OR_OPERATOR", "BCO", "TCO0", "S_OPERATOR", "RETURN", 
			"CONSTANT", "SCIENTIFIC_NUMBER", "STRING", "LPAREN", "RPAREN", "DQUOT", 
			"QUOT", "E1", "E2", "ID", "WS", "ADD", "SUB", "MUL", "DIV", "COMMA", 
			"SIEM"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Molang.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public MolangParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprSetContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> SIEM() { return getTokens(MolangParser.SIEM); }
		public TerminalNode SIEM(int i) {
			return getToken(MolangParser.SIEM, i);
		}
		public ExprSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprSet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterExprSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitExprSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitExprSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprSetContext exprSet() throws RecognitionException {
		ExprSetContext _localctx = new ExprSetContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_exprSet);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(22);
			expr(0);
			setState(27);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(23);
					match(SIEM);
					setState(24);
					expr(0);
					}
					} 
				}
				setState(29);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(31);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SIEM) {
				{
				setState(30);
				match(SIEM);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TernaryConditionalOperatorContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode BCO() { return getToken(MolangParser.BCO, 0); }
		public TerminalNode TCO0() { return getToken(MolangParser.TCO0, 0); }
		public TernaryConditionalOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterTernaryConditionalOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitTernaryConditionalOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitTernaryConditionalOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OrOperatorContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode OR_OPERATOR() { return getToken(MolangParser.OR_OPERATOR, 0); }
		public OrOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterOrOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitOrOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitOrOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class SingleSignedAtomContext extends ExprContext {
		public SignedAtomContext signedAtom() {
			return getRuleContext(SignedAtomContext.class,0);
		}
		public SingleSignedAtomContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterSingleSignedAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitSingleSignedAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitSingleSignedAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonOperatorContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode COMPARISON_OPERATOR() { return getToken(MolangParser.COMPARISON_OPERATOR, 0); }
		public ComparisonOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterComparisonOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitComparisonOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitComparisonOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentOperatorContext extends ExprContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public AssignmentContext assignment() {
			return getRuleContext(AssignmentContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AssignmentOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterAssignmentOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitAssignmentOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAssignmentOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MulOrDivContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode MUL() { return getToken(MolangParser.MUL, 0); }
		public TerminalNode DIV() { return getToken(MolangParser.DIV, 0); }
		public MulOrDivContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterMulOrDiv(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitMulOrDiv(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitMulOrDiv(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AddOrSubContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode ADD() { return getToken(MolangParser.ADD, 0); }
		public TerminalNode SUB() { return getToken(MolangParser.SUB, 0); }
		public AddOrSubContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterAddOrSub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitAddOrSub(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAddOrSub(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NeExprContext extends ExprContext {
		public TerminalNode S_OPERATOR() { return getToken(MolangParser.S_OPERATOR, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public NeExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterNeExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitNeExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitNeExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AndOperatorContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode AND_OPERATOR() { return getToken(MolangParser.AND_OPERATOR, 0); }
		public AndOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterAndOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitAndOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAndOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ReturnOperatorContext extends ExprContext {
		public TerminalNode RETURN() { return getToken(MolangParser.RETURN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ReturnOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterReturnOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitReturnOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitReturnOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BinaryConditionalOperatorContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode BCO() { return getToken(MolangParser.BCO, 0); }
		public BinaryConditionalOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterBinaryConditionalOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitBinaryConditionalOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitBinaryConditionalOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class EqualsOperatorContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode EQUALS_OPERATOR() { return getToken(MolangParser.EQUALS_OPERATOR, 0); }
		public EqualsOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterEqualsOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitEqualsOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitEqualsOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				{
				_localctx = new NeExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(34);
				match(S_OPERATOR);
				setState(35);
				expr(12);
				}
				break;
			case 2:
				{
				_localctx = new AssignmentOperatorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(36);
				variable();
				setState(37);
				assignment();
				setState(38);
				expr(3);
				}
				break;
			case 3:
				{
				_localctx = new SingleSignedAtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(40);
				signedAtom();
				}
				break;
			case 4:
				{
				_localctx = new ReturnOperatorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(41);
				match(RETURN);
				setState(42);
				expr(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(74);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(72);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
					case 1:
						{
						_localctx = new MulOrDivContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(45);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(46);
						((MulOrDivContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==MUL || _la==DIV) ) {
							((MulOrDivContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(47);
						expr(12);
						}
						break;
					case 2:
						{
						_localctx = new AddOrSubContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(48);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(49);
						((AddOrSubContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
							((AddOrSubContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(50);
						expr(11);
						}
						break;
					case 3:
						{
						_localctx = new ComparisonOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(51);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(52);
						match(COMPARISON_OPERATOR);
						setState(53);
						expr(10);
						}
						break;
					case 4:
						{
						_localctx = new EqualsOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(54);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(55);
						match(EQUALS_OPERATOR);
						setState(56);
						expr(9);
						}
						break;
					case 5:
						{
						_localctx = new AndOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(57);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(58);
						match(AND_OPERATOR);
						setState(59);
						expr(8);
						}
						break;
					case 6:
						{
						_localctx = new OrOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(60);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(61);
						match(OR_OPERATOR);
						setState(62);
						expr(7);
						}
						break;
					case 7:
						{
						_localctx = new BinaryConditionalOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(63);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(64);
						match(BCO);
						setState(65);
						expr(6);
						}
						break;
					case 8:
						{
						_localctx = new TernaryConditionalOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(66);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(67);
						match(BCO);
						setState(68);
						expr(0);
						setState(69);
						match(TCO0);
						setState(70);
						expr(5);
						}
						break;
					}
					} 
				}
				setState(76);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FuncParamContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public FuncParamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funcParam; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterFuncParam(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitFuncParam(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitFuncParam(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FuncParamContext funcParam() throws RecognitionException {
		FuncParamContext _localctx = new FuncParamContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_funcParam);
		try {
			setState(79);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case S_OPERATOR:
			case RETURN:
			case CONSTANT:
			case SCIENTIFIC_NUMBER:
			case LPAREN:
			case ID:
			case ADD:
			case SUB:
				enterOuterAlt(_localctx, 1);
				{
				setState(77);
				expr(0);
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(78);
				string();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SignedAtomContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(MolangParser.ADD, 0); }
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public TerminalNode SUB() { return getToken(MolangParser.SUB, 0); }
		public SignedAtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_signedAtom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterSignedAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitSignedAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitSignedAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SignedAtomContext signedAtom() throws RecognitionException {
		SignedAtomContext _localctx = new SignedAtomContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_signedAtom);
		try {
			setState(86);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADD:
				enterOuterAlt(_localctx, 1);
				{
				setState(81);
				match(ADD);
				setState(82);
				atom();
				}
				break;
			case SUB:
				enterOuterAlt(_localctx, 2);
				{
				setState(83);
				match(SUB);
				setState(84);
				atom();
				}
				break;
			case CONSTANT:
			case SCIENTIFIC_NUMBER:
			case LPAREN:
			case ID:
				enterOuterAlt(_localctx, 3);
				{
				setState(85);
				atom();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AtomContext extends ParserRuleContext {
		public FunctionContext function() {
			return getRuleContext(FunctionContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TerminalNode CONSTANT() { return getToken(MolangParser.CONSTANT, 0); }
		public ScientificContext scientific() {
			return getRuleContext(ScientificContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(MolangParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(MolangParser.RPAREN, 0); }
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_atom);
		try {
			setState(96);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(88);
				function();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(89);
				variable();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(90);
				match(CONSTANT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(91);
				scientific();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(92);
				match(LPAREN);
				setState(93);
				expr(0);
				setState(94);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ScientificContext extends ParserRuleContext {
		public TerminalNode SCIENTIFIC_NUMBER() { return getToken(MolangParser.SCIENTIFIC_NUMBER, 0); }
		public ScientificContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scientific; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterScientific(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitScientific(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitScientific(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScientificContext scientific() throws RecognitionException {
		ScientificContext _localctx = new ScientificContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_scientific);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(98);
			match(SCIENTIFIC_NUMBER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionContext extends ParserRuleContext {
		public FuncnameContext funcname() {
			return getRuleContext(FuncnameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(MolangParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(MolangParser.RPAREN, 0); }
		public List<FuncParamContext> funcParam() {
			return getRuleContexts(FuncParamContext.class);
		}
		public FuncParamContext funcParam(int i) {
			return getRuleContext(FuncParamContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(MolangParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(MolangParser.COMMA, i);
		}
		public FunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionContext function() throws RecognitionException {
		FunctionContext _localctx = new FunctionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_function);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			funcname();
			setState(101);
			match(LPAREN);
			setState(110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 13663744L) != 0)) {
				{
				setState(102);
				funcParam();
				setState(107);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(103);
					match(COMMA);
					setState(104);
					funcParam();
					}
					}
					setState(109);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(112);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(MolangParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(MolangParser.ID, i);
		}
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_variable);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
			match(ID);
			setState(119);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(115);
					match(T__0);
					setState(116);
					match(ID);
					}
					} 
				}
				setState(121);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FuncnameContext extends ParserRuleContext {
		public List<TerminalNode> ID() { return getTokens(MolangParser.ID); }
		public TerminalNode ID(int i) {
			return getToken(MolangParser.ID, i);
		}
		public FuncnameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funcname; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterFuncname(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitFuncname(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitFuncname(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FuncnameContext funcname() throws RecognitionException {
		FuncnameContext _localctx = new FuncnameContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_funcname);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(ID);
			setState(127);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(123);
				match(T__0);
				setState(124);
				match(ID);
				}
				}
				setState(129);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(MolangParser.STRING, 0); }
		public StringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_string);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(130);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentContext extends ParserRuleContext {
		public TerminalNode Assignment_Operator() { return getToken(MolangParser.Assignment_Operator, 0); }
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MolangListener ) ((MolangListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_assignment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(132);
			match(Assignment_Operator);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 11);
		case 1:
			return precpred(_ctx, 10);
		case 2:
			return precpred(_ctx, 9);
		case 3:
			return precpred(_ctx, 8);
		case 4:
			return precpred(_ctx, 7);
		case 5:
			return precpred(_ctx, 6);
		case 6:
			return precpred(_ctx, 5);
		case 7:
			return precpred(_ctx, 4);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u001b\u0087\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0005\u0000\u001a\b\u0000\n\u0000\f\u0000\u001d\t\u0000\u0001"+
		"\u0000\u0003\u0000 \b\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0003\u0001,\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005"+
		"\u0001I\b\u0001\n\u0001\f\u0001L\t\u0001\u0001\u0002\u0001\u0002\u0003"+
		"\u0002P\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0003\u0003W\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004a\b"+
		"\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0005\u0006j\b\u0006\n\u0006\f\u0006m\t\u0006\u0003"+
		"\u0006o\b\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0005\u0007v\b\u0007\n\u0007\f\u0007y\t\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0005\b~\b\b\n\b\f\b\u0081\t\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001"+
		"\n\u0000\u0001\u0002\u000b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012"+
		"\u0014\u0000\u0002\u0001\u0000\u0018\u0019\u0001\u0000\u0016\u0017\u0093"+
		"\u0000\u0016\u0001\u0000\u0000\u0000\u0002+\u0001\u0000\u0000\u0000\u0004"+
		"O\u0001\u0000\u0000\u0000\u0006V\u0001\u0000\u0000\u0000\b`\u0001\u0000"+
		"\u0000\u0000\nb\u0001\u0000\u0000\u0000\fd\u0001\u0000\u0000\u0000\u000e"+
		"r\u0001\u0000\u0000\u0000\u0010z\u0001\u0000\u0000\u0000\u0012\u0082\u0001"+
		"\u0000\u0000\u0000\u0014\u0084\u0001\u0000\u0000\u0000\u0016\u001b\u0003"+
		"\u0002\u0001\u0000\u0017\u0018\u0005\u001b\u0000\u0000\u0018\u001a\u0003"+
		"\u0002\u0001\u0000\u0019\u0017\u0001\u0000\u0000\u0000\u001a\u001d\u0001"+
		"\u0000\u0000\u0000\u001b\u0019\u0001\u0000\u0000\u0000\u001b\u001c\u0001"+
		"\u0000\u0000\u0000\u001c\u001f\u0001\u0000\u0000\u0000\u001d\u001b\u0001"+
		"\u0000\u0000\u0000\u001e \u0005\u001b\u0000\u0000\u001f\u001e\u0001\u0000"+
		"\u0000\u0000\u001f \u0001\u0000\u0000\u0000 \u0001\u0001\u0000\u0000\u0000"+
		"!\"\u0006\u0001\uffff\uffff\u0000\"#\u0005\t\u0000\u0000#,\u0003\u0002"+
		"\u0001\f$%\u0003\u000e\u0007\u0000%&\u0003\u0014\n\u0000&\'\u0003\u0002"+
		"\u0001\u0003\',\u0001\u0000\u0000\u0000(,\u0003\u0006\u0003\u0000)*\u0005"+
		"\n\u0000\u0000*,\u0003\u0002\u0001\u0001+!\u0001\u0000\u0000\u0000+$\u0001"+
		"\u0000\u0000\u0000+(\u0001\u0000\u0000\u0000+)\u0001\u0000\u0000\u0000"+
		",J\u0001\u0000\u0000\u0000-.\n\u000b\u0000\u0000./\u0007\u0000\u0000\u0000"+
		"/I\u0003\u0002\u0001\f01\n\n\u0000\u000012\u0007\u0001\u0000\u00002I\u0003"+
		"\u0002\u0001\u000b34\n\t\u0000\u000045\u0005\u0003\u0000\u00005I\u0003"+
		"\u0002\u0001\n67\n\b\u0000\u000078\u0005\u0004\u0000\u00008I\u0003\u0002"+
		"\u0001\t9:\n\u0007\u0000\u0000:;\u0005\u0005\u0000\u0000;I\u0003\u0002"+
		"\u0001\b<=\n\u0006\u0000\u0000=>\u0005\u0006\u0000\u0000>I\u0003\u0002"+
		"\u0001\u0007?@\n\u0005\u0000\u0000@A\u0005\u0007\u0000\u0000AI\u0003\u0002"+
		"\u0001\u0006BC\n\u0004\u0000\u0000CD\u0005\u0007\u0000\u0000DE\u0003\u0002"+
		"\u0001\u0000EF\u0005\b\u0000\u0000FG\u0003\u0002\u0001\u0005GI\u0001\u0000"+
		"\u0000\u0000H-\u0001\u0000\u0000\u0000H0\u0001\u0000\u0000\u0000H3\u0001"+
		"\u0000\u0000\u0000H6\u0001\u0000\u0000\u0000H9\u0001\u0000\u0000\u0000"+
		"H<\u0001\u0000\u0000\u0000H?\u0001\u0000\u0000\u0000HB\u0001\u0000\u0000"+
		"\u0000IL\u0001\u0000\u0000\u0000JH\u0001\u0000\u0000\u0000JK\u0001\u0000"+
		"\u0000\u0000K\u0003\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000"+
		"MP\u0003\u0002\u0001\u0000NP\u0003\u0012\t\u0000OM\u0001\u0000\u0000\u0000"+
		"ON\u0001\u0000\u0000\u0000P\u0005\u0001\u0000\u0000\u0000QR\u0005\u0016"+
		"\u0000\u0000RW\u0003\b\u0004\u0000ST\u0005\u0017\u0000\u0000TW\u0003\b"+
		"\u0004\u0000UW\u0003\b\u0004\u0000VQ\u0001\u0000\u0000\u0000VS\u0001\u0000"+
		"\u0000\u0000VU\u0001\u0000\u0000\u0000W\u0007\u0001\u0000\u0000\u0000"+
		"Xa\u0003\f\u0006\u0000Ya\u0003\u000e\u0007\u0000Za\u0005\u000b\u0000\u0000"+
		"[a\u0003\n\u0005\u0000\\]\u0005\u000e\u0000\u0000]^\u0003\u0002\u0001"+
		"\u0000^_\u0005\u000f\u0000\u0000_a\u0001\u0000\u0000\u0000`X\u0001\u0000"+
		"\u0000\u0000`Y\u0001\u0000\u0000\u0000`Z\u0001\u0000\u0000\u0000`[\u0001"+
		"\u0000\u0000\u0000`\\\u0001\u0000\u0000\u0000a\t\u0001\u0000\u0000\u0000"+
		"bc\u0005\f\u0000\u0000c\u000b\u0001\u0000\u0000\u0000de\u0003\u0010\b"+
		"\u0000en\u0005\u000e\u0000\u0000fk\u0003\u0004\u0002\u0000gh\u0005\u001a"+
		"\u0000\u0000hj\u0003\u0004\u0002\u0000ig\u0001\u0000\u0000\u0000jm\u0001"+
		"\u0000\u0000\u0000ki\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000"+
		"lo\u0001\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000nf\u0001\u0000\u0000"+
		"\u0000no\u0001\u0000\u0000\u0000op\u0001\u0000\u0000\u0000pq\u0005\u000f"+
		"\u0000\u0000q\r\u0001\u0000\u0000\u0000rw\u0005\u0014\u0000\u0000st\u0005"+
		"\u0001\u0000\u0000tv\u0005\u0014\u0000\u0000us\u0001\u0000\u0000\u0000"+
		"vy\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000\u0000wx\u0001\u0000\u0000"+
		"\u0000x\u000f\u0001\u0000\u0000\u0000yw\u0001\u0000\u0000\u0000z\u007f"+
		"\u0005\u0014\u0000\u0000{|\u0005\u0001\u0000\u0000|~\u0005\u0014\u0000"+
		"\u0000}{\u0001\u0000\u0000\u0000~\u0081\u0001\u0000\u0000\u0000\u007f"+
		"}\u0001\u0000\u0000\u0000\u007f\u0080\u0001\u0000\u0000\u0000\u0080\u0011"+
		"\u0001\u0000\u0000\u0000\u0081\u007f\u0001\u0000\u0000\u0000\u0082\u0083"+
		"\u0005\r\u0000\u0000\u0083\u0013\u0001\u0000\u0000\u0000\u0084\u0085\u0005"+
		"\u0002\u0000\u0000\u0085\u0015\u0001\u0000\u0000\u0000\f\u001b\u001f+"+
		"HJOV`knw\u007f";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}