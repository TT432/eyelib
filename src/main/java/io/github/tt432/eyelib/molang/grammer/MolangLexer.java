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
		"\u0004\u0000\u0019\u00a8\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0002\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0001\u0000\u0001\u0000"+
		"\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\t\u0001"+
		"\t\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f"+
		"\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001"+
		"\u0012\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0015\u0004"+
		"\u0015l\b\u0015\u000b\u0015\f\u0015m\u0001\u0015\u0001\u0015\u0004\u0015"+
		"r\b\u0015\u000b\u0015\f\u0015s\u0003\u0015v\b\u0015\u0001\u0016\u0001"+
		"\u0016\u0005\u0016z\b\u0016\n\u0016\f\u0016}\t\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0005\u0016\u0082\b\u0016\n\u0016\f\u0016\u0085\t\u0016"+
		"\u0001\u0016\u0003\u0016\u0088\b\u0016\u0001\u0017\u0004\u0017\u008b\b"+
		"\u0017\u000b\u0017\f\u0017\u008c\u0001\u0017\u0001\u0017\u0004\u0017\u0091"+
		"\b\u0017\u000b\u0017\f\u0017\u0092\u0005\u0017\u0095\b\u0017\n\u0017\f"+
		"\u0017\u0098\t\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0003\u0018\u009d"+
		"\b\u0018\u0001\u0018\u0003\u0018\u00a0\b\u0018\u0001\u0019\u0004\u0019"+
		"\u00a3\b\u0019\u000b\u0019\f\u0019\u00a4\u0001\u0019\u0001\u0019\u0002"+
		"{\u0083\u0000\u001a\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t"+
		"\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f"+
		"\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012%\u0013\'\u0014"+
		")\u0015+\u0000-\u0016/\u00171\u00183\u0019\u0001\u0000\u0005\u0003\u0000"+
		"AZ__az\u0004\u000009AZ__az\u0002\u0000EEee\u0002\u0000++--\u0003\u0000"+
		"\t\n\r\r  \u00b2\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001"+
		"\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001"+
		"\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000"+
		"\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000"+
		"\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000"+
		"\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000"+
		"\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000"+
		"\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000"+
		"\u0000\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0000"+
		"%\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000\u0000\u0000)\u0001"+
		"\u0000\u0000\u0000\u0000-\u0001\u0000\u0000\u0000\u0000/\u0001\u0000\u0000"+
		"\u0000\u00001\u0001\u0000\u0000\u0000\u00003\u0001\u0000\u0000\u0000\u0001"+
		"5\u0001\u0000\u0000\u0000\u00037\u0001\u0000\u0000\u0000\u00059\u0001"+
		"\u0000\u0000\u0000\u0007;\u0001\u0000\u0000\u0000\t=\u0001\u0000\u0000"+
		"\u0000\u000b?\u0001\u0000\u0000\u0000\rA\u0001\u0000\u0000\u0000\u000f"+
		"C\u0001\u0000\u0000\u0000\u0011F\u0001\u0000\u0000\u0000\u0013I\u0001"+
		"\u0000\u0000\u0000\u0015K\u0001\u0000\u0000\u0000\u0017N\u0001\u0000\u0000"+
		"\u0000\u0019Q\u0001\u0000\u0000\u0000\u001bT\u0001\u0000\u0000\u0000\u001d"+
		"W\u0001\u0000\u0000\u0000\u001fY\u0001\u0000\u0000\u0000![\u0001\u0000"+
		"\u0000\u0000#]\u0001\u0000\u0000\u0000%d\u0001\u0000\u0000\u0000\'f\u0001"+
		"\u0000\u0000\u0000)h\u0001\u0000\u0000\u0000+k\u0001\u0000\u0000\u0000"+
		"-\u0087\u0001\u0000\u0000\u0000/\u008a\u0001\u0000\u0000\u00001\u0099"+
		"\u0001\u0000\u0000\u00003\u00a2\u0001\u0000\u0000\u000056\u0005;\u0000"+
		"\u00006\u0002\u0001\u0000\u0000\u000078\u0005!\u0000\u00008\u0004\u0001"+
		"\u0000\u0000\u00009:\u0005*\u0000\u0000:\u0006\u0001\u0000\u0000\u0000"+
		";<\u0005/\u0000\u0000<\b\u0001\u0000\u0000\u0000=>\u0005+\u0000\u0000"+
		">\n\u0001\u0000\u0000\u0000?@\u0005-\u0000\u0000@\f\u0001\u0000\u0000"+
		"\u0000AB\u0005<\u0000\u0000B\u000e\u0001\u0000\u0000\u0000CD\u0005<\u0000"+
		"\u0000DE\u0005=\u0000\u0000E\u0010\u0001\u0000\u0000\u0000FG\u0005>\u0000"+
		"\u0000GH\u0005=\u0000\u0000H\u0012\u0001\u0000\u0000\u0000IJ\u0005>\u0000"+
		"\u0000J\u0014\u0001\u0000\u0000\u0000KL\u0005=\u0000\u0000LM\u0005=\u0000"+
		"\u0000M\u0016\u0001\u0000\u0000\u0000NO\u0005!\u0000\u0000OP\u0005=\u0000"+
		"\u0000P\u0018\u0001\u0000\u0000\u0000QR\u0005&\u0000\u0000RS\u0005&\u0000"+
		"\u0000S\u001a\u0001\u0000\u0000\u0000TU\u0005|\u0000\u0000UV\u0005|\u0000"+
		"\u0000V\u001c\u0001\u0000\u0000\u0000WX\u0005?\u0000\u0000X\u001e\u0001"+
		"\u0000\u0000\u0000YZ\u0005:\u0000\u0000Z \u0001\u0000\u0000\u0000[\\\u0005"+
		"=\u0000\u0000\\\"\u0001\u0000\u0000\u0000]^\u0005r\u0000\u0000^_\u0005"+
		"e\u0000\u0000_`\u0005t\u0000\u0000`a\u0005u\u0000\u0000ab\u0005r\u0000"+
		"\u0000bc\u0005n\u0000\u0000c$\u0001\u0000\u0000\u0000de\u0005(\u0000\u0000"+
		"e&\u0001\u0000\u0000\u0000fg\u0005,\u0000\u0000g(\u0001\u0000\u0000\u0000"+
		"hi\u0005)\u0000\u0000i*\u0001\u0000\u0000\u0000jl\u000209\u0000kj\u0001"+
		"\u0000\u0000\u0000lm\u0001\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000"+
		"mn\u0001\u0000\u0000\u0000nu\u0001\u0000\u0000\u0000oq\u0005.\u0000\u0000"+
		"pr\u000209\u0000qp\u0001\u0000\u0000\u0000rs\u0001\u0000\u0000\u0000s"+
		"q\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000tv\u0001\u0000\u0000"+
		"\u0000uo\u0001\u0000\u0000\u0000uv\u0001\u0000\u0000\u0000v,\u0001\u0000"+
		"\u0000\u0000w{\u0005\"\u0000\u0000xz\t\u0000\u0000\u0000yx\u0001\u0000"+
		"\u0000\u0000z}\u0001\u0000\u0000\u0000{|\u0001\u0000\u0000\u0000{y\u0001"+
		"\u0000\u0000\u0000|~\u0001\u0000\u0000\u0000}{\u0001\u0000\u0000\u0000"+
		"~\u0088\u0005\"\u0000\u0000\u007f\u0083\u0005\'\u0000\u0000\u0080\u0082"+
		"\t\u0000\u0000\u0000\u0081\u0080\u0001\u0000\u0000\u0000\u0082\u0085\u0001"+
		"\u0000\u0000\u0000\u0083\u0084\u0001\u0000\u0000\u0000\u0083\u0081\u0001"+
		"\u0000\u0000\u0000\u0084\u0086\u0001\u0000\u0000\u0000\u0085\u0083\u0001"+
		"\u0000\u0000\u0000\u0086\u0088\u0005\'\u0000\u0000\u0087w\u0001\u0000"+
		"\u0000\u0000\u0087\u007f\u0001\u0000\u0000\u0000\u0088.\u0001\u0000\u0000"+
		"\u0000\u0089\u008b\u0007\u0000\u0000\u0000\u008a\u0089\u0001\u0000\u0000"+
		"\u0000\u008b\u008c\u0001\u0000\u0000\u0000\u008c\u008a\u0001\u0000\u0000"+
		"\u0000\u008c\u008d\u0001\u0000\u0000\u0000\u008d\u0096\u0001\u0000\u0000"+
		"\u0000\u008e\u0090\u0005.\u0000\u0000\u008f\u0091\u0007\u0001\u0000\u0000"+
		"\u0090\u008f\u0001\u0000\u0000\u0000\u0091\u0092\u0001\u0000\u0000\u0000"+
		"\u0092\u0090\u0001\u0000\u0000\u0000\u0092\u0093\u0001\u0000\u0000\u0000"+
		"\u0093\u0095\u0001\u0000\u0000\u0000\u0094\u008e\u0001\u0000\u0000\u0000"+
		"\u0095\u0098\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000\u0000\u0000"+
		"\u0096\u0097\u0001\u0000\u0000\u0000\u00970\u0001\u0000\u0000\u0000\u0098"+
		"\u0096\u0001\u0000\u0000\u0000\u0099\u009f\u0003+\u0015\u0000\u009a\u009c"+
		"\u0007\u0002\u0000\u0000\u009b\u009d\u0007\u0003\u0000\u0000\u009c\u009b"+
		"\u0001\u0000\u0000\u0000\u009c\u009d\u0001\u0000\u0000\u0000\u009d\u009e"+
		"\u0001\u0000\u0000\u0000\u009e\u00a0\u0003+\u0015\u0000\u009f\u009a\u0001"+
		"\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000\u0000\u00a02\u0001\u0000"+
		"\u0000\u0000\u00a1\u00a3\u0007\u0004\u0000\u0000\u00a2\u00a1\u0001\u0000"+
		"\u0000\u0000\u00a3\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a6\u0001\u0000"+
		"\u0000\u0000\u00a6\u00a7\u0006\u0019\u0000\u0000\u00a74\u0001\u0000\u0000"+
		"\u0000\r\u0000msu{\u0083\u0087\u008c\u0092\u0096\u009c\u009f\u00a4\u0001"+
		"\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}