package codediffparser.parser;

import codediffparser.parser.eclipse.EclipseASTParserRunner;

public class ParserRunnerFactory {
    public enum PARSER_TYPE {
        ECLIPSE_PARSER(new EclipseASTParserRunner());

        private final IParserRunner parserRunner;

        PARSER_TYPE(IParserRunner parserRunner) {
            this.parserRunner = parserRunner;
        }

        public IParserRunner getInstance() {
            return this.parserRunner;
        }
    }

    public static IParserRunner get(PARSER_TYPE type) {
        return type.getInstance();
    }

    public static IParserRunner get() {
        return get(PARSER_TYPE.ECLIPSE_PARSER);
    }
}
