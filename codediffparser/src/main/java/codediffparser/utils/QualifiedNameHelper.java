package codediffparser.utils;

import java.util.Arrays;
import java.util.Objects;

public class QualifiedNameHelper {
    private static final String DEFAULT_DELIMITER = ".";

    public static String concatSaveWithDelimiter(String delimiter, String... names) {
        return String.join(delimiter, Arrays.asList(names).stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new));
    }

    public static String concatSave(String... names) {
        return concatSaveWithDelimiter(DEFAULT_DELIMITER, names);
    }
}
