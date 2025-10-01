package net.cyberneticforge.quickstocks.application.boot;

/**
 * Utility class for word manipulation operations.
 * Provides functionality similar to Apache Commons Lang WordUtils.
 */
public class WordUtils {
    
    /**
     * Capitalizes all the whitespace separated words in a String.
     * Only the first character of each word is changed to uppercase.
     * 
     * @param str the String to capitalize
     * @return capitalized String, null if null String input
     */
    public static String capitalizeFully(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        return capitalizeFully(str, new char[]{' ', '\t', '\n', '\r'});
    }
    
    /**
     * Capitalizes all the delimiter separated words in a String.
     * Only the first character of each word is changed to uppercase.
     * All other characters are changed to lowercase.
     * 
     * @param str the String to capitalize
     * @param delimiters set of characters to determine capitalization
     * @return capitalized String, null if null String input
     */
    public static String capitalizeFully(String str, char[] delimiters) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        str = str.toLowerCase();
        return capitalize(str, delimiters);
    }
    
    /**
     * Capitalizes all the delimiter separated words in a String.
     * Only the first character of each word is changed to uppercase.
     * 
     * @param str the String to capitalize
     * @param delimiters set of characters to determine capitalization
     * @return capitalized String, null if null String input
     */
    private static String capitalize(String str, char[] delimiters) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        
        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toUpperCase(ch);
                capitalizeNext = false;
            }
        }
        
        return new String(buffer);
    }
    
    /**
     * Is the character a delimiter.
     * 
     * @param ch the character to check
     * @param delimiters the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }
}