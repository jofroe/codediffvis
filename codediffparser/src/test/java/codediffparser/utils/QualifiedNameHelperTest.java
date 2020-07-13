package codediffparser.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class QualifiedNameHelperTest {

    @Test
    public void testConcatSave() {
        assertEquals("", QualifiedNameHelper.concatSave("", null));
        assertEquals("a.bc", QualifiedNameHelper.concatSave("a", "", null, "bc"));
        assertEquals("a-bc", QualifiedNameHelper.concatSaveWithDelimiter("-", "a", "", null, "bc"));
    }

}
