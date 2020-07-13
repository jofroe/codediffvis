package codediffparser.parser.eclipse;

import static codediffparser.parser.eclipse.IEclipseASTConstants.API_LEVEL;
import static codediffparser.parser.eclipse.IEclipseASTConstants.EMPTY_STRING_ARRAY;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;

public class ASTParserProvider {
    private String[] classPaths;

    public ASTParserProvider() {
        this.classPaths = EMPTY_STRING_ARRAY;
    }

    public ASTParserProvider withClassPaths(String[] classPaths) {
        this.classPaths = classPaths;
        return this;
    }

    public ASTParser get() {
        ASTParser parser = ASTParser.newParser(API_LEVEL);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_13, options);
        parser.setCompilerOptions(options);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(classPaths, EMPTY_STRING_ARRAY, null, true);
        return parser;
    }
}
