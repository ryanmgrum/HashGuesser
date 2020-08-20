package hashguesser;

import com.mifmif.common.regex.Generex;
import java.math.BigInteger;
import java.util.regex.PatternSyntaxException;
import static java.math.BigInteger.ZERO;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

/**@author Ryan McAllister-Grum
 */
class RegexParser {
    private final Generex stringGenerator;
    private final LinkedList<LinkedList<RegexChunk>> parsedRegex;
    private final Stack<Character> openSymbols;
    
    /** First define the syntax for parsing a (limited) regular expression for generating candidates:
     *  <program> -> <expression>
     *  <expression> -> <parenthesisExpression> | <bracketExpression> | <literalExpression>
     *  <parenthesisExpression> -> ( <expression> ) | ( <expression> ) <bracesExpression>
     *  <bracketExpression> -> [ <literalExpression> | <rangeExpression> ] | [ <literalExpression> | <rangeExpression> ] <bracesExpression>
     *  <literalExpression> -> <lowerCaseLetter><literalExpression> | <upperCaseLetter><literalExpression> | <symbol><literalExpression> | <escapedExpression><literalExpression> | <number><literalExpression> | <lowerCaseLetter> | <upperCaseLetter> | <symbol> | <escapedExpression> | <number>
     *  <lowerCaseLetter> -> a | b | c | d | e | f | g | h | i | j | k | l | m | n | o | p | q | r | s | t | u | v | w | x | y | z
     *  <upperCaseLetter> -> A | B | C | D | E | F | G | H | I | J | K | L | M | N | O | P | Q | R | S | T | U | V | W | X | Y | Z
     *  <symbol> -> ! | @ | # | % | & | * | / | - | _ | '|' | ' ' | " | ' | ; | : | < | > | ` | ~
     *  <escapedExpression> -> (not needed right now, but involves \\, \^, and so on)
     *  <number> -> 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
     *  <specialExpression> -> (not needed right now, but involves $, ^, *, ., ?, and so on)
     *  <rangeExpression> -> <lowerCaseLetter>-<lowerCaseLetter> | <upperCaseLetter>-<upperCaseLetter> | <number>-<number>
     *  <bracesExpression> -> { <number> } | { <multiDigitNumber> } | { <number>,<number> } | { <number>,<multiDigitNumber> } | { <multiDigitNumber>,<multiDigitNumber> }
     *  <multiDigitNumber> -> <number><multiDigitNumber> | <number>
     */
    private static final char[] lowercaseLetters = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private static final char[] uppercaseLetters = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    // I do not think these symbols are in order, need to verify.
    private static final char[] symbols = new char[]{'!', '@', '#', '%', '&', '/', '-', '_', '|', ' ', '"', ';', ':', '<', '>', '`', '~'};
    private static final char[] numbers = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    
    
    private void parseRegex(String expression) throws PatternSyntaxException {
        parsedRegex.clear();
        int state = 0, priorState = 0; // 0 = literal, 1 = parenthesis, 2 = bracket, 3 = braces, 4 = range, 5 = multiDigit
        for (Integer i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            switch(c) {
                case '(' -> {
                    openSymbols.push(expression.charAt(i));
                    LinkedList<RegexChunk> list = new LinkedList<>();
                    parenthesis(++i, expression, list);
                }
                case '[' -> {
                    priorState = state;
                    state = 2;
                }
                case '{' -> {
                    priorState = state;
                    state = 3;
                }
                default -> {
                    LinkedList<RegexChunk> list = new LinkedList<>();
                    RegexChunk chk = new RegexChunk(expression.charAt(i));
                    list.add(chk);
                    parsedRegex.add(list);
                }
            }
        }
    }
    
    
    private void parenthesis(Integer i, String expression, LinkedList<RegexChunk> list) throws PatternSyntaxException {
        RegexChunk chunk = new RegexChunk();
        for (; i < expression.length(); i++) {
            char c = expression.charAt(i);
            switch(c) {
                case '|' -> {
                    list.add(chunk);
                    chunk = new RegexChunk();
                }
                case '}' -> throw new PatternSyntaxException("Error parsing regex: unescaped closing brace found inside parenthesis.", expression, i);
                case ']' -> throw new PatternSyntaxException("Error parsing regex: unescaped closing bracket found inside parenthesis.", expression, i);
                case '[' -> {
                    openSymbols.push(expression.charAt(i));
                    bracket(++i, expression, list);
                }
                case '{' -> {
                    openSymbols.push(expression.charAt(i));
                    brace(++i, expression, list);
                }
            }
        }
    }
    
    // This is where ranges come into play.
    private void bracket(Integer i, String expression, LinkedList<RegexChunk> list) throws PatternSyntaxException {
        RegexChunk result = new RegexChunk();
        boolean popped = false;
        for(; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Arrays.binarySearch(numbers, c) >= 0) { // Is it a number?
                if (i+1 < expression.length() && i+2 < expression.length()) {
                    if (expression.charAt(i+1) == '-') { // Check whether it is a range of numbers.
                        char upper = expression.charAt(i+2);
                        if (Arrays.binarySearch(numbers, upper) >= 0) { // Check whether the other end of the range is also a number.
                            if (upper > c)
                                for (; c <= upper; c++)
                                    result.addCharacter(c);
                            else
                                for (; c >= upper; c--)
                                    result.addCharacter(c);
                            i = i + 2;
                        } else
                            throw new PatternSyntaxException("Error parsing regex: unknown range (numbers, lowercase letters, or uppercase letters).", expression, i);
                    } else // Not a sequence, add the number.
                        result.addCharacter(c);
                } else if (i+1 < expression.length()) // Last number in bracket or just a single character, add.
                    result.addCharacter(c);
                else // we are at the end of the expression inside a bracket, error.
                    throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);                    
            } else if (Arrays.binarySearch(lowercaseLetters, c) >= 0) { // Is it a lowercase letter?
                if (i+1 < expression.length() && i+2 < expression.length()) {
                    if (expression.charAt(i+1) == '-') { // Check whether it is a range of letters.
                        char upper = expression.charAt(i+2);
                        if (Arrays.binarySearch(lowercaseLetters, upper) >= 0) { // Check whether the other end of the range is also a letter.
                            if (upper > c)
                                for (; c <= upper; c++)
                                    result.addCharacter(c);
                            else
                                for (; c >= upper; c--)
                                    result.addCharacter(c);
                            i = i + 2;
                        } else
                            throw new PatternSyntaxException("Error parsing regex: unknown range (numbers, lowercase letters, or uppercase letters).", expression, i);
                    } else // Not a sequence, add the letter.
                        result.addCharacter(c);
                } else if (i+1 < expression.length()) // Last letter in bracket or just a single character, add.
                    result.addCharacter(c);
                else // we are at the end of the expression inside a bracket, error.
                    throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);                    
            } else if (Arrays.binarySearch(uppercaseLetters, c) >= 0) { // Is it an uppercase letter?
                if (i+1 < expression.length() && i+2 < expression.length()) {
                    if (expression.charAt(i+1) == '-') { // Check whether it is a range of letters.
                        char upper = expression.charAt(i+2);
                        if (Arrays.binarySearch(uppercaseLetters, upper) >= 0) { // Check whether the other end of the range is also a letter.
                            if (upper > c)
                                for (; c <= upper; c++)
                                    result.addCharacter(c);
                            else
                                for (; c >= upper; c--)
                                    result.addCharacter(c);
                            i = i + 2;
                        } else
                            throw new PatternSyntaxException("Error parsing regex: unknown range (numbers, lowercase letters, or uppercase letters).", expression, i);
                    } else // Not a sequence, add the letter.
                        result.addCharacter(c);
                } else if (i+1 < expression.length()) // Last letter in bracket or just a single character, add.
                    result.addCharacter(c);
                else // we are at the end of the expression inside a bracket, error.
                    throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);                    
            } else if (Arrays.binarySearch(symbols, c) >= 0) { // Is it a symbol?
                if (c == ']') { // Exit
                    if (openSymbols.peek() == '[') {
                        openSymbols.pop();
                        popped = true;
                        i += 1;
                        break;
                    } else
                        throw new PatternSyntaxException("Error parsing regex: no matching opening bracket found.", expression, i);
                } else // Arbitrary symbol, add it to parsed.
                    result.addCharacter(c);
            }
        }
        if (!popped)
            throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);
        else
            list.add(result);
    }
    
    
    private void brace(Integer i, String expression, LinkedList<RegexChunk> list) throws PatternSyntaxException {
        
    }
    
    
    RegexParser(String newRegex) throws PatternSyntaxException {
        parsedRegex = new LinkedList<>();
        openSymbols = new Stack<>();
        stringGenerator = new Generex(newRegex, new Random());
    }
    
    
    public String getCandidate(boolean random) {
        if (random)
            return stringGenerator.random();
        else
            return stringGenerator.getFirstMatch();
    }
    
    public com.mifmif.common.regex.util.Iterator iterator() {
        return stringGenerator.iterator();
    }

    //@Override
    //public java.util.Iterator iterator() {
    //    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    //}

    private class RegexChunk {
        private char letter;
        private BigInteger minimumNumberToFetch;
        private BigInteger maximumNumberToFetch;
        private final LinkedList<Character> sequence;
        
        protected RegexChunk() {
            sequence = new LinkedList<>();
            minimumNumberToFetch = ZERO;
            maximumNumberToFetch = ZERO;
        }
        
        protected RegexChunk(Character ch) {
            letter = ch;
            sequence = null;
            minimumNumberToFetch = ZERO;
            maximumNumberToFetch = ZERO;
        }
        
        protected RegexChunk(LinkedList<Character> characters) {
            sequence = characters;
            minimumNumberToFetch = ZERO;
            maximumNumberToFetch = ZERO;
        }
        
        protected void setNumberToFetch(BigInteger min) {
            if (min.compareTo(ZERO) > 0)
                minimumNumberToFetch = min;
        }
        
        protected void setNumberToFetch(BigInteger min, BigInteger max) {
            if (min.compareTo(ZERO) > 0)
                if (max.compareTo(min) > 0) {
                    minimumNumberToFetch = min;
                    maximumNumberToFetch = max;
                }
        }
        
        protected void addCharacter(char c) {
            sequence.add(c);
        }
        
        protected void addSequence(LinkedList<Character> chars) {
            for (Character chara : chars)
                sequence.add(chara);
        }
    }
}