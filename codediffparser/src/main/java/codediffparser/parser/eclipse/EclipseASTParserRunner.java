package codediffparser.parser.eclipse;

import static codediffparser.parser.eclipse.IEclipseASTConstants.EMPTY_STRING_ARRAY;
import static codediffparser.parser.eclipse.IEclipseASTConstants.JAR_FILE_ENDING;
import static codediffparser.parser.eclipse.IEclipseASTConstants.JAVA_FILE_ENDING;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTParser;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import codediffparser.callgraph.Graph;
import codediffparser.callgraph.Mode;
import codediffparser.callgraph.Node;
import codediffparser.callgraph.Node.NodeType;
import codediffparser.parser.IParserRunner;

public class EclipseASTParserRunner implements IParserRunner {
    private Graph graph;

    public EclipseASTParserRunner() {
        this.graph = new Graph();
    }

    private <T> String toJson(T object) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        String jsonString = "";
        try {
            jsonString = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    @Override
    public String getResult() {
        return toJson(this.graph.getPrintableGraph());
    }

    @Override
    public void run(String sourcesRoot, String dependenciesRoot, String[] filterFiles, Mode mode) {
        final String separator = java.io.File.separator.equals("\\") ? "\\\\" : java.io.File.separator;
        sourcesRoot = sourcesRoot.replaceAll("\\/|\\\\", separator);
        graph.setMode(mode);
        final List<String> filterFilesList = List.of(filterFiles).stream().map(file -> file.replaceAll("\\/|\\\\", separator)).collect(Collectors.toList());
        filterFilesList.stream()
                .filter(file -> !file.endsWith(JAVA_FILE_ENDING))
                .forEach(file -> {
                    final String[] split = file.split(separator);
                    graph.addNode(new Node.Builder()
                            .withFilePath(file)
                            .withName(split[split.length - 1])
                            .withType(NodeType.NONJAVA)
                            .build());
                });

        String[] srcPaths = FileUtils.listFiles(new File(sourcesRoot), new String[] { JAVA_FILE_ENDING }, true)
                .stream().map(File::toString)
                .filter(f -> {
                    return filterFilesList.isEmpty() || filterFilesList.stream().anyMatch(i -> f.endsWith(i));
                })
                .toArray(String[]::new);

        String[] classPaths = FileUtils.listFiles(new File(dependenciesRoot), new String[] { JAR_FILE_ENDING }, true)
                .stream().map(File::toString).toArray(String[]::new);

        ASTParser parser = new ASTParserProvider().withClassPaths(classPaths).get();
        final DependencyFileASTRequestor requestor = new DependencyFileASTRequestor(sourcesRoot, filterFiles, graph);
        parser.createASTs(srcPaths, null, EMPTY_STRING_ARRAY, requestor, null);

    }
}
