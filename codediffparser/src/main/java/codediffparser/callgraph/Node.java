package codediffparser.callgraph;

import static codediffparser.utils.QualifiedNameHelper.concatSave;
import static codediffparser.utils.QualifiedNameHelper.concatSaveWithDelimiter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node {
    private String filePath;
    private String name;
    private String declariningClassesName;
    private String packageName;
    private NodeType type;
    private List<String> modifiers;
    private Integer[] position;
    private Integer[] positionOld;
    private Status status;
    private String parentNodeId;
    private int paramHashCode;
    private int nodeHashCode;
    private boolean isGenerated;

    private Node() {
    }

    public enum NodeType {
        CLASS,
        INTERFACE,
        ABSTRACT_CLASS,
        METHOD,
        TYPE_REFERENCE,
        METHOD_REFERENCE,
        NONJAVA;

        public boolean isDeclaredType() {
            if (List.of(CLASS, INTERFACE, ABSTRACT_CLASS, METHOD).contains(this)) {
                return true;
            }
            return false;
        }

        public boolean isReferencedType() {
            if (List.of(TYPE_REFERENCE, METHOD_REFERENCE).contains(this)) {
                return true;
            }
            return false;
        }
    }

    public String getId() {
        if (type == NodeType.METHOD || type == NodeType.METHOD_REFERENCE) {
            return concatSave(packageName, concatSaveWithDelimiter("#", declariningClassesName, name)) + paramHashCode;
        } else if (type == NodeType.NONJAVA) {
            return filePath;

        } else {
            return concatSave(packageName, declariningClassesName, name);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return name;
    }

    public String getDeclaringClassesName() {
        return declariningClassesName;
    }

    public String getPackageName() {
        return packageName;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public List<String> getModifiers() {
        if (modifiers == null) {
            modifiers = new ArrayList<>();
        }
        return modifiers;
    }

    public Integer[] getPosition() {
        return position;
    }

    public Integer[] getPositionOld() {
        return positionOld;
    }

    public void setPositionOld(Integer[] positionOld) {
        if (position == null) {
            this.position = positionOld;
        } else if (!Arrays.equals(position, positionOld)) {
            this.positionOld = positionOld;
        }
    }

    public Status getStatus() {
        return status;
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    public void addModifier(String modifier) {
        getModifiers().add(modifier);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getNodeHashCode() {
        return nodeHashCode;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public void setGenerated(boolean isGenerated) {
        this.isGenerated = isGenerated;
    }

    public static class Builder extends AbstractBuilder<Node> {
        private String filePath;
        private String name;
        private String declaringClassesName;
        private String packageName;
        private NodeType type;
        private List<String> modifiers;
        private Integer[] position;
        private String parentNodeId;
        private int paramHashCode;
        private int nodeHashCode;
        private boolean isGenerated = false;

        public Builder withFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDeclaringClassesName(String declaringClassesName) {
            this.declaringClassesName = declaringClassesName;
            return this;
        }

        public Builder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder withType(NodeType type) {
            this.type = type;
            return this;
        }

        public Builder withModifiers(List<String> modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder withPosition(Integer lineNumberStart, Integer lineNumberEnd) {
            this.position = new Integer[] { lineNumberStart, lineNumberEnd };
            return this;
        }

        public Builder withParentNodeId(String parentNodeId) {
            this.parentNodeId = parentNodeId;
            return this;
        }

        public Builder withParamHashCode(int paramHashCode) {
            this.paramHashCode = paramHashCode;
            return this;
        }

        public Builder withNodeHashCode(int nodeHashCode) {
            this.nodeHashCode = nodeHashCode;
            return this;
        }

        public Builder withIsGenerated(boolean isGenerated) {
            this.isGenerated = isGenerated;
            return this;
        }

        @Override
        public Node build() {
            final Node node = new Node();
            node.filePath = this.filePath;
            node.name = this.name;
            node.declariningClassesName = this.declaringClassesName;
            node.packageName = this.packageName;
            node.type = this.type;
            node.modifiers = this.modifiers;
            node.position = this.position;
            node.parentNodeId = this.parentNodeId;
            node.paramHashCode = this.paramHashCode;
            node.nodeHashCode = this.nodeHashCode;
            node.isGenerated = this.isGenerated;

            nullCheck(name, type);
            return node;
        }
    }
}
