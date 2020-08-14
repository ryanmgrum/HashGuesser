package hashguesser;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import com.mifmif.common.regex.util.Iterator;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** @author Ryan McAllister-Grum
 */
class HashGuesser implements Runnable {
    private final RegexParser parser;
    private static final Object o = new Object();
    private final String name;
    volatile String guess;
    private final boolean random;
    private MessageDigest msg;
    private final byte[] hash;
    private byte[] currentHash;
    private BigInteger hashCount;
    private static volatile boolean pause;
    private static volatile boolean stop;
    private volatile BigInteger lastHashCount;
    private static long updateInterval;
    private volatile long nextUpdate;
    private static JTable table;
    private static JLabel statusBar;
    
    HashGuesser(String newName, String newHash, String regex, String hashAlgo, boolean isRandom, JTable newTable, JLabel newStatusBar) {
        try {
            msg = MessageDigest.getInstance(hashAlgo);
        } catch(NoSuchAlgorithmException ex) {
            showMessageDialog(null, String.format("Error creating thread with %s message digest!\n%s", hashAlgo, ex.getMessage()), "Error!", ERROR_MESSAGE);
            Thread.currentThread().interrupt();
        }
        parser = new RegexParser(regex);
        name = newName;
        guess = "";
        random = isRandom;
        hash = parseHex(newHash);
        hashCount = ZERO;
        lastHashCount = ZERO;
        table = newTable;
        statusBar = newStatusBar;
        nextUpdate = System.currentTimeMillis() + updateInterval;
        pause = false;
        stop = false;
        ((DefaultTableModel) table.getModel()).addRow(new Object[]{name, guess, "", ""});
        
    }
    
    private byte[] parseHex(String hash) {
        byte[] a = new BigInteger(hash, 16).toByteArray();
        if (a.length != hash.length() / 2)
            a = Arrays.copyOfRange(a, 1, a.length);
        return a;
    }
    
    public static void pause() {pause = true;}
    public static void unpause() {
        pause = false;
        synchronized(o) {
            o.notifyAll();
        }
    }
    public static void stop() {stop = true;}
    public static void setUpdateInterval(long interval) {
        if (interval >= 0)
            updateInterval = interval;
    }
    public BigInteger getHashCount() {return hashCount;}
    public BigInteger getHashCountPerSecond() {
        nextUpdate = System.currentTimeMillis() + updateInterval;
        return lastHashCount;
    }
    public void resetLastHashCount() {lastHashCount = ZERO;}


    private void publish(String guess) {
        table.getModel().setValueAt(guess, Integer.parseInt(name)-1, 1);
        table.getModel().setValueAt(String.format("%,d", getHashCountPerSecond()), Integer.parseInt(name)-1, 2);
        resetLastHashCount();
        table.getModel().setValueAt(String.format("%,d", getHashCount()), Integer.parseInt(name)-1, 3);
    }

    @Override
    public void run() {
        boolean match = false;
        Iterator iter = parser.iterator();
        try {
            while (!match && !Thread.interrupted()) {
                if (stop)
                    Thread.currentThread().interrupt();
                else if (!pause) {
                    if (random)
                        guess = parser.getCandidate(random);
                    else
                        guess = iter.next();
                    currentHash = msg.digest(guess.getBytes());
                    for (int i = 0; i < currentHash.length; i++)
                        if (hash[i] != currentHash[i])
                            break;
                        else if (i == currentHash.length-1)
                            match = true;
                    hashCount = hashCount.add(ONE);
                    lastHashCount = lastHashCount.add(ONE);
                    if (System.currentTimeMillis() > nextUpdate)
                        publish(guess);
                } else
                    while(pause)
                        synchronized(o) {
                            o.wait();
                        }
            }
        } catch (InterruptedException e) {
            // Fine.
        }
        if (match) {
            stop();
            publish(guess);
            statusBar.setText(String.format("Plaintext found! %s", guess));
        }
    }
}