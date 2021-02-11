

import java_cup.runtime.*;
import java.io.IOException;import java.util.Map;

%%


%class Lexer
%public
%unicode
%line
%column
%cup

%{
        private Symbol symbol(int type) {
            return new Symbol(type, yyline, yycolumn);
        }

        private Symbol symbol(int type, Object value) {
            return new Symbol(type, yyline, yycolumn, value);
        }

//        private void reportError(String message) {
//                    throw new RuntimeException("Error at line: " + (yyline+1) + ", column: " + (yycolumn+1) + " -> " + message);
//                }

        private Map<String, Integer> operators = Map.ofEntries(
                    Map.entry("+", sym.PLUS),
                    Map.entry("-", sym.MINUS),
                    Map.entry("/", sym.DIVIDE),
                    Map.entry("*", sym.TIMES),
                    Map.entry("%", sym.MOD),
                    Map.entry("(", sym.OPEN),
                    Map.entry(")", sym.CLOSE),
                    Map.entry("=", sym.E),
                    Map.entry("!=", sym.NE),
                    Map.entry("<", sym.L),
                    Map.entry(">", sym.G),
                    Map.entry("<=", sym.LE),
                    Map.entry(">=", sym.GE),
                    Map.entry(":=", sym.ASSIGN),
                    Map.entry(",", sym.COMMA),
                    Map.entry(";", sym.SEMICOLON),
                    Map.entry(":", sym.COLON)
                );

        private Map<String, Integer> keywords = Map.ofEntries(
                    Map.entry("DECLARE", sym.DECLARE),
                    Map.entry("BEGIN", sym.BEGIN),
                    Map.entry("END", sym.END),
                    Map.entry("IF", sym.IF),
                    Map.entry("THEN", sym.THEN),
                    Map.entry("ELSE", sym.ELSE),
                    Map.entry("ENDIF", sym.ENDIF),
                    Map.entry("WHILE", sym.WHILE),
                    Map.entry("DO", sym.DO),
                    Map.entry("ENDWHILE", sym.ENDWHILE),
                    Map.entry("REPEAT", sym.REPEAT),
                    Map.entry("UNTIL", sym.UNTIL),
                    Map.entry("FOR", sym.FOR),
                    Map.entry("FROM", sym.FROM),
                    Map.entry("TO", sym.TO),
                    Map.entry("DOWNTO", sym.DOWNTO),
                    Map.entry("ENDFOR", sym.ENDFOR),
                    Map.entry("READ", sym.READ),
                    Map.entry("WRITE", sym.WRITE)
                );


%}

WHITE_SPACE = [\ \t\b\012\f\n\r]
NUMBER = [0-9]+
OPERATORS = \+|-|\*|\/|%|\(|\)|\=|\!=|<|>|<=|>=|:=|,|;|:
KEYWORDS = DECLARE|BEGIN|END|IF|THEN|ELSE|ENDIF|WHILE|DO|ENDWHILE|REPEAT|UNTIL|FOR|FROM|TO|DOWNTO|ENDFOR|READ|WRITE
PID = [_a-z]+

%%

{WHITE_SPACE}+                    {}

\[[^\]]*\]                        {}

{OPERATORS}                       { return symbol(operators.get(yytext())); }

{KEYWORDS}                        { return symbol(keywords.get(yytext())); }

{NUMBER}                          { return symbol(sym.NUMBER, Long.parseLong(yytext())); }

{PID}                             { return symbol(sym.PID, yytext()); }

.                                 { throw new IOException("Unrecognized character at line: " + (yyline+1) + ", column: " + (yycolumn+1));} //report_error("Unrecognized character"); }

