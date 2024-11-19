// Generated from Molang.g4 by ANTLR 4.13.1
 package io.github.tt432.eyelib.molang.grammer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class MolangLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, RETURN=28, STRING=29, ID=30, SCIENTIFIC_NUMBER=31, 
		WS=32;
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
			"T__17", "T__18", "T__19", "T__20", "T__21", "T__22", "T__23", "T__24", 
			"T__25", "T__26", "RETURN", "NUMBER", "STRING", "ID", "SCIENTIFIC_NUMBER", 
			"WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'{'", "'}'", "'!'", "'*'", "'/'", "'+'", "'-'", "'<'", 
			"'<='", "'>='", "'>'", "'=='", "'!='", "'&&'", "'||'", "'?'", "':'", 
			"'='", "'->'", "'??'", "'('", "')'", "'this'", "','", "'['", "']'", "'return'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, "RETURN", "STRING", "ID", "SCIENTIFIC_NUMBER", 
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
		"\u0004\u0000 \u00c9\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002"+
		"\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002"+
		"\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002"+
		"\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002"+
		"\u001b\u0007\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002"+
		"\u001e\u0007\u001e\u0002\u001f\u0007\u001f\u0002 \u0007 \u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t"+
		"\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001"+
		"\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018"+
		"\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001b"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0001\u001c\u0004\u001c\u008d\b\u001c\u000b\u001c\f\u001c\u008e\u0001"+
		"\u001c\u0001\u001c\u0004\u001c\u0093\b\u001c\u000b\u001c\f\u001c\u0094"+
		"\u0003\u001c\u0097\b\u001c\u0001\u001d\u0001\u001d\u0005\u001d\u009b\b"+
		"\u001d\n\u001d\f\u001d\u009e\t\u001d\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0005\u001d\u00a3\b\u001d\n\u001d\f\u001d\u00a6\t\u001d\u0001\u001d\u0003"+
		"\u001d\u00a9\b\u001d\u0001\u001e\u0004\u001e\u00ac\b\u001e\u000b\u001e"+
		"\f\u001e\u00ad\u0001\u001e\u0001\u001e\u0004\u001e\u00b2\b\u001e\u000b"+
		"\u001e\f\u001e\u00b3\u0005\u001e\u00b6\b\u001e\n\u001e\f\u001e\u00b9\t"+
		"\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u00be\b\u001f\u0001"+
		"\u001f\u0003\u001f\u00c1\b\u001f\u0001 \u0004 \u00c4\b \u000b \f \u00c5"+
		"\u0001 \u0001 \u0002\u009c\u00a4\u0000!\u0001\u0001\u0003\u0002\u0005"+
		"\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n"+
		"\u0015\u000b\u0017\f\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011"+
		"#\u0012%\u0013\'\u0014)\u0015+\u0016-\u0017/\u00181\u00193\u001a5\u001b"+
		"7\u001c9\u0000;\u001d=\u001e?\u001fA \u0001\u0000\u0005\u0003\u0000AZ"+
		"__az\u0004\u000009AZ__az\u0002\u0000EEee\u0002\u0000++--\u0003\u0000\t"+
		"\n\r\r  \u00d3\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000"+
		"\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000"+
		"\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000"+
		"\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000"+
		"\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000"+
		"\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000"+
		"\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000"+
		"\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000"+
		"\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0000%"+
		"\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000\u0000\u0000)\u0001"+
		"\u0000\u0000\u0000\u0000+\u0001\u0000\u0000\u0000\u0000-\u0001\u0000\u0000"+
		"\u0000\u0000/\u0001\u0000\u0000\u0000\u00001\u0001\u0000\u0000\u0000\u0000"+
		"3\u0001\u0000\u0000\u0000\u00005\u0001\u0000\u0000\u0000\u00007\u0001"+
		"\u0000\u0000\u0000\u0000;\u0001\u0000\u0000\u0000\u0000=\u0001\u0000\u0000"+
		"\u0000\u0000?\u0001\u0000\u0000\u0000\u0000A\u0001\u0000\u0000\u0000\u0001"+
		"C\u0001\u0000\u0000\u0000\u0003E\u0001\u0000\u0000\u0000\u0005G\u0001"+
		"\u0000\u0000\u0000\u0007I\u0001\u0000\u0000\u0000\tK\u0001\u0000\u0000"+
		"\u0000\u000bM\u0001\u0000\u0000\u0000\rO\u0001\u0000\u0000\u0000\u000f"+
		"Q\u0001\u0000\u0000\u0000\u0011S\u0001\u0000\u0000\u0000\u0013U\u0001"+
		"\u0000\u0000\u0000\u0015X\u0001\u0000\u0000\u0000\u0017[\u0001\u0000\u0000"+
		"\u0000\u0019]\u0001\u0000\u0000\u0000\u001b`\u0001\u0000\u0000\u0000\u001d"+
		"c\u0001\u0000\u0000\u0000\u001ff\u0001\u0000\u0000\u0000!i\u0001\u0000"+
		"\u0000\u0000#k\u0001\u0000\u0000\u0000%m\u0001\u0000\u0000\u0000\'o\u0001"+
		"\u0000\u0000\u0000)r\u0001\u0000\u0000\u0000+u\u0001\u0000\u0000\u0000"+
		"-w\u0001\u0000\u0000\u0000/y\u0001\u0000\u0000\u00001~\u0001\u0000\u0000"+
		"\u00003\u0080\u0001\u0000\u0000\u00005\u0082\u0001\u0000\u0000\u00007"+
		"\u0084\u0001\u0000\u0000\u00009\u008c\u0001\u0000\u0000\u0000;\u00a8\u0001"+
		"\u0000\u0000\u0000=\u00ab\u0001\u0000\u0000\u0000?\u00ba\u0001\u0000\u0000"+
		"\u0000A\u00c3\u0001\u0000\u0000\u0000CD\u0005;\u0000\u0000D\u0002\u0001"+
		"\u0000\u0000\u0000EF\u0005{\u0000\u0000F\u0004\u0001\u0000\u0000\u0000"+
		"GH\u0005}\u0000\u0000H\u0006\u0001\u0000\u0000\u0000IJ\u0005!\u0000\u0000"+
		"J\b\u0001\u0000\u0000\u0000KL\u0005*\u0000\u0000L\n\u0001\u0000\u0000"+
		"\u0000MN\u0005/\u0000\u0000N\f\u0001\u0000\u0000\u0000OP\u0005+\u0000"+
		"\u0000P\u000e\u0001\u0000\u0000\u0000QR\u0005-\u0000\u0000R\u0010\u0001"+
		"\u0000\u0000\u0000ST\u0005<\u0000\u0000T\u0012\u0001\u0000\u0000\u0000"+
		"UV\u0005<\u0000\u0000VW\u0005=\u0000\u0000W\u0014\u0001\u0000\u0000\u0000"+
		"XY\u0005>\u0000\u0000YZ\u0005=\u0000\u0000Z\u0016\u0001\u0000\u0000\u0000"+
		"[\\\u0005>\u0000\u0000\\\u0018\u0001\u0000\u0000\u0000]^\u0005=\u0000"+
		"\u0000^_\u0005=\u0000\u0000_\u001a\u0001\u0000\u0000\u0000`a\u0005!\u0000"+
		"\u0000ab\u0005=\u0000\u0000b\u001c\u0001\u0000\u0000\u0000cd\u0005&\u0000"+
		"\u0000de\u0005&\u0000\u0000e\u001e\u0001\u0000\u0000\u0000fg\u0005|\u0000"+
		"\u0000gh\u0005|\u0000\u0000h \u0001\u0000\u0000\u0000ij\u0005?\u0000\u0000"+
		"j\"\u0001\u0000\u0000\u0000kl\u0005:\u0000\u0000l$\u0001\u0000\u0000\u0000"+
		"mn\u0005=\u0000\u0000n&\u0001\u0000\u0000\u0000op\u0005-\u0000\u0000p"+
		"q\u0005>\u0000\u0000q(\u0001\u0000\u0000\u0000rs\u0005?\u0000\u0000st"+
		"\u0005?\u0000\u0000t*\u0001\u0000\u0000\u0000uv\u0005(\u0000\u0000v,\u0001"+
		"\u0000\u0000\u0000wx\u0005)\u0000\u0000x.\u0001\u0000\u0000\u0000yz\u0005"+
		"t\u0000\u0000z{\u0005h\u0000\u0000{|\u0005i\u0000\u0000|}\u0005s\u0000"+
		"\u0000}0\u0001\u0000\u0000\u0000~\u007f\u0005,\u0000\u0000\u007f2\u0001"+
		"\u0000\u0000\u0000\u0080\u0081\u0005[\u0000\u0000\u00814\u0001\u0000\u0000"+
		"\u0000\u0082\u0083\u0005]\u0000\u0000\u00836\u0001\u0000\u0000\u0000\u0084"+
		"\u0085\u0005r\u0000\u0000\u0085\u0086\u0005e\u0000\u0000\u0086\u0087\u0005"+
		"t\u0000\u0000\u0087\u0088\u0005u\u0000\u0000\u0088\u0089\u0005r\u0000"+
		"\u0000\u0089\u008a\u0005n\u0000\u0000\u008a8\u0001\u0000\u0000\u0000\u008b"+
		"\u008d\u000209\u0000\u008c\u008b\u0001\u0000\u0000\u0000\u008d\u008e\u0001"+
		"\u0000\u0000\u0000\u008e\u008c\u0001\u0000\u0000\u0000\u008e\u008f\u0001"+
		"\u0000\u0000\u0000\u008f\u0096\u0001\u0000\u0000\u0000\u0090\u0092\u0005"+
		".\u0000\u0000\u0091\u0093\u000209\u0000\u0092\u0091\u0001\u0000\u0000"+
		"\u0000\u0093\u0094\u0001\u0000\u0000\u0000\u0094\u0092\u0001\u0000\u0000"+
		"\u0000\u0094\u0095\u0001\u0000\u0000\u0000\u0095\u0097\u0001\u0000\u0000"+
		"\u0000\u0096\u0090\u0001\u0000\u0000\u0000\u0096\u0097\u0001\u0000\u0000"+
		"\u0000\u0097:\u0001\u0000\u0000\u0000\u0098\u009c\u0005\"\u0000\u0000"+
		"\u0099\u009b\t\u0000\u0000\u0000\u009a\u0099\u0001\u0000\u0000\u0000\u009b"+
		"\u009e\u0001\u0000\u0000\u0000\u009c\u009d\u0001\u0000\u0000\u0000\u009c"+
		"\u009a\u0001\u0000\u0000\u0000\u009d\u009f\u0001\u0000\u0000\u0000\u009e"+
		"\u009c\u0001\u0000\u0000\u0000\u009f\u00a9\u0005\"\u0000\u0000\u00a0\u00a4"+
		"\u0005\'\u0000\u0000\u00a1\u00a3\t\u0000\u0000\u0000\u00a2\u00a1\u0001"+
		"\u0000\u0000\u0000\u00a3\u00a6\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001"+
		"\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a5\u00a7\u0001"+
		"\u0000\u0000\u0000\u00a6\u00a4\u0001\u0000\u0000\u0000\u00a7\u00a9\u0005"+
		"\'\u0000\u0000\u00a8\u0098\u0001\u0000\u0000\u0000\u00a8\u00a0\u0001\u0000"+
		"\u0000\u0000\u00a9<\u0001\u0000\u0000\u0000\u00aa\u00ac\u0007\u0000\u0000"+
		"\u0000\u00ab\u00aa\u0001\u0000\u0000\u0000\u00ac\u00ad\u0001\u0000\u0000"+
		"\u0000\u00ad\u00ab\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000"+
		"\u0000\u00ae\u00b7\u0001\u0000\u0000\u0000\u00af\u00b1\u0005.\u0000\u0000"+
		"\u00b0\u00b2\u0007\u0001\u0000\u0000\u00b1\u00b0\u0001\u0000\u0000\u0000"+
		"\u00b2\u00b3\u0001\u0000\u0000\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000"+
		"\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b6\u0001\u0000\u0000\u0000"+
		"\u00b5\u00af\u0001\u0000\u0000\u0000\u00b6\u00b9\u0001\u0000\u0000\u0000"+
		"\u00b7\u00b5\u0001\u0000\u0000\u0000\u00b7\u00b8\u0001\u0000\u0000\u0000"+
		"\u00b8>\u0001\u0000\u0000\u0000\u00b9\u00b7\u0001\u0000\u0000\u0000\u00ba"+
		"\u00c0\u00039\u001c\u0000\u00bb\u00bd\u0007\u0002\u0000\u0000\u00bc\u00be"+
		"\u0007\u0003\u0000\u0000\u00bd\u00bc\u0001\u0000\u0000\u0000\u00bd\u00be"+
		"\u0001\u0000\u0000\u0000\u00be\u00bf\u0001\u0000\u0000\u0000\u00bf\u00c1"+
		"\u00039\u001c\u0000\u00c0\u00bb\u0001\u0000\u0000\u0000\u00c0\u00c1\u0001"+
		"\u0000\u0000\u0000\u00c1@\u0001\u0000\u0000\u0000\u00c2\u00c4\u0007\u0004"+
		"\u0000\u0000\u00c3\u00c2\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001\u0000"+
		"\u0000\u0000\u00c5\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000"+
		"\u0000\u0000\u00c6\u00c7\u0001\u0000\u0000\u0000\u00c7\u00c8\u0006 \u0000"+
		"\u0000\u00c8B\u0001\u0000\u0000\u0000\r\u0000\u008e\u0094\u0096\u009c"+
		"\u00a4\u00a8\u00ad\u00b3\u00b7\u00bd\u00c0\u00c5\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}