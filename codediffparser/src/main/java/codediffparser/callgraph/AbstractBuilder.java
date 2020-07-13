package codediffparser.callgraph;

import java.util.Arrays;
import java.util.Objects;

public abstract class AbstractBuilder<T> {
    public abstract T build();

    protected void nullCheck(Object... objects) {
        if (Arrays.stream(objects).anyMatch(Objects::isNull)) {
            // TODO[jfo]: throw other exception
            throw new NullPointerException("Not all required fields are set.");
        }
    }
}
