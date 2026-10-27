package co.edu.unipamplona.logicode.pseint.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Separa el código en palabras, números, textos y operadores. Las palabras se comparan sin mayúsculas ni tildes. */
final class Lexer {

    enum Type { NUMBER, TEXT, WORD, SYMBOL, END_OF_LINE, END }

    /** {@code key}: la palabra normalizada (minúsculas, sin tildes) o el símbolo. */
    record Token(Type type, String text, String key, int line) {

        boolean is(String expected) {
            return (type == Type.WORD || type == Type.SYMBOL) && key.equals(expected);
        }
    }

    private static final List<String> SYMBOLS = List.of("<-", ":=", "<=", ">=", "<>", "!=", "==", "&&", "||",
            "=", "<", ">", "+", "-", "*", "/", "^", "%", "(", ")", "[", "]", ",", ":", "&", "|", "~");
    /** Palabras que siguen a «Fin» cuando se escribe separado («Fin Si»). */
    private static final Set<String> CLOSABLE = Set.of("si", "mientras", "para", "segun", "algoritmo", "proceso",
            "subproceso", "funcion", "subalgoritmo");

    private Lexer() {
    }

    static List<Token> tokens(String code) {
        List<Token> tokens = new ArrayList<>();
        int line = 1;
        int i = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '\n' || c == ';') {
                tokens.add(new Token(Type.END_OF_LINE, "\n", "\n", line));
                if (c == '\n') {
                    line++;
                }
                i++;
            } else if (Character.isWhitespace(c)) {
                i++;
            } else if (code.startsWith("//", i)) {
                while (i < code.length() && code.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '"' || c == '\'' || c == '“' || c == '”') {
                int end = i + 1;
                while (end < code.length() && !isQuote(code.charAt(end), c) && code.charAt(end) != '\n') {
                    end++;
                }
                if (end >= code.length() || code.charAt(end) == '\n') {
                    throw new PseintError(PseintError.Kind.SYNTAX, line, "falta cerrar las comillas del texto.");
                }
                String text = code.substring(i + 1, end);
                tokens.add(new Token(Type.TEXT, text, text, line));
                i = end + 1;
            } else if (Character.isDigit(c)) {
                int end = i;
                while (end < code.length() && Character.isDigit(code.charAt(end))) {
                    end++;
                }
                if (end + 1 < code.length() && code.charAt(end) == '.' && Character.isDigit(code.charAt(end + 1))) {
                    end++;
                    while (end < code.length() && Character.isDigit(code.charAt(end))) {
                        end++;
                    }
                }
                String text = code.substring(i, end);
                tokens.add(new Token(Type.NUMBER, text, text, line));
                i = end;
            } else if (Character.isLetter(c) || c == '_') {
                int end = i;
                while (end < code.length() && (Character.isLetterOrDigit(code.charAt(end)) || code.charAt(end) == '_')) {
                    end++;
                }
                String text = code.substring(i, end);
                String key = normalize(text);
                Token previous = tokens.isEmpty() ? null : tokens.getLast();
                if (previous != null && previous.type() == Type.WORD && previous.key().equals("fin")
                        && CLOSABLE.contains(key)) {
                    tokens.set(tokens.size() - 1, new Token(Type.WORD, "Fin " + text, "fin" + key, previous.line()));
                } else {
                    tokens.add(new Token(Type.WORD, text, key, line));
                }
                i = end;
            } else {
                String symbol = symbolAt(code, i);
                if (symbol == null) {
                    throw new PseintError(PseintError.Kind.SYNTAX, line, "no se reconoce el carácter «" + c + "».");
                }
                String key = symbol.equals("←") ? "<-" : symbol;
                tokens.add(new Token(Type.SYMBOL, symbol, key, line));
                i += symbol.length();
            }
        }
        tokens.add(new Token(Type.END_OF_LINE, "\n", "\n", line));
        tokens.add(new Token(Type.END, "", "", line));
        return tokens;
    }

    /** Minúsculas y sin tildes: «Según» y «SEGUN» son la misma palabra. */
    static String normalize(String word) {
        return Normalizer.normalize(word, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static boolean isQuote(char c, char opening) {
        return opening == '“' || opening == '”' ? c == '”' || c == '“' : c == opening;
    }

    private static String symbolAt(String code, int i) {
        if (code.charAt(i) == '←') {
            return "←";
        }
        for (String symbol : SYMBOLS) {
            if (code.startsWith(symbol, i)) {
                return symbol;
            }
        }
        return null;
    }
}
