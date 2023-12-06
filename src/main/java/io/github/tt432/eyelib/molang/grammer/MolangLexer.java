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
		T__0=1, Assignment_Operator=2, COMPARISON_OPERATOR=3, EQUALS_OPERATOR=4, 
		AND_OPERATOR=5, OR_OPERATOR=6, BCO=7, TCO0=8, S_OPERATOR=9, RETURN=10, 
		CONSTANT=11, SCIENTIFIC_NUMBER=12, STRING=13, LPAREN=14, RPAREN=15, DQUOT=16, 
		QUOT=17, E1=18, E2=19, ID=20, WS=21, ADD=22, SUB=23, MUL=24, DIV=25, COMMA=26, 
		SIEM=27;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "NUMBER", "Assignment_Operator", "COMPARISON_OPERATOR", "EQUALS_OPERATOR", 
			"AND_OPERATOR", "OR_OPERATOR", "BCO", "TCO0", "S_OPERATOR", "RETURN", 
			"CONSTANT", "SCIENTIFIC_NUMBER", "STRING", "LPAREN", "RPAREN", "DQUOT", 
			"QUOT", "E1", "E2", "ID", "WS", "ADD", "SUB", "MUL", "DIV", "COMMA", 
			"SIEM"
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
		"\u0004\u0000\u001b\u00b2\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0002\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a"+
		"\u0002\u001b\u0007\u001b\u0001\u0000\u0001\u0000\u0001\u0001\u0004\u0001"+
		"=\b\u0001\u000b\u0001\f\u0001>\u0001\u0001\u0001\u0001\u0004\u0001C\b"+
		"\u0001\u000b\u0001\f\u0001D\u0003\u0001G\b\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003Q\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0003\u0004W\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001"+
		"\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0003\fq\b\f\u0001\f\u0001"+
		"\f\u0003\fu\b\f\u0001\f\u0001\f\u0003\fy\b\f\u0001\r\u0001\r\u0005\r}"+
		"\b\r\n\r\f\r\u0080\t\r\u0001\r\u0001\r\u0001\r\u0001\r\u0005\r\u0086\b"+
		"\r\n\r\f\r\u0089\t\r\u0001\r\u0001\r\u0003\r\u008d\b\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0014\u0004"+
		"\u0014\u009c\b\u0014\u000b\u0014\f\u0014\u009d\u0001\u0015\u0004\u0015"+
		"\u00a1\b\u0015\u000b\u0015\f\u0015\u00a2\u0001\u0015\u0001\u0015\u0001"+
		"\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001"+
		"\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b\u0002"+
		"~\u0087\u0000\u001c\u0001\u0001\u0003\u0000\u0005\u0002\u0007\u0003\t"+
		"\u0004\u000b\u0005\r\u0006\u000f\u0007\u0011\b\u0013\t\u0015\n\u0017\u000b"+
		"\u0019\f\u001b\r\u001d\u000e\u001f\u000f!\u0010#\u0011%\u0012\'\u0013"+
		")\u0014+\u0015-\u0016/\u00171\u00183\u00195\u001a7\u001b\u0001\u0000\u0002"+
		"\u0003\u0000AZ__az\u0003\u0000\t\n\r\r  \u00c0\u0000\u0001\u0001\u0000"+
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
		"\u0000\u0000\u0000\u00019\u0001\u0000\u0000\u0000\u0003<\u0001\u0000\u0000"+
		"\u0000\u0005H\u0001\u0000\u0000\u0000\u0007P\u0001\u0000\u0000\u0000\t"+
		"V\u0001\u0000\u0000\u0000\u000bX\u0001\u0000\u0000\u0000\r[\u0001\u0000"+
		"\u0000\u0000\u000f^\u0001\u0000\u0000\u0000\u0011`\u0001\u0000\u0000\u0000"+
		"\u0013b\u0001\u0000\u0000\u0000\u0015d\u0001\u0000\u0000\u0000\u0017k"+
		"\u0001\u0000\u0000\u0000\u0019m\u0001\u0000\u0000\u0000\u001b\u008c\u0001"+
		"\u0000\u0000\u0000\u001d\u008e\u0001\u0000\u0000\u0000\u001f\u0090\u0001"+
		"\u0000\u0000\u0000!\u0092\u0001\u0000\u0000\u0000#\u0094\u0001\u0000\u0000"+
		"\u0000%\u0096\u0001\u0000\u0000\u0000\'\u0098\u0001\u0000\u0000\u0000"+
		")\u009b\u0001\u0000\u0000\u0000+\u00a0\u0001\u0000\u0000\u0000-\u00a6"+
		"\u0001\u0000\u0000\u0000/\u00a8\u0001\u0000\u0000\u00001\u00aa\u0001\u0000"+
		"\u0000\u00003\u00ac\u0001\u0000\u0000\u00005\u00ae\u0001\u0000\u0000\u0000"+
		"7\u00b0\u0001\u0000\u0000\u00009:\u0005.\u0000\u0000:\u0002\u0001\u0000"+
		"\u0000\u0000;=\u000209\u0000<;\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000"+
		"\u0000><\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000\u0000?F\u0001\u0000"+
		"\u0000\u0000@B\u0005.\u0000\u0000AC\u000209\u0000BA\u0001\u0000\u0000"+
		"\u0000CD\u0001\u0000\u0000\u0000DB\u0001\u0000\u0000\u0000DE\u0001\u0000"+
		"\u0000\u0000EG\u0001\u0000\u0000\u0000F@\u0001\u0000\u0000\u0000FG\u0001"+
		"\u0000\u0000\u0000G\u0004\u0001\u0000\u0000\u0000HI\u0005=\u0000\u0000"+
		"I\u0006\u0001\u0000\u0000\u0000JQ\u0005<\u0000\u0000KL\u0005<\u0000\u0000"+
		"LQ\u0005=\u0000\u0000MN\u0005>\u0000\u0000NQ\u0005=\u0000\u0000OQ\u0005"+
		">\u0000\u0000PJ\u0001\u0000\u0000\u0000PK\u0001\u0000\u0000\u0000PM\u0001"+
		"\u0000\u0000\u0000PO\u0001\u0000\u0000\u0000Q\b\u0001\u0000\u0000\u0000"+
		"RS\u0005=\u0000\u0000SW\u0005=\u0000\u0000TU\u0005!\u0000\u0000UW\u0005"+
		"=\u0000\u0000VR\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000\u0000W\n\u0001"+
		"\u0000\u0000\u0000XY\u0005&\u0000\u0000YZ\u0005&\u0000\u0000Z\f\u0001"+
		"\u0000\u0000\u0000[\\\u0005|\u0000\u0000\\]\u0005|\u0000\u0000]\u000e"+
		"\u0001\u0000\u0000\u0000^_\u0005?\u0000\u0000_\u0010\u0001\u0000\u0000"+
		"\u0000`a\u0005:\u0000\u0000a\u0012\u0001\u0000\u0000\u0000bc\u0005!\u0000"+
		"\u0000c\u0014\u0001\u0000\u0000\u0000de\u0005r\u0000\u0000ef\u0005e\u0000"+
		"\u0000fg\u0005t\u0000\u0000gh\u0005u\u0000\u0000hi\u0005r\u0000\u0000"+
		"ij\u0005n\u0000\u0000j\u0016\u0001\u0000\u0000\u0000kl\u0005E\u0000\u0000"+
		"l\u0018\u0001\u0000\u0000\u0000mx\u0003\u0003\u0001\u0000nq\u0003%\u0012"+
		"\u0000oq\u0003\'\u0013\u0000pn\u0001\u0000\u0000\u0000po\u0001\u0000\u0000"+
		"\u0000qt\u0001\u0000\u0000\u0000ru\u0003-\u0016\u0000su\u0003/\u0017\u0000"+
		"tr\u0001\u0000\u0000\u0000ts\u0001\u0000\u0000\u0000tu\u0001\u0000\u0000"+
		"\u0000uv\u0001\u0000\u0000\u0000vw\u0003\u0003\u0001\u0000wy\u0001\u0000"+
		"\u0000\u0000xp\u0001\u0000\u0000\u0000xy\u0001\u0000\u0000\u0000y\u001a"+
		"\u0001\u0000\u0000\u0000z~\u0003!\u0010\u0000{}\t\u0000\u0000\u0000|{"+
		"\u0001\u0000\u0000\u0000}\u0080\u0001\u0000\u0000\u0000~\u007f\u0001\u0000"+
		"\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f\u0081\u0001\u0000\u0000\u0000"+
		"\u0080~\u0001\u0000\u0000\u0000\u0081\u0082\u0003!\u0010\u0000\u0082\u008d"+
		"\u0001\u0000\u0000\u0000\u0083\u0087\u0003#\u0011\u0000\u0084\u0086\t"+
		"\u0000\u0000\u0000\u0085\u0084\u0001\u0000\u0000\u0000\u0086\u0089\u0001"+
		"\u0000\u0000\u0000\u0087\u0088\u0001\u0000\u0000\u0000\u0087\u0085\u0001"+
		"\u0000\u0000\u0000\u0088\u008a\u0001\u0000\u0000\u0000\u0089\u0087\u0001"+
		"\u0000\u0000\u0000\u008a\u008b\u0003#\u0011\u0000\u008b\u008d\u0001\u0000"+
		"\u0000\u0000\u008cz\u0001\u0000\u0000\u0000\u008c\u0083\u0001\u0000\u0000"+
		"\u0000\u008d\u001c\u0001\u0000\u0000\u0000\u008e\u008f\u0005(\u0000\u0000"+
		"\u008f\u001e\u0001\u0000\u0000\u0000\u0090\u0091\u0005)\u0000\u0000\u0091"+
		" \u0001\u0000\u0000\u0000\u0092\u0093\u0005\"\u0000\u0000\u0093\"\u0001"+
		"\u0000\u0000\u0000\u0094\u0095\u0005\'\u0000\u0000\u0095$\u0001\u0000"+
		"\u0000\u0000\u0096\u0097\u0005E\u0000\u0000\u0097&\u0001\u0000\u0000\u0000"+
		"\u0098\u0099\u0005e\u0000\u0000\u0099(\u0001\u0000\u0000\u0000\u009a\u009c"+
		"\u0007\u0000\u0000\u0000\u009b\u009a\u0001\u0000\u0000\u0000\u009c\u009d"+
		"\u0001\u0000\u0000\u0000\u009d\u009b\u0001\u0000\u0000\u0000\u009d\u009e"+
		"\u0001\u0000\u0000\u0000\u009e*\u0001\u0000\u0000\u0000\u009f\u00a1\u0007"+
		"\u0001\u0000\u0000\u00a0\u009f\u0001\u0000\u0000\u0000\u00a1\u00a2\u0001"+
		"\u0000\u0000\u0000\u00a2\u00a0\u0001\u0000\u0000\u0000\u00a2\u00a3\u0001"+
		"\u0000\u0000\u0000\u00a3\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a5\u0006"+
		"\u0015\u0000\u0000\u00a5,\u0001\u0000\u0000\u0000\u00a6\u00a7\u0005+\u0000"+
		"\u0000\u00a7.\u0001\u0000\u0000\u0000\u00a8\u00a9\u0005-\u0000\u0000\u00a9"+
		"0\u0001\u0000\u0000\u0000\u00aa\u00ab\u0005*\u0000\u0000\u00ab2\u0001"+
		"\u0000\u0000\u0000\u00ac\u00ad\u0005/\u0000\u0000\u00ad4\u0001\u0000\u0000"+
		"\u0000\u00ae\u00af\u0005,\u0000\u0000\u00af6\u0001\u0000\u0000\u0000\u00b0"+
		"\u00b1\u0005;\u0000\u0000\u00b18\u0001\u0000\u0000\u0000\u000e\u0000>"+
		"DFPVptx~\u0087\u008c\u009d\u00a2\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}