package uk.co.crunch.platform.utils;

public class StringUtils {

    // FIXME Swiped from Spring's StringUtils
    public static String replace(String inString, String oldPattern, String newPattern) {
        if ( inString.isEmpty() || oldPattern.isEmpty() || newPattern == null) {
            return inString;
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0; // our position in the old string
        int index = inString.indexOf(oldPattern);
        // the index of an occurrence we've found, or -1
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }
        sb.append(inString.substring(pos));
        // remember to append any characters to the right of a match
        return sb.toString();
    }

    public static CharSequence collapseWhitespace(CharSequence toBeStripped) {
        if (toBeStripped == null || toBeStripped.length() < 1) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        boolean lastWasSpace = true;
        int i = 0;
        while (i < toBeStripped.length()) {
            char c = toBeStripped.charAt(i++);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    result.append(' ');
                }
                lastWasSpace = true;
            } else {
                result.append(c);
                lastWasSpace = false;
            }
        }
        return result.toString().trim();
    }
}
