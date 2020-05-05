package ristretto.compiler.plugin;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Log;
import ristretto.compiler.plugin.VariableFinalModifier.VariableScope;

public final class JavacPlugin implements Plugin {

    public static final String NAME = "ristretto";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        LogWrapper log = new LogWrapper(Log.instance(((BasicJavacTask) task).getContext()));
        MetricsCollector metrics = MetricsCollector.newCollector();

        task.addTaskListener(new OnParseFinished(metrics));
        task.addTaskListener(new OnCompilationFinished(metrics, log));

        log.notice(String.format("%s plugin loaded", NAME));
    }

    private static final class OnParseFinished implements TaskListener {

        final MetricsCollector collector;

        OnParseFinished(MetricsCollector collector) {
            this.collector = collector;
        }

        @Override
        public void finished(TaskEvent event) {
            if (!TaskEvent.Kind.PARSE.equals(event.getKind())) {
                return;
            }
            CompilationUnitTree compilationUnit = event.getCompilationUnit();
            compilationUnit.accept(VariableFinalModifier.newInstance(collector), null);
        }
    }

    private static final class OnCompilationFinished implements TaskListener {

        final MetricsCollector collector;
        final LogWrapper log;

        OnCompilationFinished(MetricsCollector collector, LogWrapper log) {
            this.collector = collector;
            this.log = log;
        }

        @Override
        public void finished(TaskEvent event) {
            if (!TaskEvent.Kind.COMPILATION.equals(event.getKind())) {
                return;
            }

            log.notice(formatMetrics(
                VariableScope.METHOD,
                "%s parameter(s) inspected (%s%% marked as final | %s%% skipped)",
                "0 parameters inspected"
            ));
            log.notice(formatMetrics(
                VariableScope.BLOCK,
                "%s local variable(s) inspected (%s%% marked as final | %s%% skipped)",
                "0 local variables inspected"
            ));
            log.notice(formatMetrics(
                VariableScope.CLASS,
                "%s field(s) inspected (%s%% marked as final | %s%% skipped)",
                "0 fields inspected"
            ));
        }

        private String formatMetrics(VariableScope scope, String whenPresent, String whenAbsent) {
            return collector.calculate(scope)
                .map(metrics ->
                    String.format(
                        whenPresent,
                        metrics.inspectedCount,
                        metrics.markedAsFinalPercentage,
                        metrics.skippedPercentage
                    )
                )
                .orElse(whenAbsent);
        }
    }

    private static final class LogWrapper {

        final Log log;

        private LogWrapper(Log log) {
            this.log = log;
        }

        void notice(String msg) {
            log.printRawLines(Log.WriterKind.NOTICE, msg);
        }
    }
}
