package hashguesser;

import com.mifmif.common.regex.Generex;
import com.mifmif.common.regex.util.Iterator;
import java.util.Random;

/**@author Ryan McAllister-Grum
 */
class RegexParser {
    private final Generex stringGenerator;
    
    RegexParser(String newRegex) {
        stringGenerator = new Generex(newRegex, new Random());
    }
    
    public String getCandidate(boolean random) {
        if (random)
            return stringGenerator.random();
        else
            return stringGenerator.getFirstMatch();
    }
    
    public Iterator iterator() {
        return stringGenerator.iterator();
    }
}