package hashguesser;

import com.mifmif.common.regex.Generex;
import java.math.BigInteger;
import java.util.regex.PatternSyntaxException;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.TEN;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;

/**@author Ryan McAllister-Grum
 */
class RegexParser {
    private final Generex stringGenerator;
    private final RegexChunk parsedRegex; // The head of the linked list of parsed regex.
    
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
    private static final char[] symbols = new char[]{' ', '!', '"', '#', '%', '&', ',', '-', '/', ':', ';', '<', '>', '@', '[', ']', '_', '`', '{', '|', '}', '~'};
    private static final char[] numbers = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    
    
    private void parseRegex(String expression) throws PatternSyntaxException {
        parsedRegex.reset();
        RegexChunk chunk = new RegexChunk();
        parsedRegex.setNext(chunk);
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            switch(c) {
                case '(' -> {
                    if (chunk.getLetter() == null && chunk.getSequence() == null && chunk.getPeers() == null) {
                        chunk = parenthesis(++i, expression);
                    } else {
                        RegexChunk next = parenthesis(++i, expression);
                        chunk.setNext(next);
                        next.setPrev(chunk);
                        chunk = chunk.next();
                    }
                    i = expression.indexOf(")", i);
                }
                case '[' -> {
                    if (chunk.getLetter() == null && chunk.getSequence() == null && chunk.getPeers() == null) {
                        chunk.addSequence(bracket(++i, expression));
                        i = expression.indexOf("]", i);
                    } else {
                        RegexChunk prev = chunk;
                        chunk.setNext(new RegexChunk());
                        chunk = chunk.next();
                        chunk.setPrev(prev);
                        i = expression.indexOf("]", i);
                    }
                }
                case '{' -> {
                    if (chunk.getLetter() == null && chunk.getSequence() == null && chunk.getPeers() == null)
                        throw new PatternSyntaxException("Error parsing regex: braces on an empty expression.", expression, i);
                    brace(++i, expression, chunk);
                    i = expression.indexOf("}", i);
                }
                default -> {
                    if (chunk.getLetter() == null && chunk.getSequence() == null && chunk.getPeers() == null)
                        chunk.setLetter(expression.charAt(i));
                    else {
                        RegexChunk prev = chunk;
                        chunk.setNext(new RegexChunk(expression.charAt(i)));
                        chunk = chunk.next();
                        chunk.setPrev(prev);
                    }
                }
            }
        }
        // Rewind chunk
        while (chunk.hasPrev())
            chunk = chunk.prev();
        // Set parsedRegex again in case its chunk got lost.
        parsedRegex.setNext(chunk);
    }
    
    
    private RegexChunk parenthesis(int i, String expression) throws PatternSyntaxException {
        RegexChunk result = new RegexChunk();
        RegexChunk chunk = new RegexChunk();
        result.addPeer(chunk);
        boolean popped = false;
        for (; i < expression.length() && !popped; i++) {
            char c = expression.charAt(i);
            switch(c) {
                case '|' -> {
                    RegexChunk prev = chunk;
                    chunk.setNext(new RegexChunk());
                    chunk = chunk.next();
                    chunk.setPrev(prev);
                }
                case '}' -> throw new PatternSyntaxException("Error parsing regex: unescaped closing brace found inside parenthesis.", expression, i);
                case ']' -> throw new PatternSyntaxException("Error parsing regex: unescaped closing bracket found inside parenthesis.", expression, i);
                case '[' -> {
                    if (chunk.getLetter() == null && chunk.getSequence() == null && chunk.getPeers() == null) {
                        chunk.addSequence(bracket(++i, expression));
                        i = expression.indexOf("]", i);
                    } else {
                        chunk.addPeer(new RegexChunk(bracket(++i, expression)));
                        i = expression.indexOf("]", i);
                    }
                }
                case '{' -> {
                    if (chunk.getPeers() != null)
                        if (!chunk.getPeers().isEmpty())
                            brace(++i, expression, chunk.getPeers().peekLast());
                    else
                        brace(++i, expression, chunk);
                    i = expression.indexOf("}", i);
                }
                case ')' -> popped = true;
                default -> {
                    if (chunk.getLetter() == null && chunk.getSequence() == null)
                        chunk.setLetter(expression.charAt(i));
                    else
                        chunk.addPeer(new RegexChunk(expression.charAt(i)));
                }
            }
        }
        if (!popped)
            throw new PatternSyntaxException("Error parsing regex: no closing parenthesis found.", expression, i);
        else
            return result;
    }
    
    // This is where ranges come into play.
    private TreeSet<Character> bracket(int i, String expression) throws PatternSyntaxException {
        TreeSet<Character> result = new TreeSet<>();
        boolean popped = false;
        for(; i < expression.length() && !popped; i++) {
            char c = expression.charAt(i);
            if (Arrays.binarySearch(numbers, c) >= 0) { // Is it a number?
                if (i+1 < expression.length() && i+2 < expression.length()) {
                    if (expression.charAt(i+1) == '-') { // Check whether it is a range of numbers.
                        char upper = expression.charAt(i+2);
                        if (Arrays.binarySearch(numbers, upper) >= 0) { // Check whether the other end of the range is also a number.
                            if (upper > c)
                                for (; c <= upper; c++)
                                    result.add(c);
                            else
                                for (; c >= upper; c--)
                                    result.add(c);
                            i += 2;
                        } else
                            throw new PatternSyntaxException("Error parsing regex: unknown range (numbers, lowercase letters, or uppercase letters).", expression, i);
                    } else // Not a sequence, add the number.
                        result.add(c);
                } else if (i+1 < expression.length()) // Last number in bracket or just a single character, add.
                    result.add(c);
                else // we are at the end of the expression inside a bracket, error.
                    throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);                    
            } else if (Arrays.binarySearch(lowercaseLetters, c) >= 0) { // Is it a lowercase letter?
                if (i+1 < expression.length() && i+2 < expression.length()) {
                    if (expression.charAt(i+1) == '-') { // Check whether it is a range of letters.
                        char upper = expression.charAt(i+2);
                        if (Arrays.binarySearch(lowercaseLetters, upper) >= 0) { // Check whether the other end of the range is also a letter.
                            if (upper > c)
                                for (; c <= upper; c++)
                                    result.add(c);
                            else
                                for (; c >= upper; c--)
                                    result.add(c);
                            i += 2;
                        } else
                            throw new PatternSyntaxException("Error parsing regex: unknown range (numbers, lowercase letters, or uppercase letters).", expression, i);
                    } else // Not a sequence, add the letter.
                        result.add(c);
                } else if (i+1 < expression.length()) // Last letter in bracket or just a single character, add.
                    result.add(c);
                else // we are at the end of the expression inside a bracket, error.
                    throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);                    
            } else if (Arrays.binarySearch(uppercaseLetters, c) >= 0) { // Is it an uppercase letter?
                if (i+1 < expression.length() && i+2 < expression.length()) {
                    if (expression.charAt(i+1) == '-') { // Check whether it is a range of letters.
                        char upper = expression.charAt(i+2);
                        if (Arrays.binarySearch(uppercaseLetters, upper) >= 0) { // Check whether the other end of the range is also a letter.
                            if (upper > c)
                                for (; c <= upper; c++)
                                    result.add(c);
                            else
                                for (; c >= upper; c--)
                                    result.add(c);
                            i += 2;
                        } else
                            throw new PatternSyntaxException("Error parsing regex: unknown range (numbers, lowercase letters, or uppercase letters).", expression, i);
                    } else // Not a sequence, add the letter.
                        result.add(c);
                } else if (i+1 < expression.length()) // Last letter in bracket or just a single character, add.
                    result.add(c);
                else // we are at the end of the expression inside a bracket, error.
                    throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);                    
            } else if (Arrays.binarySearch(symbols, c) >= 0) { // Is it a symbol?
                if (c == ']') // Exit
                    popped = true;
                else
                    throw new PatternSyntaxException("Error parsing regex: no matching opening bracket found.", expression, i);
            } else // Arbitrary symbol, add it to parsed.
                result.add(c);
        }
        if (!popped)
            throw new PatternSyntaxException("Error parsing regex: no closing bracket found.", expression, i);
        else
            return result;
    }
    
    
    private void brace(int i, String expression, RegexChunk chunk) throws PatternSyntaxException {
        BigInteger min = ZERO, max = ZERO;
        boolean popped = false, toMax = false;
        for (; i < expression.length() && !popped; i++) {
            char c = expression.charAt(i);
            if (Arrays.binarySearch(numbers, c) > 0) // Is it a number?
                if (!toMax)
                    min = min.multiply(TEN).add(new BigInteger(expression.substring(i, i+1)));
                else
                    max = max.multiply(TEN).add(new BigInteger(expression.substring(i, i+1)));
            else if (Arrays.binarySearch(symbols, c) > 0) { // Is it a symbol?
                switch(expression.charAt(i)) {
                    case '}' -> popped = true;
                    case ',' -> toMax = true;
                    default -> throw new PatternSyntaxException("Error while parsing regex: Unexpected symbol inside braces.", expression, i);
                }
            }
            else // Unknown, throw error.
                throw new PatternSyntaxException("Error while parsing regex: Unexpected symbol inside braces.", expression, i);
        }
        if (!popped)
            throw new PatternSyntaxException("Error while parsing regex: Reached end of parse inside braces.", expression, i);
        else
            if (max.equals(ZERO)) // Only found one number, not two separated by a comma.
                chunk.setMinimumNumberToFetch(min);
            else
                chunk.setMinMaxNumberToFetch(min, max);
    }
    
    
    RegexParser(String newRegex) throws PatternSyntaxException {
        parsedRegex = null; // Will use it after I implement outputting a random and in-order lexicographical sequence.
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
        private Character letter;
        private BigInteger minimumNumberToFetch;
        private BigInteger maximumNumberToFetch;
        private TreeSet<Character> sequence;
        private RegexChunk next;
        private RegexChunk prev;
        private LinkedList<RegexChunk> peers;
        
        protected RegexChunk() {
            letter = null;
            sequence = null;
            next = null;
            prev = null;
            peers = null;
            minimumNumberToFetch = null;
            maximumNumberToFetch = null;
        }
        
        protected RegexChunk(Character ch) {
            letter = ch;
            sequence = null;
            next = null;
            prev = null;
            peers = null;
            minimumNumberToFetch = null;
            maximumNumberToFetch = null;
        }
        
        protected RegexChunk(RegexChunk nextChunk) {
            letter = null;
            sequence = null;
            next = nextChunk;
            prev = null;
            peers = null;
            minimumNumberToFetch = null;
            maximumNumberToFetch = null;
        }
        
        protected RegexChunk(LinkedList peer) {
            letter = null;
            sequence = null;
            next = null;
            prev = null;
            peers = peer;
            minimumNumberToFetch = null;
            maximumNumberToFetch = null;
        }
        
        protected RegexChunk(TreeSet newSequence) {
            letter = null;
            sequence = newSequence;
            next = null;
            prev = null;
            peers = null;
            minimumNumberToFetch = null;
            maximumNumberToFetch = null;
        }
        
        protected void setMinimumNumberToFetch(BigInteger min) {
            minimumNumberToFetch = min;
        }
        
        protected void setMaximumNumberToFetch(BigInteger max) {
            maximumNumberToFetch = max;
        }
        
        protected void setMinMaxNumberToFetch(BigInteger min, BigInteger max) {
            if (min != null && max != null)
                if (min.compareTo(ZERO) > 0 && max.compareTo(min) > 0) {
                    minimumNumberToFetch = min;
                    maximumNumberToFetch = max;
                }
        }
        
        protected BigInteger getMinNumberToFetch() {
            return minimumNumberToFetch;
        }
        
        protected BigInteger getMaxNumberToFetch() {
            return maximumNumberToFetch;
        }
        
        protected void setNext(RegexChunk nextChunk) {
            next = nextChunk;
        }
        
        protected boolean hasNext() {
            return next != null;
        }
        
        protected RegexChunk next() {
            return next;
        }
        
        protected void setPrev(RegexChunk prevChunk) {
            prev = prevChunk;
        }
        
        protected boolean hasPrev() {
            return prev != null;
        }
        
        protected RegexChunk prev() {
            return prev;
        }
        
        protected void addToSequence(char c) {
            if (sequence == null)
                sequence = new TreeSet<>();
            sequence.add(c);
        }
        
        protected Character getLetter() {
            return letter;
        }
        
        protected void setLetter(char newLetter) {
            letter = newLetter;
        }
        
        protected void addSequence(TreeSet<Character> chars) {
            if (sequence == null)
                sequence = new TreeSet<>();
            for (Character chara : chars)
                sequence.add(chara);
        }
        
        protected TreeSet<Character> getSequence() {
            return sequence;
        }
        
        protected void addPeer(RegexChunk peer) {
            if (peers == null)
                peers = new LinkedList<>();
            peers.add(peer);
        }
        
        protected LinkedList<RegexChunk> getPeers() {
            return peers;
        }
        
        protected void reset() {
            next = null;
            prev = null;
            if (peers != null)
                peers.clear();
            peers = null;
            if (sequence != null)
                sequence.clear();
            sequence = null;
            minimumNumberToFetch = null;
            maximumNumberToFetch = null;
        }
    }
    
    
    public static void main(String[] args) {
        try {
            RegexParser parser = new RegexParser("([1-9]|1[0-9]|2[0-5])-(ab[c-f]{3,4}|de[j-m]{2}){6}");
            System.out.println("Done!");
        } catch (PatternSyntaxException e) {
            System.err.println(e);
        }
    }
}