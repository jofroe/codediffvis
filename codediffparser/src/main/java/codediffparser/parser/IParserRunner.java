package codediffparser.parser;

import codediffparser.callgraph.Mode;

public interface IParserRunner {

    String getResult();

    void run(String sourcesRoot, String dependenciesRoot, String[] filterFiles, Mode mode);
}
