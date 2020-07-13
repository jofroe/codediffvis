package codediffparser.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import codediffparser.parser.eclipse.DependencyHierarchicalASTVisitor;

@TestInstance(Lifecycle.PER_CLASS)
public class DependencyHierarchicalASTVisitorTest {
    private DependencyHierarchicalASTVisitor visitor;

    @BeforeAll
    public void beforeAll() {
        visitor = new DependencyHierarchicalASTVisitor(null, null, null);
    }

    @Test
    public void testExtractDeclaringClassNames() {
        try {
            Method method = visitor.getClass().getMethod("extractDeclaringClassNames");
            method.setAccessible(true);
            assertEquals(null, method.invoke(visitor, "com.test.MyClass", "com.test", "MyClass"));
            assertEquals("OtherClass", method.invoke(visitor, "com.test.OtherClass.MyClass", "com.test", "MyClass"));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            fail();
        }
    }
}
