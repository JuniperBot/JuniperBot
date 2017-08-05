package ru.caramel.juniperbot.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ParseUtils {

    private ParseUtils() {}

    /// <summary>
    /// Reads command line arguments from a single string.
    /// </summary>
    /// <param name="argsString">The string that contains the entire command line.</param>
    /// <returns>An array of the parsed arguments.</returns>
    public static String[] readArgs(String argsString) {
        // Collects the split argument strings
        List<String> args = new ArrayList<>();
        // Builds the current argument
        StringBuilder currentArg = new StringBuilder();
        // Indicates whether the last character was a backslash escape character
        boolean escape = false;
        // Indicates whether we're in a quoted range
        boolean inQuote = false;
        // Indicates whether there were quotes in the current arguments
        boolean hadQuote = false;
        // Remembers the previous character
        char prevCh = '\0';
        // Iterate all characters from the input string
        for (int i = 0; i < argsString.length(); i++) {
            char ch = argsString.charAt(i);
            if (ch == '\\' && !escape) {
                // Beginning of a backslash-escape sequence
                escape = true;
            } else if (ch == '\\' && escape) {
                // Double backslash, keep one
                currentArg.append(ch);
                escape = false;
            } else if (ch == '"' && !escape) {
                // Toggle quoted range
                inQuote = !inQuote;
                hadQuote = true;
                if (inQuote && prevCh == '"') {
                    // Doubled quote within a quoted range is like escaping
                    currentArg.append(ch);
                }
            } else if (ch == '"' && escape) {
                // Backslash-escaped quote, keep it
                currentArg.append(ch);
                escape = false;
            } else if (Character.isWhitespace(ch) && !inQuote) {
                if (escape) {
                    // Add pending escape char
                    currentArg.append('\\');
                    escape = false;
                }
                // Accept empty arguments only if they are quoted
                if (currentArg.length() > 0 || hadQuote) {
                    args.add(currentArg.toString());
                }
                // Reset for next argument
                currentArg = new StringBuilder();
                hadQuote = false;
            } else {
                if (escape) {
                    // Add pending escape char
                    currentArg.append('\\');
                    escape = false;
                }
                // Copy character from input, no special meaning
                currentArg.append(ch);
            }
            prevCh = ch;
        }
        // Save last argument
        if (currentArg.length() > 0 || hadQuote) {
            args.add(currentArg.toString());
        }
        return args.toArray(new String[args.size()]);
    }
}
