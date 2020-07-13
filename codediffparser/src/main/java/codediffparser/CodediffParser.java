package codediffparser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import codediffparser.callgraph.Mode;
import codediffparser.parser.IParserRunner;
import codediffparser.parser.ParserRunnerFactory;
import codediffparser.parser.ParserRunnerFactory.PARSER_TYPE;

public class CodediffParser {

    public static void main(String[] args) {

        String[] filterFiles = new String[0];
        if (args.length == 4) {
            try {
                filterFiles = Files.readString(new File(args[3]).toPath(), Charset.defaultCharset()).split("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(
                    "Usage:\njava -jar CodeDiffParser.jar <path/to/source/branch> <path/to/target/branch> <path/to/dependencies/root> <path/to/changed_files.json>");
            return;
        }

        final String sourceBranch = args[0];
        final String targetBranch = args[1];
        final String dependenciesRoot = args[2];

        IParserRunner parser = ParserRunnerFactory.get(PARSER_TYPE.ECLIPSE_PARSER);
        parser.run(targetBranch, dependenciesRoot, filterFiles, Mode.TARGET); // aka the old branch
        parser.run(sourceBranch, dependenciesRoot, filterFiles, Mode.SOURCE); // aka the new branch
        System.out.println(parser.getResult());
    }
}
