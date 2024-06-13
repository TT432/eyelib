// Generated from Molang.g4 by ANTLR 4.9.1
 package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MolangLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, STRING=22, ID=23, SCIENTIFIC_NUMBER=24, 
		WS=25;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "T__12", "T__13", "T__14", "T__15", "T__16", 
			"T__17", "T__18", "T__19", "T__20", "NUMBER", "STRING", "ID", "SCIENTIFIC_NUMBER", 
			"WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'!'", "'*'", "'/'", "'+'", "'-'", "'<'", "'<='", "'>='", 
			"'>'", "'=='", "'!='", "'&&'", "'||'", "'?'", "':'", "'='", "'return'", 
			"'('", "','", "')'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, "STRING", 
			"ID", "SCIENTIFIC_NUMBER", "WS"
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
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Molang.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\33\u00aa\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7"+
		"\3\7\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\r"+
		"\3\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\6\27n\n\27"+
		"\r\27\16\27o\3\27\3\27\6\27t\n\27\r\27\16\27u\5\27x\n\27\3\30\3\30\7\30"+
		"|\n\30\f\30\16\30\177\13\30\3\30\3\30\3\30\7\30\u0084\n\30\f\30\16\30"+
		"\u0087\13\30\3\30\5\30\u008a\n\30\3\31\6\31\u008d\n\31\r\31\16\31\u008e"+
		"\3\31\3\31\6\31\u0093\n\31\r\31\16\31\u0094\7\31\u0097\n\31\f\31\16\31"+
		"\u009a\13\31\3\32\3\32\3\32\5\32\u009f\n\32\3\32\5\32\u00a2\n\32\3\33"+
		"\6\33\u00a5\n\33\r\33\16\33\u00a6\3\33\3\33\4}\u0085\2\34\3\3\5\4\7\5"+
		"\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23"+
		"%\24\'\25)\26+\27-\2/\30\61\31\63\32\65\33\3\2\7\5\2C\\aac|\6\2\62;C\\"+
		"aac|\4\2GGgg\4\2--//\5\2\13\f\17\17\"\"\2\u00b4\2\3\3\2\2\2\2\5\3\2\2"+
		"\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2"+
		"\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3"+
		"\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65"+
		"\3\2\2\2\3\67\3\2\2\2\59\3\2\2\2\7;\3\2\2\2\t=\3\2\2\2\13?\3\2\2\2\rA"+
		"\3\2\2\2\17C\3\2\2\2\21E\3\2\2\2\23H\3\2\2\2\25K\3\2\2\2\27M\3\2\2\2\31"+
		"P\3\2\2\2\33S\3\2\2\2\35V\3\2\2\2\37Y\3\2\2\2![\3\2\2\2#]\3\2\2\2%_\3"+
		"\2\2\2\'f\3\2\2\2)h\3\2\2\2+j\3\2\2\2-m\3\2\2\2/\u0089\3\2\2\2\61\u008c"+
		"\3\2\2\2\63\u009b\3\2\2\2\65\u00a4\3\2\2\2\678\7=\2\28\4\3\2\2\29:\7#"+
		"\2\2:\6\3\2\2\2;<\7,\2\2<\b\3\2\2\2=>\7\61\2\2>\n\3\2\2\2?@\7-\2\2@\f"+
		"\3\2\2\2AB\7/\2\2B\16\3\2\2\2CD\7>\2\2D\20\3\2\2\2EF\7>\2\2FG\7?\2\2G"+
		"\22\3\2\2\2HI\7@\2\2IJ\7?\2\2J\24\3\2\2\2KL\7@\2\2L\26\3\2\2\2MN\7?\2"+
		"\2NO\7?\2\2O\30\3\2\2\2PQ\7#\2\2QR\7?\2\2R\32\3\2\2\2ST\7(\2\2TU\7(\2"+
		"\2U\34\3\2\2\2VW\7~\2\2WX\7~\2\2X\36\3\2\2\2YZ\7A\2\2Z \3\2\2\2[\\\7<"+
		"\2\2\\\"\3\2\2\2]^\7?\2\2^$\3\2\2\2_`\7t\2\2`a\7g\2\2ab\7v\2\2bc\7w\2"+
		"\2cd\7t\2\2de\7p\2\2e&\3\2\2\2fg\7*\2\2g(\3\2\2\2hi\7.\2\2i*\3\2\2\2j"+
		"k\7+\2\2k,\3\2\2\2ln\4\62;\2ml\3\2\2\2no\3\2\2\2om\3\2\2\2op\3\2\2\2p"+
		"w\3\2\2\2qs\7\60\2\2rt\4\62;\2sr\3\2\2\2tu\3\2\2\2us\3\2\2\2uv\3\2\2\2"+
		"vx\3\2\2\2wq\3\2\2\2wx\3\2\2\2x.\3\2\2\2y}\7$\2\2z|\13\2\2\2{z\3\2\2\2"+
		"|\177\3\2\2\2}~\3\2\2\2}{\3\2\2\2~\u0080\3\2\2\2\177}\3\2\2\2\u0080\u008a"+
		"\7$\2\2\u0081\u0085\7)\2\2\u0082\u0084\13\2\2\2\u0083\u0082\3\2\2\2\u0084"+
		"\u0087\3\2\2\2\u0085\u0086\3\2\2\2\u0085\u0083\3\2\2\2\u0086\u0088\3\2"+
		"\2\2\u0087\u0085\3\2\2\2\u0088\u008a\7)\2\2\u0089y\3\2\2\2\u0089\u0081"+
		"\3\2\2\2\u008a\60\3\2\2\2\u008b\u008d\t\2\2\2\u008c\u008b\3\2\2\2\u008d"+
		"\u008e\3\2\2\2\u008e\u008c\3\2\2\2\u008e\u008f\3\2\2\2\u008f\u0098\3\2"+
		"\2\2\u0090\u0092\7\60\2\2\u0091\u0093\t\3\2\2\u0092\u0091\3\2\2\2\u0093"+
		"\u0094\3\2\2\2\u0094\u0092\3\2\2\2\u0094\u0095\3\2\2\2\u0095\u0097\3\2"+
		"\2\2\u0096\u0090\3\2\2\2\u0097\u009a\3\2\2\2\u0098\u0096\3\2\2\2\u0098"+
		"\u0099\3\2\2\2\u0099\62\3\2\2\2\u009a\u0098\3\2\2\2\u009b\u00a1\5-\27"+
		"\2\u009c\u009e\t\4\2\2\u009d\u009f\t\5\2\2\u009e\u009d\3\2\2\2\u009e\u009f"+
		"\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0\u00a2\5-\27\2\u00a1\u009c\3\2\2\2\u00a1"+
		"\u00a2\3\2\2\2\u00a2\64\3\2\2\2\u00a3\u00a5\t\6\2\2\u00a4\u00a3\3\2\2"+
		"\2\u00a5\u00a6\3\2\2\2\u00a6\u00a4\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\u00a8"+
		"\3\2\2\2\u00a8\u00a9\b\33\2\2\u00a9\66\3\2\2\2\17\2ouw}\u0085\u0089\u008e"+
		"\u0094\u0098\u009e\u00a1\u00a6\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}