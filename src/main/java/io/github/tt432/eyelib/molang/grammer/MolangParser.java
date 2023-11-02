// Generated from .\Molang.g4 by ANTLR 4.9.1
package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MolangParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            T__0 = 1, Assignment_Operator = 2, COMPARISON_OPERATOR = 3, EQUALS_OPERATOR = 4,
            AND_OPERATOR = 5, OR_OPERATOR = 6, BCO = 7, TCO0 = 8, S_OPERATOR = 9, RETURN = 10,
            CONSTANT = 11, SCIENTIFIC_NUMBER = 12, STRING = 13, LPAREN = 14, RPAREN = 15, DQUOT = 16,
            QUOT = 17, E1 = 18, E2 = 19, ID = 20, WS = 21, ADD = 22, SUB = 23, MUL = 24, DIV = 25, COMMA = 26,
            SIEM = 27;
    public static final int
            RULE_exprSet = 0, RULE_expr = 1, RULE_funcParam = 2, RULE_signedAtom = 3,
            RULE_atom = 4, RULE_scientific = 5, RULE_function = 6, RULE_variable = 7,
            RULE_funcname = 8, RULE_string = 9, RULE_assignment = 10;

    private static String[] makeRuleNames() {
        return new String[]{
                "exprSet", "expr", "funcParam", "signedAtom", "atom", "scientific", "function",
                "variable", "funcname", "string", "assignment"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'.'", "'='", null, null, "'&&'", "'||'", "'?'", "':'", "'!'",
                "'return'", null, null, null, "'('", "')'", "'\"'", "'''", null, "'e'",
                null, null, "'+'", "'-'", "'*'", "'/'", "','", "';'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
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
    public String getGrammarFileName() {
        return "Molang.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public MolangParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class ExprSetContext extends ParserRuleContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public List<TerminalNode> SIEM() {
            return getTokens(MolangParser.SIEM);
        }

        public TerminalNode SIEM(int i) {
            return getToken(MolangParser.SIEM, i);
        }

        public ExprSetContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exprSet;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterExprSet(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitExprSet(this);
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
                _alt = getInterpreter().adaptivePredict(_input, 0, _ctx);
                while (_alt != 2 && _alt != ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
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
                    _alt = getInterpreter().adaptivePredict(_input, 0, _ctx);
                }
                setState(31);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == SIEM) {
                    {
                        setState(30);
                        match(SIEM);
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class ExprContext extends ParserRuleContext {
        public ExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expr;
        }

        public ExprContext() {
        }

        public void copyFrom(ExprContext ctx) {
            super.copyFrom(ctx);
        }
    }

    public static class TernaryConditionalOperatorContext extends ExprContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode BCO() {
            return getToken(MolangParser.BCO, 0);
        }

        public TerminalNode TCO0() {
            return getToken(MolangParser.TCO0, 0);
        }

        public TernaryConditionalOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterTernaryConditionalOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitTernaryConditionalOperator(this);
        }
    }

    public static class OrOperatorContext extends ExprContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode OR_OPERATOR() {
            return getToken(MolangParser.OR_OPERATOR, 0);
        }

        public OrOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterOrOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitOrOperator(this);
        }
    }

    public static class SingleSignedAtomContext extends ExprContext {
        public SignedAtomContext signedAtom() {
            return getRuleContext(SignedAtomContext.class, 0);
        }

        public SingleSignedAtomContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterSingleSignedAtom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitSingleSignedAtom(this);
        }
    }

    public static class ComparisonOperatorContext extends ExprContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode COMPARISON_OPERATOR() {
            return getToken(MolangParser.COMPARISON_OPERATOR, 0);
        }

        public ComparisonOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterComparisonOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitComparisonOperator(this);
        }
    }

    public static class AssignmentOperatorContext extends ExprContext {
        public VariableContext variable() {
            return getRuleContext(VariableContext.class, 0);
        }

        public AssignmentContext assignment() {
            return getRuleContext(AssignmentContext.class, 0);
        }

        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public AssignmentOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterAssignmentOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitAssignmentOperator(this);
        }
    }

    public static class MulOrDivContext extends ExprContext {
        public Token op;

        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode MUL() {
            return getToken(MolangParser.MUL, 0);
        }

        public TerminalNode DIV() {
            return getToken(MolangParser.DIV, 0);
        }

        public MulOrDivContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterMulOrDiv(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitMulOrDiv(this);
        }
    }

    public static class AddOrSubContext extends ExprContext {
        public Token op;

        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode ADD() {
            return getToken(MolangParser.ADD, 0);
        }

        public TerminalNode SUB() {
            return getToken(MolangParser.SUB, 0);
        }

        public AddOrSubContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterAddOrSub(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitAddOrSub(this);
        }
    }

    public static class NeExprContext extends ExprContext {
        public TerminalNode S_OPERATOR() {
            return getToken(MolangParser.S_OPERATOR, 0);
        }

        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public NeExprContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterNeExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitNeExpr(this);
        }
    }

    public static class AndOperatorContext extends ExprContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode AND_OPERATOR() {
            return getToken(MolangParser.AND_OPERATOR, 0);
        }

        public AndOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterAndOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitAndOperator(this);
        }
    }

    public static class ReturnOperatorContext extends ExprContext {
        public TerminalNode RETURN() {
            return getToken(MolangParser.RETURN, 0);
        }

        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public ReturnOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterReturnOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitReturnOperator(this);
        }
    }

    public static class BinaryConditionalOperatorContext extends ExprContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode BCO() {
            return getToken(MolangParser.BCO, 0);
        }

        public BinaryConditionalOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterBinaryConditionalOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitBinaryConditionalOperator(this);
        }
    }

    public static class EqualsOperatorContext extends ExprContext {
        public List<ExprContext> expr() {
            return getRuleContexts(ExprContext.class);
        }

        public ExprContext expr(int i) {
            return getRuleContext(ExprContext.class, i);
        }

        public TerminalNode EQUALS_OPERATOR() {
            return getToken(MolangParser.EQUALS_OPERATOR, 0);
        }

        public EqualsOperatorContext(ExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterEqualsOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitEqualsOperator(this);
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
                switch (getInterpreter().adaptivePredict(_input, 2, _ctx)) {
                    case 1: {
                        _localctx = new NeExprContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(34);
                        match(S_OPERATOR);
                        setState(35);
                        expr(12);
                    }
                    break;
                    case 2: {
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
                    case 3: {
                        _localctx = new SingleSignedAtomContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(40);
                        signedAtom();
                    }
                    break;
                    case 4: {
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
                _alt = getInterpreter().adaptivePredict(_input, 4, _ctx);
                while (_alt != 2 && _alt != ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(72);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 3, _ctx)) {
                                case 1: {
                                    _localctx = new MulOrDivContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(45);
                                    if (!(precpred(_ctx, 11)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 11)");
                                    setState(46);
                                    ((MulOrDivContext) _localctx).op = _input.LT(1);
                                    _la = _input.LA(1);
                                    if (!(_la == MUL || _la == DIV)) {
                                        ((MulOrDivContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(47);
                                    expr(12);
                                }
                                break;
                                case 2: {
                                    _localctx = new AddOrSubContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(48);
                                    if (!(precpred(_ctx, 10)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 10)");
                                    setState(49);
                                    ((AddOrSubContext) _localctx).op = _input.LT(1);
                                    _la = _input.LA(1);
                                    if (!(_la == ADD || _la == SUB)) {
                                        ((AddOrSubContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(50);
                                    expr(11);
                                }
                                break;
                                case 3: {
                                    _localctx = new ComparisonOperatorContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(51);
                                    if (!(precpred(_ctx, 9)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 9)");
                                    setState(52);
                                    match(COMPARISON_OPERATOR);
                                    setState(53);
                                    expr(10);
                                }
                                break;
                                case 4: {
                                    _localctx = new EqualsOperatorContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(54);
                                    if (!(precpred(_ctx, 8)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 8)");
                                    setState(55);
                                    match(EQUALS_OPERATOR);
                                    setState(56);
                                    expr(9);
                                }
                                break;
                                case 5: {
                                    _localctx = new AndOperatorContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(57);
                                    if (!(precpred(_ctx, 7)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 7)");
                                    setState(58);
                                    match(AND_OPERATOR);
                                    setState(59);
                                    expr(8);
                                }
                                break;
                                case 6: {
                                    _localctx = new OrOperatorContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(60);
                                    if (!(precpred(_ctx, 6)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 6)");
                                    setState(61);
                                    match(OR_OPERATOR);
                                    setState(62);
                                    expr(7);
                                }
                                break;
                                case 7: {
                                    _localctx = new BinaryConditionalOperatorContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(63);
                                    if (!(precpred(_ctx, 5)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 5)");
                                    setState(64);
                                    match(BCO);
                                    setState(65);
                                    expr(6);
                                }
                                break;
                                case 8: {
                                    _localctx = new TernaryConditionalOperatorContext(new ExprContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_expr);
                                    setState(66);
                                    if (!(precpred(_ctx, 4)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 4)");
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
                    _alt = getInterpreter().adaptivePredict(_input, 4, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    public static class FuncParamContext extends ParserRuleContext {
        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public FuncParamContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_funcParam;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterFuncParam(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitFuncParam(this);
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class SignedAtomContext extends ParserRuleContext {
        public TerminalNode ADD() {
            return getToken(MolangParser.ADD, 0);
        }

        public AtomContext atom() {
            return getRuleContext(AtomContext.class, 0);
        }

        public TerminalNode SUB() {
            return getToken(MolangParser.SUB, 0);
        }

        public SignedAtomContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_signedAtom;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterSignedAtom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitSignedAtom(this);
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AtomContext extends ParserRuleContext {
        public FunctionContext function() {
            return getRuleContext(FunctionContext.class, 0);
        }

        public VariableContext variable() {
            return getRuleContext(VariableContext.class, 0);
        }

        public TerminalNode CONSTANT() {
            return getToken(MolangParser.CONSTANT, 0);
        }

        public ScientificContext scientific() {
            return getRuleContext(ScientificContext.class, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(MolangParser.LPAREN, 0);
        }

        public ExprContext expr() {
            return getRuleContext(ExprContext.class, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(MolangParser.RPAREN, 0);
        }

        public AtomContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_atom;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterAtom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitAtom(this);
        }
    }

    public final AtomContext atom() throws RecognitionException {
        AtomContext _localctx = new AtomContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_atom);
        try {
            setState(96);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class ScientificContext extends ParserRuleContext {
        public TerminalNode SCIENTIFIC_NUMBER() {
            return getToken(MolangParser.SCIENTIFIC_NUMBER, 0);
        }

        public ScientificContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_scientific;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterScientific(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitScientific(this);
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class FunctionContext extends ParserRuleContext {
        public FuncnameContext funcname() {
            return getRuleContext(FuncnameContext.class, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(MolangParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(MolangParser.RPAREN, 0);
        }

        public List<FuncParamContext> funcParam() {
            return getRuleContexts(FuncParamContext.class);
        }

        public FuncParamContext funcParam(int i) {
            return getRuleContext(FuncParamContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(MolangParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(MolangParser.COMMA, i);
        }

        public FunctionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_function;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterFunction(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitFunction(this);
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
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << S_OPERATOR) | (1L << RETURN) | (1L << CONSTANT) | (1L << SCIENTIFIC_NUMBER) | (1L << STRING) | (1L << LPAREN) | (1L << ID) | (1L << ADD) | (1L << SUB))) != 0)) {
                    {
                        setState(102);
                        funcParam();
                        setState(107);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la == COMMA) {
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class VariableContext extends ParserRuleContext {
        public List<TerminalNode> ID() {
            return getTokens(MolangParser.ID);
        }

        public TerminalNode ID(int i) {
            return getToken(MolangParser.ID, i);
        }

        public VariableContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variable;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterVariable(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitVariable(this);
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
                _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                while (_alt != 2 && _alt != ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
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
                    _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class FuncnameContext extends ParserRuleContext {
        public List<TerminalNode> ID() {
            return getTokens(MolangParser.ID);
        }

        public TerminalNode ID(int i) {
            return getToken(MolangParser.ID, i);
        }

        public FuncnameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_funcname;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterFuncname(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitFuncname(this);
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
                while (_la == T__0) {
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class StringContext extends ParserRuleContext {
        public TerminalNode STRING() {
            return getToken(MolangParser.STRING, 0);
        }

        public StringContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_string;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterString(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitString(this);
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class AssignmentContext extends ParserRuleContext {
        public TerminalNode Assignment_Operator() {
            return getToken(MolangParser.Assignment_Operator, 0);
        }

        public AssignmentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_assignment;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).enterAssignment(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MolangListener) ((MolangListener) listener).exitAssignment(this);
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
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 1:
                return expr_sempred((ExprContext) _localctx, predIndex);
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
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\35\u0089\4\2\t\2" +
                    "\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13" +
                    "\t\13\4\f\t\f\3\2\3\2\3\2\7\2\34\n\2\f\2\16\2\37\13\2\3\2\5\2\"\n\2\3" +
                    "\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3.\n\3\3\3\3\3\3\3\3\3\3\3\3" +
                    "\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3" +
                    "\3\3\3\3\3\3\3\3\7\3K\n\3\f\3\16\3N\13\3\3\4\3\4\5\4R\n\4\3\5\3\5\3\5" +
                    "\3\5\3\5\5\5Y\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6c\n\6\3\7\3\7\3\b" +
                    "\3\b\3\b\3\b\3\b\7\bl\n\b\f\b\16\bo\13\b\5\bq\n\b\3\b\3\b\3\t\3\t\3\t" +
                    "\7\tx\n\t\f\t\16\t{\13\t\3\n\3\n\3\n\7\n\u0080\n\n\f\n\16\n\u0083\13\n" +
                    "\3\13\3\13\3\f\3\f\3\f\2\3\4\r\2\4\6\b\n\f\16\20\22\24\26\2\4\3\2\32\33" +
                    "\3\2\30\31\2\u0095\2\30\3\2\2\2\4-\3\2\2\2\6Q\3\2\2\2\bX\3\2\2\2\nb\3" +
                    "\2\2\2\fd\3\2\2\2\16f\3\2\2\2\20t\3\2\2\2\22|\3\2\2\2\24\u0084\3\2\2\2" +
                    "\26\u0086\3\2\2\2\30\35\5\4\3\2\31\32\7\35\2\2\32\34\5\4\3\2\33\31\3\2" +
                    "\2\2\34\37\3\2\2\2\35\33\3\2\2\2\35\36\3\2\2\2\36!\3\2\2\2\37\35\3\2\2" +
                    "\2 \"\7\35\2\2! \3\2\2\2!\"\3\2\2\2\"\3\3\2\2\2#$\b\3\1\2$%\7\13\2\2%" +
                    ".\5\4\3\16&\'\5\20\t\2\'(\5\26\f\2()\5\4\3\5).\3\2\2\2*.\5\b\5\2+,\7\f" +
                    "\2\2,.\5\4\3\3-#\3\2\2\2-&\3\2\2\2-*\3\2\2\2-+\3\2\2\2.L\3\2\2\2/\60\f" +
                    "\r\2\2\60\61\t\2\2\2\61K\5\4\3\16\62\63\f\f\2\2\63\64\t\3\2\2\64K\5\4" +
                    "\3\r\65\66\f\13\2\2\66\67\7\5\2\2\67K\5\4\3\f89\f\n\2\29:\7\6\2\2:K\5" +
                    "\4\3\13;<\f\t\2\2<=\7\7\2\2=K\5\4\3\n>?\f\b\2\2?@\7\b\2\2@K\5\4\3\tAB" +
                    "\f\7\2\2BC\7\t\2\2CK\5\4\3\bDE\f\6\2\2EF\7\t\2\2FG\5\4\3\2GH\7\n\2\2H" +
                    "I\5\4\3\7IK\3\2\2\2J/\3\2\2\2J\62\3\2\2\2J\65\3\2\2\2J8\3\2\2\2J;\3\2" +
                    "\2\2J>\3\2\2\2JA\3\2\2\2JD\3\2\2\2KN\3\2\2\2LJ\3\2\2\2LM\3\2\2\2M\5\3" +
                    "\2\2\2NL\3\2\2\2OR\5\4\3\2PR\5\24\13\2QO\3\2\2\2QP\3\2\2\2R\7\3\2\2\2" +
                    "ST\7\30\2\2TY\5\n\6\2UV\7\31\2\2VY\5\n\6\2WY\5\n\6\2XS\3\2\2\2XU\3\2\2" +
                    "\2XW\3\2\2\2Y\t\3\2\2\2Zc\5\16\b\2[c\5\20\t\2\\c\7\r\2\2]c\5\f\7\2^_\7" +
                    "\20\2\2_`\5\4\3\2`a\7\21\2\2ac\3\2\2\2bZ\3\2\2\2b[\3\2\2\2b\\\3\2\2\2" +
                    "b]\3\2\2\2b^\3\2\2\2c\13\3\2\2\2de\7\16\2\2e\r\3\2\2\2fg\5\22\n\2gp\7" +
                    "\20\2\2hm\5\6\4\2ij\7\34\2\2jl\5\6\4\2ki\3\2\2\2lo\3\2\2\2mk\3\2\2\2m" +
                    "n\3\2\2\2nq\3\2\2\2om\3\2\2\2ph\3\2\2\2pq\3\2\2\2qr\3\2\2\2rs\7\21\2\2" +
                    "s\17\3\2\2\2ty\7\26\2\2uv\7\3\2\2vx\7\26\2\2wu\3\2\2\2x{\3\2\2\2yw\3\2" +
                    "\2\2yz\3\2\2\2z\21\3\2\2\2{y\3\2\2\2|\u0081\7\26\2\2}~\7\3\2\2~\u0080" +
                    "\7\26\2\2\177}\3\2\2\2\u0080\u0083\3\2\2\2\u0081\177\3\2\2\2\u0081\u0082" +
                    "\3\2\2\2\u0082\23\3\2\2\2\u0083\u0081\3\2\2\2\u0084\u0085\7\17\2\2\u0085" +
                    "\25\3\2\2\2\u0086\u0087\7\4\2\2\u0087\27\3\2\2\2\16\35!-JLQXbmpy\u0081";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}