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
public class NodeTest {
    private NodeTestDataProvider provider;

    @BeforeAll
    public void beforeAll() {
        this.provider = new NodeTestDataProvider();
    }

    @Test
    public void testBuild() {
        assertThrows(NullPointerException.class, () -> new Node.Builder().build());
        assertThrows(NullPointerException.class, () -> new Node.Builder().withName("Test").build());
        assertDoesNotThrow(() -> provider.getEmpty());
    }

    @Test
    public void testQualifiedName() {
        final Node node = provider.getEmpty();
        assertEquals(node.getId(), QualifiedNameHelper.concatSave(node.getPackageName(), node.getDeclaringClassesName(), node.getName()));
    }
}
