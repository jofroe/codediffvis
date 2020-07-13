package codediffparser.callgraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import codediffparser.callgraph.Link.LinkRelation;
import codediffparser.callgraph.Node.NodeType;

@TestInstance(Lifecycle.PER_CLASS)
public class GraphTest {
    private GraphTestDataProvider graphProvider;
    private NodeTestDataProvider nodeProvider;
    private LinkTestDataProvider linkProvider;

    @BeforeAll
    public void beforeAll() {
        this.graphProvider = new GraphTestDataProvider();
        this.nodeProvider = new NodeTestDataProvider();
        this.linkProvider = new LinkTestDataProvider();
    }

    @Test
    public void testAddNode() {
        Graph graph = graphProvider.getEmpty();
        Node node = nodeProvider.getEmpty();
        graph.addNode(node);

        assertEquals(1L, graph.getNodes().stream().count());
    }

    @Test
    public void testMode() {
        Graph graph = graphProvider.getEmpty();
        Link link = linkProvider.getEmpty();
        Link link2 = new Link.Builder()
                .withSource("source")
                .withTarget("target")
                .withRelation(LinkRelation.METHOD_CALL)
                .build();
        Node node = new Node.Builder()
                .withName("class")
                .withPackageName("package")
                .withType(NodeType.CLASS)
                .build();
        Node node2 = new Node.Builder()
                .withName("subclass")
                .withPackageName("package")
                .withType(NodeType.CLASS)
                .withParentNodeId(node.getId())
                .build();

        graph.setMode(Mode.TARGET);
        graph.addLink(link);
        graph.addLink(link2);
        graph.addNode(node);

        graph.setMode(Mode.SOURCE);
        graph.addLink(link);
        graph.addNode(node);
        graph.addNode(node2);

        graph.cleanup();

        assertFalse(graph.getNodes().stream()
                .anyMatch(n -> (n.getId() == node.getId() && n.getStatus() == Status.CHANGED)
                        || (n.getId() == node2.getId() && n.getStatus() == Status.ADDED)));

        assertFalse(graph.getLinks().stream()
                .anyMatch(l -> (l.getId() == link.getId() && l.getStatus() == Status.UNCHANGED)
                        || (l.getId() == link2.getId() && l.getStatus() == Status.DELETED)));
    }
}
