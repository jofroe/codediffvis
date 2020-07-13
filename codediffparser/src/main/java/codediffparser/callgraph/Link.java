package codediffparser.callgraph;

import codediffparser.utils.QualifiedNameHelper;

public class Link {
    private String source;
    private String target;
    private LinkRelation relation;
    private Status status;

    public enum LinkRelation {
        SUPERCLASS,
        ENCLOSING_CLASS,
        INTERFACE,
        METHOD_CALL,
        METHOD,
        TYPE
    }

    public String getId() {
        return QualifiedNameHelper
                .concatSaveWithDelimiter(":", relation.toString(), source, target);
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public LinkRelation getRelation() {
        return relation;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public static class Builder extends AbstractBuilder<Link> {
        private String source;
        private String target;
        private LinkRelation relation;

        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        public Builder withTarget(String target) {
            this.target = target;
            return this;
        }

        public Builder withRelation(LinkRelation relation) {
            this.relation = relation;
            return this;
        }

        @Override
        public Link build() {
            final Link link = new Link();
            link.source = this.source;
            link.target = this.target;
            link.relation = this.relation;
            nullCheck(source, target, relation);
            return link;
        }
    }
}
