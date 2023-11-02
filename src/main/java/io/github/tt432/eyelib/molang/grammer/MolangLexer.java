// Generated from .\Molang.g4 by ANTLR 4.9.1
package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MolangLexer extends Lexer {
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
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "T__0", "NUMBER", "Assignment_Operator", "COMPARISON_OPERATOR", "EQUALS_OPERATOR",
                "AND_OPERATOR", "OR_OPERATOR", "BCO", "TCO0", "S_OPERATOR", "RETURN",
                "CONSTANT", "SCIENTIFIC_NUMBER", "STRING", "LPAREN", "RPAREN", "DQUOT",
                "QUOT", "E1", "E2", "ID", "WS", "ADD", "SUB", "MUL", "DIV", "COMMA",
                "SIEM"
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


    public MolangLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
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
    public String[] getChannelNames() {
        return channelNames;
    }

    @Override
    public String[] getModeNames() {
        return modeNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\35\u00b4\b\1\4\2" +
                    "\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4" +
                    "\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22" +
                    "\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31" +
                    "\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\3\2\3\2\3\3\6\3?\n\3\r\3" +
                    "\16\3@\3\3\3\3\6\3E\n\3\r\3\16\3F\5\3I\n\3\3\4\3\4\3\5\3\5\3\5\3\5\3\5" +
                    "\3\5\5\5S\n\5\3\6\3\6\3\6\3\6\5\6Y\n\6\3\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t" +
                    "\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\16\3\16\3\16" +
                    "\5\16s\n\16\3\16\3\16\5\16w\n\16\3\16\3\16\5\16{\n\16\3\17\3\17\7\17\177" +
                    "\n\17\f\17\16\17\u0082\13\17\3\17\3\17\3\17\3\17\7\17\u0088\n\17\f\17" +
                    "\16\17\u008b\13\17\3\17\3\17\5\17\u008f\n\17\3\20\3\20\3\21\3\21\3\22" +
                    "\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\6\26\u009e\n\26\r\26\16\26\u009f" +
                    "\3\27\6\27\u00a3\n\27\r\27\16\27\u00a4\3\27\3\27\3\30\3\30\3\31\3\31\3" +
                    "\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\4\u0080\u0089\2\36\3\3\5\2\7\4" +
                    "\t\5\13\6\r\7\17\b\21\t\23\n\25\13\27\f\31\r\33\16\35\17\37\20!\21#\22" +
                    "%\23\'\24)\25+\26-\27/\30\61\31\63\32\65\33\67\349\35\3\2\4\5\2C\\aac" +
                    "|\5\2\13\f\17\17\"\"\2\u00c2\2\3\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13" +
                    "\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2" +
                    "\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2" +
                    "!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3" +
                    "\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2" +
                    "\29\3\2\2\2\3;\3\2\2\2\5>\3\2\2\2\7J\3\2\2\2\tR\3\2\2\2\13X\3\2\2\2\r" +
                    "Z\3\2\2\2\17]\3\2\2\2\21`\3\2\2\2\23b\3\2\2\2\25d\3\2\2\2\27f\3\2\2\2" +
                    "\31m\3\2\2\2\33o\3\2\2\2\35\u008e\3\2\2\2\37\u0090\3\2\2\2!\u0092\3\2" +
                    "\2\2#\u0094\3\2\2\2%\u0096\3\2\2\2\'\u0098\3\2\2\2)\u009a\3\2\2\2+\u009d" +
                    "\3\2\2\2-\u00a2\3\2\2\2/\u00a8\3\2\2\2\61\u00aa\3\2\2\2\63\u00ac\3\2\2" +
                    "\2\65\u00ae\3\2\2\2\67\u00b0\3\2\2\29\u00b2\3\2\2\2;<\7\60\2\2<\4\3\2" +
                    "\2\2=?\4\62;\2>=\3\2\2\2?@\3\2\2\2@>\3\2\2\2@A\3\2\2\2AH\3\2\2\2BD\7\60" +
                    "\2\2CE\4\62;\2DC\3\2\2\2EF\3\2\2\2FD\3\2\2\2FG\3\2\2\2GI\3\2\2\2HB\3\2" +
                    "\2\2HI\3\2\2\2I\6\3\2\2\2JK\7?\2\2K\b\3\2\2\2LS\7>\2\2MN\7>\2\2NS\7?\2" +
                    "\2OP\7@\2\2PS\7?\2\2QS\7@\2\2RL\3\2\2\2RM\3\2\2\2RO\3\2\2\2RQ\3\2\2\2" +
                    "S\n\3\2\2\2TU\7?\2\2UY\7?\2\2VW\7#\2\2WY\7?\2\2XT\3\2\2\2XV\3\2\2\2Y\f" +
                    "\3\2\2\2Z[\7(\2\2[\\\7(\2\2\\\16\3\2\2\2]^\7~\2\2^_\7~\2\2_\20\3\2\2\2" +
                    "`a\7A\2\2a\22\3\2\2\2bc\7<\2\2c\24\3\2\2\2de\7#\2\2e\26\3\2\2\2fg\7t\2" +
                    "\2gh\7g\2\2hi\7v\2\2ij\7w\2\2jk\7t\2\2kl\7p\2\2l\30\3\2\2\2mn\7G\2\2n" +
                    "\32\3\2\2\2oz\5\5\3\2ps\5\'\24\2qs\5)\25\2rp\3\2\2\2rq\3\2\2\2sv\3\2\2" +
                    "\2tw\5/\30\2uw\5\61\31\2vt\3\2\2\2vu\3\2\2\2vw\3\2\2\2wx\3\2\2\2xy\5\5" +
                    "\3\2y{\3\2\2\2zr\3\2\2\2z{\3\2\2\2{\34\3\2\2\2|\u0080\5#\22\2}\177\13" +
                    "\2\2\2~}\3\2\2\2\177\u0082\3\2\2\2\u0080\u0081\3\2\2\2\u0080~\3\2\2\2" +
                    "\u0081\u0083\3\2\2\2\u0082\u0080\3\2\2\2\u0083\u0084\5#\22\2\u0084\u008f" +
                    "\3\2\2\2\u0085\u0089\5%\23\2\u0086\u0088\13\2\2\2\u0087\u0086\3\2\2\2" +
                    "\u0088\u008b\3\2\2\2\u0089\u008a\3\2\2\2\u0089\u0087\3\2\2\2\u008a\u008c" +
                    "\3\2\2\2\u008b\u0089\3\2\2\2\u008c\u008d\5%\23\2\u008d\u008f\3\2\2\2\u008e" +
                    "|\3\2\2\2\u008e\u0085\3\2\2\2\u008f\36\3\2\2\2\u0090\u0091\7*\2\2\u0091" +
                    " \3\2\2\2\u0092\u0093\7+\2\2\u0093\"\3\2\2\2\u0094\u0095\7$\2\2\u0095" +
                    "$\3\2\2\2\u0096\u0097\7)\2\2\u0097&\3\2\2\2\u0098\u0099\7G\2\2\u0099(" +
                    "\3\2\2\2\u009a\u009b\7g\2\2\u009b*\3\2\2\2\u009c\u009e\t\2\2\2\u009d\u009c" +
                    "\3\2\2\2\u009e\u009f\3\2\2\2\u009f\u009d\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0" +
                    ",\3\2\2\2\u00a1\u00a3\t\3\2\2\u00a2\u00a1\3\2\2\2\u00a3\u00a4\3\2\2\2" +
                    "\u00a4\u00a2\3\2\2\2\u00a4\u00a5\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a7" +
                    "\b\27\2\2\u00a7.\3\2\2\2\u00a8\u00a9\7-\2\2\u00a9\60\3\2\2\2\u00aa\u00ab" +
                    "\7/\2\2\u00ab\62\3\2\2\2\u00ac\u00ad\7,\2\2\u00ad\64\3\2\2\2\u00ae\u00af" +
                    "\7\61\2\2\u00af\66\3\2\2\2\u00b0\u00b1\7.\2\2\u00b18\3\2\2\2\u00b2\u00b3" +
                    "\7=\2\2\u00b3:\3\2\2\2\20\2@FHRXrvz\u0080\u0089\u008e\u009f\u00a4\3\b" +
                    "\2\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}