package ag.ndn.ndnoverwifidirect.videosharing.util;

/**
 * Class exposing methods to sanitize potential input
 * to NDN methods, including those implemented in NDNOverWifiDirect.
 *
 * Created by allengong on 9/2/16.
 */
public class NDNSanitizer {

    /**
     * Returns a new string that can be assumed safe to use in any appropriate NDN
     * method. The input string is left unmodified.
     *
     * @param str the string to sanitize
     * @return a new string with invalid characters replaced with valid ones
     */
    public static String sanitizeName(String str) {

        /**
         * Replace:
         *
         * - leading and trailing whitespace with empty space (remove them)
         * - convert all internal whitespace to hypens
         *
         */
        String sanitized = str.trim();
        return sanitized.replaceAll("[_.\\s+]", "-");
    }
}
