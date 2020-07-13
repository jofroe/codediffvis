package codediffparser.callgraph;

public class GraphTestDataProvider extends AbstractTestDataProvider<Graph> {

    public GraphTestDataProvider() {
    }

    @Override
    public Graph getEmpty() {
        return new Graph();
    }
}
