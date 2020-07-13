package codediffparser.callgraph;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import codediffparser.utils.QualifiedNameHelper;

@TestInstance(Lifecycle.PER_CLASS)
public class LinkTest {
    private LinkTestDataProvider provider;

    @BeforeAll
    public void beforeAll() {
        this.provider = new LinkTestDataProvider();
    }

    @Test
    public void testBuild() {
        assertThrows(NullPointerException.class, () -> new Link.Builder().build());
        assertThrows(NullPointerException.class, () -> new Link.Builder().withSource("MyClass").build());
        assertDoesNotThrow(() -> provider.getEmpty());
    }

    @Test
    public void testQualifiedName() {
        final Link link = provider.getEmpty();
        assertEquals(link.getId(), QualifiedNameHelper.concatSaveWithDelimiter(":", link.getRelation().toString(), link.getSource(), link.getTarget()));
    }
}
