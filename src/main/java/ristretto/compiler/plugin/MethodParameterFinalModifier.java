package ristretto.compiler.plugin;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import java.util.LinkedList;

final class MethodParameterFinalModifier extends TreeScanner<MethodParameterFinalModifier.Context, MethodParameterFinalModifier.Context> {

    static final TreeScanner<Context, Context> INSTANCE = new MethodParameterFinalModifier();

    private MethodParameterFinalModifier() {
    }

    @Override
    public Context visitImport(ImportTree importTree, Context context) {
        context.resolver.importClass(importTree.getQualifiedIdentifier().toString());
        return super.visitImport(importTree, context);
    }

    @Override
    public Context visitClass(ClassTree node, Context context) {
        context.enterClass();
        Context result = super.visitClass(node, context);
        context.leave();
        return result;
    }

    @Override
    public Context visitMethod(MethodTree method, Context context) {
        context.enterMethod();
        Context result = super.visitMethod(method, context);
        context.leave();
        return result;
    }

    @Override
    public Context visitBlock(BlockTree block, Context context) {
        context.enterBlock();
        Context result = super.visitBlock(block, context);
        context.leave();
        return result;
    }

    @Override
    public Context visitForLoop(ForLoopTree forLoop, Context context) {
        context.enterForLoop();
        Context result = super.visitForLoop(forLoop, context);
        context.leave();
        return result;
    }

    @Override
    public Context visitVariable(VariableTree variable, Context context) {
        if (context.isNotMethod()) {
            return super.visitVariable(variable, context);
        }

        if (JCTreeCatalog.isAnnotatedAsMutable(variable, context.resolver)) {
            context.observer.skipped(VariableScope.METHOD);
            return super.visitVariable(variable, context);
        }

        JCTreeCatalog.addFinalModifier(variable);
        context.observer.markedAsFinal(VariableScope.METHOD);
        return super.visitVariable(variable, context);
    }

    public static final class Context {

        private final AnnotationNameResolver resolver;
        private final VariableFinalMarkerObservable observer;
        private final LinkedList<VariableScope> variableScopeStack = new LinkedList<>();

        private Context(AnnotationNameResolver resolver, VariableFinalMarkerObservable observer) {
            this.resolver = resolver;
            this.observer = observer;
        }

        static Context of(VariableFinalMarkerObservable observer) {
            return new Context(AnnotationNameResolver.newResolver(), observer);
        }

        private void enterClass() {
            variableScopeStack.push(VariableScope.CLASS);
        }

        private void enterMethod() {
            variableScopeStack.push(VariableScope.METHOD);
        }

        private void enterBlock() {
            variableScopeStack.push(VariableScope.BLOCK);
        }

        private void enterForLoop() {
            variableScopeStack.push(VariableScope.FOR_LOOP);
        }

        private void leave() {
            variableScopeStack.pop();
        }

        private boolean isNotMethod() {
            return !VariableScope.METHOD.equals(variableScopeStack.peek());
        }
    }
}
