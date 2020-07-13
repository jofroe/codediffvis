package codediffparser.parser.eclipse;

import org.eclipse.jdt.core.dom.AST;

public interface IEclipseASTConstants {
    final static String[] EMPTY_STRING_ARRAY = new String[] { "" };
    final static int API_LEVEL = AST.JLS13;
    final static String JAVA_FILE_ENDING = "java";
    final static String JAR_FILE_ENDING = "jar";
}
