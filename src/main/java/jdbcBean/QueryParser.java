/*
 * MIT License
 *
 * Copyright (c) 2022 Le Manh Hieu
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
 */

package jdbcBean;


import jdbcBean.exception.JDBCBeanException;
import lombok.val;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

class QueryParser {
    private static final char paramCharacter = ':';
    private static final Map<String, MappedQuery> cachedParamMap = new Hashtable<>();

    public static MappedQuery parseNPSql(String rawSqlText) {
        return parseNPSql(rawSqlText, true);
    }

    public static MappedQuery parseNPSql(String rawSqlText, boolean useCache) {
        if (useCache && cachedParamMap.containsKey(rawSqlText)) {
            return cachedParamMap.get(rawSqlText);
        }
        
        val translatedQuery = new StringBuilder();
        int paramIndex = 1;
        val paramsMap = new HashMap<String, Integer>();

        int i = 0;

        while (i < rawSqlText.length()) {
            if (rawSqlText.charAt(i) == '?') {
                throw new JDBCBeanException("Don't use JDBC's parameter character '?' inside a named parameter query");
            } else if (rawSqlText.charAt(i) == paramCharacter) {
                if (i + 1 < rawSqlText.length() && Character.isJavaIdentifierStart(rawSqlText.charAt(i + 1))) {
                    i++;

                    StringBuilder paramName = new StringBuilder();
                    paramName.append(rawSqlText.charAt(i));

                    //  loop until there is no next char or next char is not a java identifier char
                    while (i + 2 <= rawSqlText.length() && Character.isJavaIdentifierPart(rawSqlText.charAt(i + 1))) {
                        i++;
                        paramName.append(rawSqlText.charAt(i));
                    }
                    paramsMap.put(paramName.toString(), paramIndex);
                    paramIndex++;
                    i++;
                    translatedQuery.append('?');
                } else throw new JDBCBeanException("Dangling param starter");
            } else {
                translatedQuery.append(rawSqlText.charAt(i));
                i++;
            }
        }

        MappedQuery mappedQuery = new MappedQuery(translatedQuery.toString(), paramsMap);
        if (useCache) {
            cachedParamMap.put(rawSqlText, mappedQuery);
        }
        return mappedQuery;
    }
}
