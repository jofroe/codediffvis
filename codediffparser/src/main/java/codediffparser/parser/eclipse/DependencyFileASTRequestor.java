package codediffparser.parser.eclipse;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import codediffparser.callgraph.Graph;

public final class DependencyFileASTRequestor extends FileASTRequestor {
    private Graph graph;
    private String sourcesRoot;
    private String[] filterFiles;

    public DependencyFileASTRequestor(String sourcesRoot, String[] filterFiles, Graph graph) {
        this.sourcesRoot = sourcesRoot;
        this.filterFiles = filterFiles;
        this.graph = graph;
    }

    @Override
    public void acceptAST(String filePath, CompilationUnit cu) {
        final DependencyHierarchicalASTVisitor visitor = new DependencyHierarchicalASTVisitor(cu, filePath.replace(sourcesRoot, ""), graph);
        cu.accept(visitor);
    }
}
