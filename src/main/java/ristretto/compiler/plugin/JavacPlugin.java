package ristretto.compiler.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import static ristretto.compiler.plugin.TaskListeners.whenEventKindIs;
import static ristretto.compiler.plugin.TaskListeners.whenPackageName;

public final class JavacPlugin implements Plugin {

    public static final String NAME = "ristretto";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        RistrettoOptions options = RistrettoOptions.parse(args);
        DiagnosticsReport diagnosticsReport;

        if (options.isStandardErrorOutputEnabled()) {
            Context context = ((BasicJavacTask) task).getContext();
            diagnosticsReport = new DiagnosticsReport(RistrettoLogger.stderr(Log.instance(context)));
        } else {
            diagnosticsReport = new DiagnosticsReport(RistrettoLogger.javaUtilLogging());
        }

        task.addTaskListener(TaskListeners.onFinished(
            whenEventKindIs(TaskEvent.Kind.PARSE).and(whenPackageName(options::isIncluded)),
            event -> {
                var compilationUnit = event.getCompilationUnit();
                var report = diagnosticsReport.withJavaFile(compilationUnit.getSourceFile());
                var nameResolver = new AnnotationNameResolver(ImportDeclaration.of(compilationUnit.getImports()));

                compilationUnit.accept(new DefaultImmutabilityRule(nameResolver, report), Scope.COMPILATION_UNIT);
                compilationUnit.accept(new DefaultPrivateAccessRule(nameResolver, report), Scope.COMPILATION_UNIT);
            }
        ));

        task.addTaskListener(TaskListeners.onFinished(
            whenEventKindIs(TaskEvent.Kind.COMPILATION),
            diagnosticsReport::pluginFinished
        ));

        diagnosticsReport.pluginLoaded();
    }
}
