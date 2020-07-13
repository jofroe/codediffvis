package codediffparser.callgraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class Graph {
    private Map<String, Link> links;
    private Map<String, Node> nodes;
    private Mode mode;

    public Collection<Link> getLinks() {
        return links.values();
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Graph() {
        this.links = new HashMap<>();
        this.nodes = new HashMap<>();
        this.mode = Mode.TARGET;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    private Status statusNew() {
        Status resultStatus;
        switch (mode) {
        case TARGET:
            resultStatus = Status.DELETED;
            break;
        case SOURCE:
        default:
            resultStatus = Status.ADDED;
        }
        return resultStatus;
    }

    private void updateStatus(Link link) {
        if (link.getStatus() != Status.ADDED && link.getStatus() != statusNew()) {
            link.setStatus(Status.UNCHANGED);
        }
    }

    public void addLink(Link link) {
        Optional.ofNullable(links.get(link.getId())).ifPresentOrElse(storedLink -> {
            updateStatus(storedLink);
        }, () -> {
            link.setStatus(statusNew());
            links.put(link.getId(), link);
        });
    }

    private Consumer<? super Node> updateExistingNode(Node node) {
        return storedNode -> {
            if (node.getType().isDeclaredType() && storedNode.getType().isReferencedType()) {
                node.setStatus(statusNew());
                nodes.put(node.getId(), node);
                return;
            } else if (node.getType().isReferencedType() && storedNode.getType().isDeclaredType()) {
                // ignore
                return;
            } else if (node.getType().isDeclaredType() && storedNode.getType().isDeclaredType()) {
                if (storedNode.getStatus() == statusNew()) {
                    return; // skip, somehow visited multiple times
                }
                if (node.getNodeHashCode() != storedNode.getNodeHashCode()) {
                    storedNode.setStatus(Status.CHANGED);
                    storedNode.setPositionOld(node.getPosition());
                } else if (storedNode.getStatus() == Status.DELETED && statusNew() == Status.ADDED) {
                    storedNode.setStatus(Status.UNCHANGED);
                    storedNode.setPositionOld(node.getPosition());
                }
            } else if (storedNode.getType().isReferencedType()) {
                storedNode.setStatus(Status.UNCHANGED);
            } else {
                // NodeType NONJAVA
                storedNode.setStatus(Status.CHANGED);
            }
        };
    }

    public Runnable addNewNode(Node node) {
        return () -> {
            if (node.getType().isReferencedType()) {
                node.setStatus(Status.UNCHANGED);
            } else {
                node.setStatus(statusNew());
            }
            nodes.put(node.getId(), node);
        };
    }

    public void addNode(Node node) {
        Optional.ofNullable(nodes.get(node.getId()))
                .ifPresentOrElse(
                        updateExistingNode(node),
                        addNewNode(node));
    }

    private void updateParentNodes(Node node) {
        final Node parentNode = nodes.get(node.getParentNodeId());
        if (parentNode != null) {
            if (parentNode.getStatus() == Status.UNCHANGED) {
                parentNode.setStatus(Status.CHANGED);
            }
            updateParentNodes(parentNode);
        }
    }

    private void updateChildNodes(Node node) {
        nodes.values().stream().filter(childNode -> node.getId().equals(childNode.getParentNodeId()))
                .forEach(childNode -> {
                    childNode.setGenerated(true);
                    updateChildNodes(childNode);
                });
    }

    public void cleanup() {
        // update nodes with !unchanged links
        links.values().stream().filter(link -> link.getStatus() != Status.UNCHANGED).forEach(link -> {
            Node node = nodes.get(link.getSource());
            if (node != null && !node.getType().isReferencedType() && node.getStatus() == Status.UNCHANGED) {
                node.setStatus(Status.CHANGED);
            }
        });

        // update parent nodes of !unchanged nodes
        nodes.values().stream().filter(node -> node.getStatus() != Status.UNCHANGED)
                .forEach(node -> updateParentNodes(node));

        // update child nodes of @generated nodes
        nodes.values().stream()
                .filter(node -> node.isGenerated())
                .forEach(node -> updateChildNodes(node));
    }

    public PrintableGraph getPrintableGraph() {
        cleanup();
        return new PrintableGraph(getLinks(), getNodes());
    }

    private static class PrintableGraph {
        private Collection<Link> links;
        private Collection<Node> nodes;

        public PrintableGraph(Collection<Link> links, Collection<Node> nodes) {
            this.links = links;
            this.nodes = nodes;
        }
    }
}
