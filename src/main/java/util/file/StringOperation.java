package util.file;

import data.ClientPos;
import data.Node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StringOperation is a utility class for string manipulation.
 */
public class StringOperation {
    /**
     * Extract position information from a string.
     *
     * @param posStr The string containing position information.
     * @return The position information as a Node object.
     */
    public static Node getPosFromStr(String posStr) {

        // Define a pattern for matching decimal numbers
        Pattern p = Pattern.compile("(\\d+\\.\\d+)");
        Matcher m = p.matcher(posStr); // Create a matcher for the input string
        int count = 0;
        double lon = 0.0, lat = 0.0;
        while (m.find()) {
            // group(0) represents the entire match, group(1) represents the first capturing group, and so on.
            if (count == 0) {
                lon = Double.valueOf(m.group());
            } else if (count == 1) {
                lat = Double.valueOf(m.group());
            }
            count++;
        }
        Node node = new Node();
        node.setLongitude(lon);
        node.setLatitude(lat);
        return node;
    }

    public static void main(String[] args) {
        String posStr;
        // Define a pattern for matching decimal numbers
        Pattern p = Pattern.compile("(\\d+\\.\\d+)");
        Matcher m = p.matcher("POINT(111.234567 33.222222)");
        while (m.find()) {
            // If a match is found, retrieve it
            posStr = m.group(0) == null ? "" : m.group(0);
            System.out.println(posStr);
        }
    }
}
