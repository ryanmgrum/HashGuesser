package hashguesser;

import com.mifmif.common.regex.Generex;
import java.util.ArrayList;
import java.util.Random;

/**@author Ryan McAllister-Grum
 */
class RegexParser {
    private final Generex stringGenerator;
    private final ArrayList<ArrayList<Character>> parsedRegex;
    
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
    private static final char[] symbols = new char[]{'!', '@', '#', '%', '&', '/', '-', '_', '|', ' ', '"', ';', ':', '<', '>', '`', '~'};
    private static final char[] numbers = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    
    
    private boolean parseRegex(String expression) {
        parsedRegex.clear();
        int state = 0, priorState = 0; // 0 = literal, 1 = parenthesis, 2 = bracket, 3 = braces, 4 = range, 5 = multiDigit
        for (int i = 0; i < expression.length(); i++) {
            Character c = expression.charAt(i);
            switch(c) {
                case '(' -> {
                    priorState = state;
                    state = 1;
                }
                case '[' -> {
                    priorState = state;
                    state = 2;
                }
                case '{' -> {
                    priorState = state;
                    state = 3;
                }
            }
        }
        return true;
    }
    
    
    RegexParser(String newRegex) {
        parsedRegex = new ArrayList<>();
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

}