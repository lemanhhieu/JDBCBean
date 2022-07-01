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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryParserTest {

    @RepeatedTest(2)
    void happyDays() {
        String npSql = """
            SELECT * FROM table_x WHERE column_1 = :param1 AND column_2 = :param2;
            INSERT INTO table_x VALUES (:param3, :param4)
            """;
        MappedQuery mappedQuery = QueryParser.parseNPSql(npSql);
        Assertions.assertEquals("""
            SELECT * FROM table_x WHERE column_1 = ? AND column_2 = ?;
            INSERT INTO table_x VALUES (?, ?)
            """, mappedQuery.getTranslatedQuery());
        Assertions.assertEquals(1, mappedQuery.getParamIndex("param1"));
        Assertions.assertEquals(2, mappedQuery.getParamIndex("param2"));
        Assertions.assertEquals(3, mappedQuery.getParamIndex("param3"));
        Assertions.assertEquals(4, mappedQuery.getParamIndex("param4"));

        MappedQuery mappedQuery2 = QueryParser.parseNPSql("SELECT * FROM table_x WHERE id = :param_1");
        Assertions.assertEquals(1, mappedQuery2.getParamIndex("param_1"));
    }
}
