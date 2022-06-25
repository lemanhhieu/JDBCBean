package jdbcBean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.sql.SQLException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SharedDbContext {
    protected CommentsDb commentsDb;

    public SharedDbContext() throws Exception {
        commentsDb = CommentsDb.getInstance();
    }



}
