package codediffparser.callgraph;

import codediffparser.callgraph.Link.LinkRelation;

public class LinkTestDataProvider extends AbstractTestDataProvider<Link> {
    private static final String source = "test.class.MyClass";
    private static final String target = "test.otherclass.MyOtherClass";
    private static final LinkRelation relation = LinkRelation.METHOD_CALL;

    public LinkTestDataProvider() {
    }

    @Override
    public Link getEmpty() {
        return new Link.Builder()
                .withSource(source)
                .withTarget(target)
                .withRelation(relation)
                .build();
    }
}
