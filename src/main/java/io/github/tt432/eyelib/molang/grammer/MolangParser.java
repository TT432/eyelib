// Generated from Molang.g4 by ANTLR 4.13.1
 package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, RETURN=29, STRING=30, ID=31, SCIENTIFIC_NUMBER=32, 
		WS=33;
	public static final int
		RULE_exprSet = 0, RULE_expr = 1, RULE_atom = 2, RULE_values = 3;
	private static String[] makeRuleNames() {
		return new String[] {
			"exprSet", "expr", "atom", "values"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'loop('", "','", "'{'", "'}'", "')'", "'!'", "'*'", "'/'", 
			"'+'", "'-'", "'<'", "'<='", "'>='", "'>'", "'=='", "'!='", "'&&'", "'||'", 
			"'?'", "':'", "'='", "'->'", "'??'", "'('", "'this'", "'['", "']'", "'return'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, "RETURN", "STRING", "ID", "SCIENTIFIC_NUMBER", 
			"WS"
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
		public ExprSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprSet; }
	 
		public ExprSetContext() { }
		public void copyFrom(ExprSetContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OneExprContext extends ExprSetContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public OneExprContext(ExprSetContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitOneExpr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class BaseContext extends ExprSetContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode RETURN() { return getToken(MolangParser.RETURN, 0); }
		public BaseContext(ExprSetContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitBase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprSetContext exprSet() throws RecognitionException {
		ExprSetContext _localctx = new ExprSetContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_exprSet);
		int _la;
		try {
			int _alt;
			setState(24);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				_localctx = new OneExprContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(8);
				expr(0);
				}
				break;
			case 2:
				_localctx = new BaseContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(14);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
				while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(9);
						expr(0);
						setState(10);
						match(T__0);
						}
						} 
					}
					setState(16);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
				}
				setState(18);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==RETURN) {
					{
					setState(17);
					match(RETURN);
					}
				}

				setState(20);
				expr(0);
				setState(22);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__0) {
					{
					setState(21);
					match(T__0);
					}
				}

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
	public static class SignedAtomContext extends ExprContext {
		public Token op;
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public SignedAtomContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitSignedAtom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ObjectRefContext extends ExprContext {
		public ValuesContext values() {
			return getRuleContext(ValuesContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ObjectRefContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitObjectRef(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonOperatorContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public ComparisonOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitComparisonOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AssignmentOperatorContext extends ExprContext {
		public TerminalNode ID() { return getToken(MolangParser.ID, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AssignmentOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAssignmentOperator(this);
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
		public AddOrSubContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAddOrSub(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NeExprContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public NeExprContext(ExprContext ctx) { copyFrom(ctx); }
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
		public AndOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAndOperator(this);
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
		public BinaryConditionalOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitBinaryConditionalOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class EqualsOperatorContext extends ExprContext {
		public Token op;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public EqualsOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitEqualsOperator(this);
			else return visitor.visitChildren(this);
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
		public TernaryConditionalOperatorContext(ExprContext ctx) { copyFrom(ctx); }
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
		public OrOperatorContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitOrOperator(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class StringValueContext extends ExprContext {
		public TerminalNode STRING() { return getToken(MolangParser.STRING, 0); }
		public StringValueContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitStringValue(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LoopContext extends ExprContext {
		public TerminalNode SCIENTIFIC_NUMBER() { return getToken(MolangParser.SCIENTIFIC_NUMBER, 0); }
		public ExprSetContext exprSet() {
			return getRuleContext(ExprSetContext.class,0);
		}
		public LoopContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitLoop(this);
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
		public MulOrDivContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitMulOrDiv(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NullCoalescingContext extends ExprContext {
		public ValuesContext values() {
			return getRuleContext(ValuesContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public NullCoalescingContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitNullCoalescing(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ScopedExprSetContext extends ExprContext {
		public ExprSetContext exprSet() {
			return getRuleContext(ExprSetContext.class,0);
		}
		public ScopedExprSetContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitScopedExprSet(this);
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
			setState(57);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				_localctx = new LoopContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(27);
				match(T__1);
				setState(28);
				match(SCIENTIFIC_NUMBER);
				setState(29);
				match(T__2);
				setState(30);
				match(T__3);
				setState(31);
				exprSet();
				setState(32);
				match(T__4);
				setState(33);
				match(T__5);
				}
				break;
			case 2:
				{
				_localctx = new ScopedExprSetContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(35);
				match(T__3);
				setState(36);
				exprSet();
				setState(37);
				match(T__4);
				}
				break;
			case 3:
				{
				_localctx = new NeExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(39);
				match(T__6);
				setState(40);
				expr(14);
				}
				break;
			case 4:
				{
				_localctx = new AssignmentOperatorContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(41);
				match(ID);
				setState(42);
				match(T__21);
				setState(43);
				expr(5);
				}
				break;
			case 5:
				{
				_localctx = new SignedAtomContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(45);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__9 || _la==T__10) {
					{
					setState(44);
					((SignedAtomContext)_localctx).op = _input.LT(1);
					_la = _input.LA(1);
					if ( !(_la==T__9 || _la==T__10) ) {
						((SignedAtomContext)_localctx).op = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(47);
				atom();
				}
				break;
			case 6:
				{
				_localctx = new StringValueContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(48);
				match(STRING);
				}
				break;
			case 7:
				{
				_localctx = new ObjectRefContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(49);
				values(0);
				setState(50);
				match(T__22);
				setState(51);
				expr(2);
				}
				break;
			case 8:
				{
				_localctx = new NullCoalescingContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(53);
				values(0);
				setState(54);
				match(T__23);
				setState(55);
				expr(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(88);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(86);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
					case 1:
						{
						_localctx = new MulOrDivContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(59);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(60);
						((MulOrDivContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__7 || _la==T__8) ) {
							((MulOrDivContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(61);
						expr(14);
						}
						break;
					case 2:
						{
						_localctx = new AddOrSubContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(62);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(63);
						((AddOrSubContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__9 || _la==T__10) ) {
							((AddOrSubContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(64);
						expr(13);
						}
						break;
					case 3:
						{
						_localctx = new ComparisonOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(65);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(66);
						((ComparisonOperatorContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 61440L) != 0)) ) {
							((ComparisonOperatorContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(67);
						expr(12);
						}
						break;
					case 4:
						{
						_localctx = new EqualsOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(68);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(69);
						((EqualsOperatorContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__15 || _la==T__16) ) {
							((EqualsOperatorContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(70);
						expr(11);
						}
						break;
					case 5:
						{
						_localctx = new AndOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(71);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(72);
						match(T__17);
						setState(73);
						expr(10);
						}
						break;
					case 6:
						{
						_localctx = new OrOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(74);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(75);
						match(T__18);
						setState(76);
						expr(9);
						}
						break;
					case 7:
						{
						_localctx = new BinaryConditionalOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(77);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(78);
						match(T__19);
						setState(79);
						expr(8);
						}
						break;
					case 8:
						{
						_localctx = new TernaryConditionalOperatorContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(80);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(81);
						match(T__19);
						setState(82);
						expr(0);
						setState(83);
						match(T__20);
						setState(84);
						expr(7);
						}
						break;
					}
					} 
				}
				setState(90);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
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
	public static class AtomContext extends ParserRuleContext {
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
	 
		public AtomContext() { }
		public void copyFrom(AtomContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NumberContext extends AtomContext {
		public TerminalNode SCIENTIFIC_NUMBER() { return getToken(MolangParser.SCIENTIFIC_NUMBER, 0); }
		public NumberContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitNumber(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ThisContext extends AtomContext {
		public ThisContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitThis(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ValueContext extends AtomContext {
		public ValuesContext values() {
			return getRuleContext(ValuesContext.class,0);
		}
		public ValueContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesesPrecedenceContext extends AtomContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ParenthesesPrecedenceContext(AtomContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitParenthesesPrecedence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_atom);
		try {
			setState(98);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				_localctx = new ValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(91);
				values(0);
				}
				break;
			case SCIENTIFIC_NUMBER:
				_localctx = new NumberContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(92);
				match(SCIENTIFIC_NUMBER);
				}
				break;
			case T__24:
				_localctx = new ParenthesesPrecedenceContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(93);
				match(T__24);
				setState(94);
				expr(0);
				setState(95);
				match(T__5);
				}
				break;
			case T__25:
				_localctx = new ThisContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(97);
				match(T__25);
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
	public static class ValuesContext extends ParserRuleContext {
		public ValuesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_values; }
	 
		public ValuesContext() { }
		public void copyFrom(ValuesContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionContext extends ValuesContext {
		public TerminalNode ID() { return getToken(MolangParser.ID, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public FunctionContext(ValuesContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class VariableContext extends ValuesContext {
		public TerminalNode ID() { return getToken(MolangParser.ID, 0); }
		public VariableContext(ValuesContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AccessArrayContext extends ValuesContext {
		public ValuesContext values() {
			return getRuleContext(ValuesContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AccessArrayContext(ValuesContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MolangVisitor ) return ((MolangVisitor<? extends T>)visitor).visitAccessArray(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValuesContext values() throws RecognitionException {
		return values(0);
	}

	private ValuesContext values(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ValuesContext _localctx = new ValuesContext(_ctx, _parentState);
		ValuesContext _prevctx = _localctx;
		int _startState = 6;
		enterRecursionRule(_localctx, 6, RULE_values, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(115);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				_localctx = new FunctionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(101);
				match(ID);
				setState(102);
				match(T__24);
				setState(111);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 7616859284L) != 0)) {
					{
					setState(103);
					expr(0);
					setState(108);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__2) {
						{
						{
						setState(104);
						match(T__2);
						setState(105);
						expr(0);
						}
						}
						setState(110);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(113);
				match(T__5);
				}
				break;
			case 2:
				{
				_localctx = new VariableContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(114);
				match(ID);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(124);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new AccessArrayContext(new ValuesContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_values);
					setState(117);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(118);
					match(T__26);
					setState(119);
					expr(0);
					setState(120);
					match(T__27);
					}
					} 
				}
				setState(126);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 3:
			return values_sempred((ValuesContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 13);
		case 1:
			return precpred(_ctx, 12);
		case 2:
			return precpred(_ctx, 11);
		case 3:
			return precpred(_ctx, 10);
		case 4:
			return precpred(_ctx, 9);
		case 5:
			return precpred(_ctx, 8);
		case 6:
			return precpred(_ctx, 7);
		case 7:
			return precpred(_ctx, 6);
		}
		return true;
	}
	private boolean values_sempred(ValuesContext _localctx, int predIndex) {
		switch (predIndex) {
		case 8:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001!\u0080\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0005\u0000\r\b\u0000\n\u0000\f\u0000\u0010\t\u0000"+
		"\u0001\u0000\u0003\u0000\u0013\b\u0000\u0001\u0000\u0001\u0000\u0003\u0000"+
		"\u0017\b\u0000\u0003\u0000\u0019\b\u0000\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001.\b"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001:\b"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001W\b\u0001\n\u0001"+
		"\f\u0001Z\t\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0003\u0002c\b\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0005\u0003k\b"+
		"\u0003\n\u0003\f\u0003n\t\u0003\u0003\u0003p\b\u0003\u0001\u0003\u0001"+
		"\u0003\u0003\u0003t\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0005\u0003{\b\u0003\n\u0003\f\u0003~\t\u0003\u0001"+
		"\u0003\u0000\u0002\u0002\u0006\u0004\u0000\u0002\u0004\u0006\u0000\u0004"+
		"\u0001\u0000\n\u000b\u0001\u0000\b\t\u0001\u0000\f\u000f\u0001\u0000\u0010"+
		"\u0011\u0096\u0000\u0018\u0001\u0000\u0000\u0000\u00029\u0001\u0000\u0000"+
		"\u0000\u0004b\u0001\u0000\u0000\u0000\u0006s\u0001\u0000\u0000\u0000\b"+
		"\u0019\u0003\u0002\u0001\u0000\t\n\u0003\u0002\u0001\u0000\n\u000b\u0005"+
		"\u0001\u0000\u0000\u000b\r\u0001\u0000\u0000\u0000\f\t\u0001\u0000\u0000"+
		"\u0000\r\u0010\u0001\u0000\u0000\u0000\u000e\f\u0001\u0000\u0000\u0000"+
		"\u000e\u000f\u0001\u0000\u0000\u0000\u000f\u0012\u0001\u0000\u0000\u0000"+
		"\u0010\u000e\u0001\u0000\u0000\u0000\u0011\u0013\u0005\u001d\u0000\u0000"+
		"\u0012\u0011\u0001\u0000\u0000\u0000\u0012\u0013\u0001\u0000\u0000\u0000"+
		"\u0013\u0014\u0001\u0000\u0000\u0000\u0014\u0016\u0003\u0002\u0001\u0000"+
		"\u0015\u0017\u0005\u0001\u0000\u0000\u0016\u0015\u0001\u0000\u0000\u0000"+
		"\u0016\u0017\u0001\u0000\u0000\u0000\u0017\u0019\u0001\u0000\u0000\u0000"+
		"\u0018\b\u0001\u0000\u0000\u0000\u0018\u000e\u0001\u0000\u0000\u0000\u0019"+
		"\u0001\u0001\u0000\u0000\u0000\u001a\u001b\u0006\u0001\uffff\uffff\u0000"+
		"\u001b\u001c\u0005\u0002\u0000\u0000\u001c\u001d\u0005 \u0000\u0000\u001d"+
		"\u001e\u0005\u0003\u0000\u0000\u001e\u001f\u0005\u0004\u0000\u0000\u001f"+
		" \u0003\u0000\u0000\u0000 !\u0005\u0005\u0000\u0000!\"\u0005\u0006\u0000"+
		"\u0000\":\u0001\u0000\u0000\u0000#$\u0005\u0004\u0000\u0000$%\u0003\u0000"+
		"\u0000\u0000%&\u0005\u0005\u0000\u0000&:\u0001\u0000\u0000\u0000\'(\u0005"+
		"\u0007\u0000\u0000(:\u0003\u0002\u0001\u000e)*\u0005\u001f\u0000\u0000"+
		"*+\u0005\u0016\u0000\u0000+:\u0003\u0002\u0001\u0005,.\u0007\u0000\u0000"+
		"\u0000-,\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000./\u0001\u0000"+
		"\u0000\u0000/:\u0003\u0004\u0002\u00000:\u0005\u001e\u0000\u000012\u0003"+
		"\u0006\u0003\u000023\u0005\u0017\u0000\u000034\u0003\u0002\u0001\u0002"+
		"4:\u0001\u0000\u0000\u000056\u0003\u0006\u0003\u000067\u0005\u0018\u0000"+
		"\u000078\u0003\u0002\u0001\u00018:\u0001\u0000\u0000\u00009\u001a\u0001"+
		"\u0000\u0000\u00009#\u0001\u0000\u0000\u00009\'\u0001\u0000\u0000\u0000"+
		"9)\u0001\u0000\u0000\u00009-\u0001\u0000\u0000\u000090\u0001\u0000\u0000"+
		"\u000091\u0001\u0000\u0000\u000095\u0001\u0000\u0000\u0000:X\u0001\u0000"+
		"\u0000\u0000;<\n\r\u0000\u0000<=\u0007\u0001\u0000\u0000=W\u0003\u0002"+
		"\u0001\u000e>?\n\f\u0000\u0000?@\u0007\u0000\u0000\u0000@W\u0003\u0002"+
		"\u0001\rAB\n\u000b\u0000\u0000BC\u0007\u0002\u0000\u0000CW\u0003\u0002"+
		"\u0001\fDE\n\n\u0000\u0000EF\u0007\u0003\u0000\u0000FW\u0003\u0002\u0001"+
		"\u000bGH\n\t\u0000\u0000HI\u0005\u0012\u0000\u0000IW\u0003\u0002\u0001"+
		"\nJK\n\b\u0000\u0000KL\u0005\u0013\u0000\u0000LW\u0003\u0002\u0001\tM"+
		"N\n\u0007\u0000\u0000NO\u0005\u0014\u0000\u0000OW\u0003\u0002\u0001\b"+
		"PQ\n\u0006\u0000\u0000QR\u0005\u0014\u0000\u0000RS\u0003\u0002\u0001\u0000"+
		"ST\u0005\u0015\u0000\u0000TU\u0003\u0002\u0001\u0007UW\u0001\u0000\u0000"+
		"\u0000V;\u0001\u0000\u0000\u0000V>\u0001\u0000\u0000\u0000VA\u0001\u0000"+
		"\u0000\u0000VD\u0001\u0000\u0000\u0000VG\u0001\u0000\u0000\u0000VJ\u0001"+
		"\u0000\u0000\u0000VM\u0001\u0000\u0000\u0000VP\u0001\u0000\u0000\u0000"+
		"WZ\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000"+
		"\u0000Y\u0003\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000[c\u0003"+
		"\u0006\u0003\u0000\\c\u0005 \u0000\u0000]^\u0005\u0019\u0000\u0000^_\u0003"+
		"\u0002\u0001\u0000_`\u0005\u0006\u0000\u0000`c\u0001\u0000\u0000\u0000"+
		"ac\u0005\u001a\u0000\u0000b[\u0001\u0000\u0000\u0000b\\\u0001\u0000\u0000"+
		"\u0000b]\u0001\u0000\u0000\u0000ba\u0001\u0000\u0000\u0000c\u0005\u0001"+
		"\u0000\u0000\u0000de\u0006\u0003\uffff\uffff\u0000ef\u0005\u001f\u0000"+
		"\u0000fo\u0005\u0019\u0000\u0000gl\u0003\u0002\u0001\u0000hi\u0005\u0003"+
		"\u0000\u0000ik\u0003\u0002\u0001\u0000jh\u0001\u0000\u0000\u0000kn\u0001"+
		"\u0000\u0000\u0000lj\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000"+
		"mp\u0001\u0000\u0000\u0000nl\u0001\u0000\u0000\u0000og\u0001\u0000\u0000"+
		"\u0000op\u0001\u0000\u0000\u0000pq\u0001\u0000\u0000\u0000qt\u0005\u0006"+
		"\u0000\u0000rt\u0005\u001f\u0000\u0000sd\u0001\u0000\u0000\u0000sr\u0001"+
		"\u0000\u0000\u0000t|\u0001\u0000\u0000\u0000uv\n\u0001\u0000\u0000vw\u0005"+
		"\u001b\u0000\u0000wx\u0003\u0002\u0001\u0000xy\u0005\u001c\u0000\u0000"+
		"y{\u0001\u0000\u0000\u0000zu\u0001\u0000\u0000\u0000{~\u0001\u0000\u0000"+
		"\u0000|z\u0001\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}\u0007\u0001"+
		"\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000\r\u000e\u0012\u0016\u0018"+
		"-9VXblos|";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}