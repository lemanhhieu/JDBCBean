package jdbcBean;

import jdbcBean.exception.JDBCBeanException;
import org.jetbrains.annotations.NotNull;

class JDBCUtil {

    /**
     * Convert a camel-case name to all lower-case, snake-case string.
     */
    public static String convertCamelCaseToSnakeCase(@NotNull String input) {
        // lower case char to Upper / number
        // upper case char to upper & lower
        // lower/upper case to number
        // number to upper case

        StringBuilder output = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            boolean curCharDigit = Character.isDigit(input.charAt(i));
            boolean curCharLowerCase = Character.isLowerCase(input.charAt(i));
            boolean curCharUpperCase = Character.isUpperCase(input.charAt(i));

            boolean nextCharUpperCase = i + 1 < input.length() && Character.isUpperCase(input.charAt(i+1));
            boolean nextCharDigit = i + 1 < input.length() && Character.isDigit(input.charAt(i+1));

            boolean nextNextCharLowerCase = i + 2 < input.length() && Character.isLowerCase(input.charAt(i+2));

            if (
                (curCharLowerCase && (nextCharUpperCase || nextCharDigit))
                || ((curCharLowerCase || curCharUpperCase) && nextCharDigit)
                || (curCharDigit && nextCharUpperCase)
                || (curCharUpperCase && nextCharUpperCase && nextNextCharLowerCase)
            ) {
                output.append(Character.toLowerCase(input.charAt(i)));
                output.append('_');
            }
            else {
                output.append(Character.toLowerCase(input.charAt(i)));
            }
        }
        return output.toString();
    }

    public static void assertTrue(boolean cond, @NotNull String msg) {
        if (!cond) {
            throw new JDBCBeanException(String.format("Assertion failed: %s", msg));
        }
    }

}
