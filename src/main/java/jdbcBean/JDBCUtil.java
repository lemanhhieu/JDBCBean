/*
 * MIT License
 *
 * Copyright (c) 2022 LE MANH HIEU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
