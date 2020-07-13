package codediffparser.parser.eclipse;

import static codediffparser.utils.QualifiedNameHelper.concatSave;
import static codediffparser.utils.QualifiedNameHelper.concatSaveWithDelimiter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.core.nd.indexer.HierarchicalASTVisitor;

import codediffparser.callgraph.Graph;
import codediffparser.callgraph.Link;
import codediffparser.callgraph.Link.LinkRelation;
import codediffparser.callgraph.Node;
import codediffparser.callgraph.Node.NodeType;

public final class DependencyHierarchicalASTVisitor extends HierarchicalASTVisitor {
    private Graph graph;
    private CompilationUnit compilationUnit;
    private String packageName;
    private String filePath;

    public Graph getGraph() {
        return graph;
    }

    public DependencyHierarchicalASTVisitor(CompilationUnit compilationUnit, String filePath, Graph graph) {
        this.graph = graph;
        this.filePath = filePath;
        this.compilationUnit = compilationUnit;
        this.packageName = compilationUnit == null ? null : compilationUnit.getPackage().getName().toString();
    }

    private List<String> declaringClassTraversal(ASTNode node, List<String> containerClasses) {
        if (node.getNodeType() == ASTNode.COMPILATION_UNIT) {
            Collections.reverse(containerClasses);
            return containerClasses;
        } else if (node.getNodeType() == ASTNode.TYPE_DECLARATION) {
            containerClasses.add(((TypeDeclaration) node).getName().getFullyQualifiedName());
        }
        return declaringClassTraversal(node.getParent(), containerClasses);
    }

    private String concatDeclaringClassNames(ASTNode node) {
        final String result = concatSave(declaringClassTraversal(node.getParent(), new ArrayList<>()).toArray(String[]::new));
        return result.isEmpty() ? null : result;
    }

    private String extractDeclaringClassNames(String fullyQualifiedName, String packageName, String className) {
        if (packageName.length() + 1 < fullyQualifiedName.length() - className.length() - 1) {
            return fullyQualifiedName.substring(packageName.length(), fullyQualifiedName.length() - className.length()).replaceAll("^\\.|\\.$", "");
        }
        return null;
    }

    private MethodDeclaration getEnclosingMethod(ASTNode node) {
        final ASTNode parent = node.getParent();
        if (parent == null) {
            return null;
        } else if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
            return (MethodDeclaration) parent;
        }
        return getEnclosingMethod(parent);
    }

    private String getDeclaringClassName(final ITypeBinding declaringClass) {
        return declaringClass == null ? null : declaringClass.getName();
    }

    private int getLineNumberStart(ASTNode node) {
        return compilationUnit.getLineNumber(node.getStartPosition());
    }

    private int getLineNumberEnd(ASTNode node) {
        return compilationUnit.getLineNumber(node.getStartPosition() + node.getLength());
    }

    private NodeType nodeType(TypeDeclaration node, List<String> modifiers) {
        if (node.isInterface()) {
            return NodeType.INTERFACE;
        } else if (modifiers.stream().filter(modifier -> modifier.toLowerCase().equals("abstract")).findAny().isPresent()) {
            return NodeType.ABSTRACT_CLASS;
        }
        return NodeType.CLASS;
    }

    @Override
    public boolean visit(Type node) {
        final ITypeBinding resolvedBinding;
        try {
            resolvedBinding = node.resolveBinding();
        } catch (NullPointerException e) {
            // Resolving failed with error, fail silently
            return super.visit(node);
        }

        if (resolvedBinding == null || resolvedBinding.isRecovered()) {
            // Resolve recovered. Possibly only partial data!
            return super.visit(node);
        }
        String target = null;
        if (ASTNode.SIMPLE_TYPE == node.getNodeType() && !resolvedBinding.getQualifiedName().contains("<")) {
            target = resolvedBinding.getQualifiedName();
        } else if (node.getNodeType() == ASTNode.PARAMETERIZED_TYPE) {
            target = resolvedBinding.getTypeDeclaration().getBinaryName();
        } else {
            return super.visit(node);
        }

        if (resolvedBinding.getPackage() == null) {
            // Could not resolve package for node, fail silently
            return super.visit(node);
        }

        final String resolvedPackageName = resolvedBinding.getPackage().getName();
        final String resolvedClassName = resolvedBinding.getTypeDeclaration().getName();
        final String declaringClassName = extractDeclaringClassNames(target, resolvedPackageName, resolvedClassName);
        final String source = concatSave(packageName, concatDeclaringClassNames(node));
        final String containingNodeId = declaringClassName == null ? concatSave(packageName, declaringClassName) : null;

        graph.addNode(new Node.Builder()
                .withName(resolvedClassName)
                .withDeclaringClassesName(declaringClassName)
                .withPackageName(resolvedPackageName)
                .withType(NodeType.TYPE_REFERENCE)
                .withParentNodeId(containingNodeId)
                .build());
        graph.addLink(new Link.Builder()
                .withSource(source)
                .withTarget(target)
                .withRelation(LinkRelation.TYPE)
                .build());

        return super.visit(node);

    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(MethodDeclaration node) {
        final String name = node.getName().toString();
        final String declaringClassesNameSource = concatDeclaringClassNames(node);
        final String source = concatSave(packageName, declaringClassesNameSource);
        final int lineNumberStart = getLineNumberStart(node);
        final int lineNumberEnd = getLineNumberEnd(node);
        final int paramHashCode = node.parameters().toString().hashCode();
        final String qualifiedMethodNameSource = concatSaveWithDelimiter("#", source, name) + paramHashCode;
        final List<String> modifiers = (List<String>) node.modifiers().stream().map(Object::toString).map(s -> s.toString().replaceAll("\\(.*", ""))
                .collect(Collectors.toList());
        final boolean isGenerated = modifiers.stream().filter(modifier -> modifier.toLowerCase().equals("@generated")).findAny().isPresent();

        graph.addNode(new Node.Builder()
                .withFilePath(filePath)
                .withName(name)
                .withDeclaringClassesName(declaringClassesNameSource)
                .withPackageName(packageName)
                .withType(NodeType.METHOD)
                .withPosition(lineNumberStart, lineNumberEnd)
                .withParentNodeId(source)
                .withNodeHashCode(node.toString().hashCode())
                .withParamHashCode(paramHashCode)
                .withIsGenerated(isGenerated)
                .build());

        graph.addLink(new Link.Builder()
                .withSource(source)
                .withTarget(qualifiedMethodNameSource)
                .withRelation(LinkRelation.METHOD)
                .build());

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        if (node.resolveMethodBinding() == null) {
            // Resolve failed!
            return super.visit(node);
        }
        ITypeBinding declaringClass = node.resolveMethodBinding().getDeclaringClass();

        final String declaringClassesNameSource = concatDeclaringClassNames(node);
        final String source = concatSave(packageName, declaringClassesNameSource);
        final String target = declaringClass.getTypeDeclaration().getQualifiedName();

        final String declaringPackage = declaringClass.getPackage().getName();
        final String declaringClassName = declaringClass.getName();
        final String declaringClassesNameTarget = extractDeclaringClassNames(target, declaringPackage, declaringClassName);
        final String declaringClassesNameMethodTarget = extractDeclaringClassNames(target, declaringPackage, "");
        final MethodDeclaration enclosingMethod = getEnclosingMethod(node);
        final String methodNameSource = enclosingMethod == null ? "" : enclosingMethod.getName().toString();
        final String methodNameTarget = node.getName().toString();

        final String containingTargetNodeId = declaringClassesNameTarget != null ? concatSave(declaringPackage, declaringClassesNameTarget) : null;
        final String containingMethodTargetNodeId = declaringClassesNameMethodTarget != null ? concatSave(declaringPackage, declaringClassesNameMethodTarget)
                : null;

        final int paramHashCodeTarget = Arrays.toString(node.resolveMethodBinding().getMethodDeclaration().getTypeParameters()).hashCode();
        final int paramHashCodeSource = enclosingMethod == null ? 0 : enclosingMethod.parameters().toString().hashCode();
        final String qualifiedMethodNameSource = concatSaveWithDelimiter("#", source, methodNameSource) + (enclosingMethod == null ? "" : paramHashCodeSource);
        final String qualifiedMethodNameTarget = concatSaveWithDelimiter("#", target, methodNameTarget) + paramHashCodeTarget;

        graph.addNode(new Node.Builder()
                .withName(declaringClassName)
                .withDeclaringClassesName(declaringClassesNameTarget)
                .withPackageName(declaringPackage)
                .withType(NodeType.TYPE_REFERENCE)
                .withParentNodeId(containingTargetNodeId)
                .build());
        graph.addNode(new Node.Builder()
                .withName(methodNameTarget)
                .withDeclaringClassesName(declaringClassesNameMethodTarget)
                .withPackageName(declaringPackage)
                .withType(NodeType.METHOD_REFERENCE)
                .withParentNodeId(containingMethodTargetNodeId)
                .withParamHashCode(paramHashCodeTarget)
                .build());

        graph.addLink(new Link.Builder()
                .withSource(qualifiedMethodNameSource)
                .withTarget(qualifiedMethodNameTarget)
                .withRelation(LinkRelation.METHOD_CALL)
                .build());
        graph.addLink(new Link.Builder()
                .withSource(qualifiedMethodNameTarget)
                .withTarget(target)
                .withRelation(LinkRelation.METHOD)
                .build());

        return super.visit(node);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeDeclaration node) {
        final String declaringClassNames = concatDeclaringClassNames(node);
        final List<String> modifiers = (List<String>) node.modifiers().stream().map(Object::toString).map(s -> s.toString().replaceAll("\\(.*", ""))
                .collect(Collectors.toList());
        final NodeType type = nodeType(node, modifiers);
        final String source = concatSave(packageName, declaringClassNames, node.getName().toString());
        final int lineNumberStart = getLineNumberStart(node);
        final int lineNumberEnd = getLineNumberEnd(node);
        final String containingNodeId = declaringClassNames != null ? concatSave(packageName, declaringClassNames) : null;
        final boolean isGenerated = modifiers.stream().filter(modifier -> modifier.toLowerCase().equals("@generated")).findAny().isPresent();

        // Superclass
        if (node.getSuperclassType() != null) {
            final ITypeBinding resolvedSuperClass = node.getSuperclassType().resolveBinding();
            final ITypeBinding declaringSuperClass = resolvedSuperClass.getDeclaringClass();

            String target = "";
            if (resolvedSuperClass.getQualifiedName() == null) {
                target = concatSave(resolvedSuperClass.getPackage().getName(), resolvedSuperClass.getName());
            } else if (!resolvedSuperClass.getQualifiedName().contains("<")) {
                target = resolvedSuperClass.getQualifiedName();
            } else {
                target = resolvedSuperClass.getTypeDeclaration().getBinaryName();
            }

            graph.addNode(new Node.Builder()
                    .withFilePath(filePath)
                    .withName(resolvedSuperClass.getTypeDeclaration().getName())
                    .withDeclaringClassesName(getDeclaringClassName(declaringSuperClass))
                    .withPackageName(resolvedSuperClass.getPackage().getName())
                    .withType(NodeType.TYPE_REFERENCE)
                    .build());
            graph.addLink(new Link.Builder()
                    .withSource(source)
                    .withTarget(target)
                    .withRelation(LinkRelation.SUPERCLASS)
                    .build());
        }

        // Interfaces
        node.superInterfaceTypes().stream().forEach(i -> {
            final String interfaceName = ((Type) i).toString();
            final ITypeBinding declaringClass = ((Type) i).resolveBinding().getDeclaringClass();
            final String containingInterfaceName = getDeclaringClassName(declaringClass);
            final String interfacePackageName = ((Type) i).resolveBinding().getPackage().getName();
            final String target = concatSave(interfacePackageName, containingInterfaceName, interfaceName);

            graph.addNode(new Node.Builder()
                    .withFilePath(filePath)
                    .withName(interfaceName)
                    .withDeclaringClassesName(containingInterfaceName)
                    .withPackageName(interfacePackageName)
                    .withType(NodeType.TYPE_REFERENCE)
                    .build());
            graph.addLink(new Link.Builder()
                    .withSource(source)
                    .withTarget(target)
                    .withRelation(LinkRelation.INTERFACE)
                    .build());
        });

        // Class
        graph.addNode(new Node.Builder()
                .withFilePath(filePath)
                .withName(node.getName().toString())
                .withDeclaringClassesName(declaringClassNames)
                .withPackageName(packageName)
                .withModifiers(modifiers)
                .withType(type)
                .withPosition(lineNumberStart, lineNumberEnd)
                .withParentNodeId(containingNodeId)
                .withNodeHashCode(node.toString().hashCode())
                .withIsGenerated(isGenerated)
                .build());

        if (containingNodeId != null) {
            graph.addLink(new Link.Builder()
                    .withSource(source)
                    .withTarget(containingNodeId)
                    .withRelation(LinkRelation.ENCLOSING_CLASS)
                    .build());
        }

        return super.visit(node);
    }
}