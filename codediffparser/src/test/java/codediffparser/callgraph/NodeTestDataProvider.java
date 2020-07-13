package codediffparser.callgraph;

import codediffparser.callgraph.Node.NodeType;

public class NodeTestDataProvider extends AbstractTestDataProvider<Node> {
    private static final String className = "MyClass";
    private static final String packageName = "mypackage";
    private static final NodeType type = NodeType.CLASS;

    public NodeTestDataProvider() {
    }

    private Node.Builder getBaseBuild() {
        return new Node.Builder()
                .withName(className)
                .withPackageName(packageName)
                .withType(type);
    }

    @Override
    public Node getEmpty() {
        return getBaseBuild()
                .build();
    }
}
