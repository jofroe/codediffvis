package codediffparser.testclasses.inner;

import java.util.Arrays;
import java.util.List;

public abstract class MyOtherClass {

    @FunctionalInterface
    public interface IDoStuff {
        String applyMe(String s);
    }

    public static void foo() {
    }

    private void doSomething(IDoStuff useMe) {
        final List<String> myList = Arrays.asList("This is a small test!".split(" "));
        myList.stream()
                .map(String::toUpperCase)
                .filter(s -> s.startsWith("T"))
                .forEach(System.out::println);

        useMe.applyMe("TEST");
    }

    public void applyDoSomething() {
        doSomething(String::toLowerCase);
    }
}
