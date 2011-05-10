package org.finroc.core.thread;

import org.finroc.core.CoreFlags;
import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.FrameworkElementTreeFilter;
import org.finroc.core.RuntimeListener;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.AggregatedEdge;
import org.finroc.core.port.EdgeAggregator;
import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;

/** ThreadContainer thread class */
@SharedPtr
public class ThreadContainerThread extends CoreLoopThreadBase implements RuntimeListener, FrameworkElementTreeFilter.Callback<Boolean> {

    /** Thread container that thread belongs to */
    private final ThreadContainer threadContainer;

    /** true, when thread needs to make a new schedule before next run */
    private volatile boolean reschedule = true;

    /** simple schedule: Tasks will be executed in specified order */
    private SimpleList<PeriodicFrameworkElementTask> schedule = new SimpleList<PeriodicFrameworkElementTask>();

    /** temporary list of tasks that need to be scheduled */
    private SimpleList<PeriodicFrameworkElementTask> tasks = new SimpleList<PeriodicFrameworkElementTask>();

    /** temporary list of tasks that need to be scheduled - which are not sensor tasks */
    private SimpleList<PeriodicFrameworkElementTask> nonSensorTasks = new SimpleList<PeriodicFrameworkElementTask>();

    /** temporary variable for scheduling algorithm: trace we're currently following */
    private SimpleList<EdgeAggregator> trace = new SimpleList<EdgeAggregator>();

    /** temporary variable: trace back */
    private SimpleList<PeriodicFrameworkElementTask> traceBack = new SimpleList<PeriodicFrameworkElementTask>();

    /** tree filter to search for tasks */
    @PassByValue private final FrameworkElementTreeFilter filter = new FrameworkElementTreeFilter();

    /** temp buffer */
    @PassByValue private final StringBuilder tmp = new StringBuilder();

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"thread_container\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("thread_container");

    public ThreadContainerThread(ThreadContainer threadContainer, long defaultCycleTime, boolean warnOnCycleTimeExceed) {
        super(defaultCycleTime, warnOnCycleTimeExceed);
        this.threadContainer = threadContainer;
        this.setName("ThreadContainer " + threadContainer.getDescription());
    }

    @InCppFile
    public void run() {
        this.threadContainer.getRuntime().addListener(this);
        super.run();
    }

    @Override
    public void mainLoopCallback() throws Exception {
        if (reschedule) {
            reschedule = false;
            synchronized (this.threadContainer.getRegistryLock()) {

                // find tasks
                tasks.clear();
                nonSensorTasks.clear();
                schedule.clear();

                //JavaOnlyBlock
                filter.traverseElementTree(this.threadContainer, this, null, tmp);

                //Cpp filter.traverseElementTree(this->threadContainer, this, false, tmp);
                tasks.addAll(nonSensorTasks);

                // create task graph
                for (@SizeT int i = 0; i < tasks.size(); i++) {
                    PeriodicFrameworkElementTask task = tasks.get(i);

                    // trace outgoing connections
                    traceOutgoing(task, task.outgoing);
                }

                // now create schedule
                while (tasks.size() > 0) {

                    // do we have task without previous tasks?
                    boolean found = false;
                    for (@SizeT int i = 0; i < tasks.size(); i++) {
                        PeriodicFrameworkElementTask task = tasks.get(i);
                        if (task.previousTasks.size() == 0) {
                            schedule.add(task);
                            tasks.removeElem(task);
                            found = true;

                            // delete from next tasks' previous task list
                            for (@SizeT int j = 0; j < task.nextTasks.size(); j++) {
                                PeriodicFrameworkElementTask next = task.nextTasks.get(j);
                                next.previousTasks.removeElem(task);
                            }
                            break;
                        }
                    }
                    if (found) {
                        continue;
                    }

                    // ok, we didn't find module to continue with... (loop)
                    logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Detected loop: doing traceback");
                    traceBack.clear();
                    PeriodicFrameworkElementTask current = tasks.get(0);
                    traceBack.add(current);
                    while (true) {
                        boolean end = true;
                        for (@SizeT int i = 0; i < current.previousTasks.size(); i++) {
                            PeriodicFrameworkElementTask prev = current.previousTasks.get(i);
                            if (!traceBack.contains(prev)) {
                                end = false;
                                current = prev;
                                traceBack.add(current);
                                break;
                            }
                        }
                        if (end) {
                            logDomain.log(LogLevel.LL_WARNING, getLogDescription(), "Choosing " + current.incoming.getQualifiedName() + " as next element");
                            schedule.add(current);
                            tasks.removeElem(current);

                            // delete from next tasks' previous task list
                            for (@SizeT int j = 0; j < current.nextTasks.size(); j++) {
                                PeriodicFrameworkElementTask next = current.nextTasks.get(j);
                                next.previousTasks.removeElem(current);
                            }
                            break;
                        }
                    }
                }
            }
        }

        // execute tasks
        for (@SizeT int i = 0; i < schedule.size(); i++) {
            schedule.get(i).task.executeTask();
        }
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, Boolean unused) {
        if (ExecutionControl.find(fe).getAnnotated() != threadContainer) { // don't handle elements in nested thread containers
            return;
        }
        FinrocAnnotation ann = fe.getAnnotation(PeriodicFrameworkElementTask.TYPE);
        if (ann != null) {
            PeriodicFrameworkElementTask task = (PeriodicFrameworkElementTask)ann;
            task.previousTasks.clear();
            task.nextTasks.clear();
            if (task.isSenseTask()) {
                tasks.add(task);
            } else {
                nonSensorTasks.add(task);
            }
        }
    }

    /**
     * Trace outgoing connection
     *
     * @param task Task we're tracing from
     * @param outgoing edge aggregator with outgoing connections to follow
     */
    private void traceOutgoing(PeriodicFrameworkElementTask task, EdgeAggregator outgoing) {

        // add to trace stack
        trace.add(outgoing);

        @Ptr ArrayWrapper<AggregatedEdge> outEdges = outgoing.getEmergingEdges();
        for (@SizeT int i = 0; i < outEdges.size(); i++) {
            EdgeAggregator dest = outEdges.get(i).destination;
            if (!trace.contains(dest)) {

                // ok, have we reached another task?
                FinrocAnnotation ann = dest.getAnnotation(PeriodicFrameworkElementTask.TYPE);
                if (ann == null && isInterface(dest)) {
                    ann = dest.getParent().getAnnotation(PeriodicFrameworkElementTask.TYPE);
                }
                if (ann != null) {
                    PeriodicFrameworkElementTask task2 = (PeriodicFrameworkElementTask)ann;
                    if (!task.nextTasks.contains(task2)) {
                        task.nextTasks.add(task2);
                        task2.previousTasks.add(task);
                    }
                    continue;
                }

                // continue from this edge aggregator
                if (dest.getEmergingEdges().size() > 0) {
                    traceOutgoing(task, dest);
                } else if (isInterface(dest)) {
                    FrameworkElement parent = dest.getParent();
                    if (parent.getFlag(CoreFlags.EDGE_AGGREGATOR)) {
                        EdgeAggregator ea = (EdgeAggregator)parent;
                        if (!trace.contains(ea)) {
                            traceOutgoing(task, ea);
                        }
                    }
                    @PassByValue FrameworkElement.ChildIterator ci = new FrameworkElement.ChildIterator(parent, CoreFlags.READY | CoreFlags.EDGE_AGGREGATOR | EdgeAggregator.IS_INTERFACE);
                    FrameworkElement otherIf = null;
                    while ((otherIf = ci.next()) != null) {
                        EdgeAggregator ea = (EdgeAggregator)otherIf;
                        if (!trace.contains(ea)) {
                            traceOutgoing(task, ea);
                        }
                    }
                }
            }
        }

        // remove from trace stack
        assert(trace.get(trace.size() - 1) == outgoing);
        trace.remove(trace.size() - 1);
    }

    /**
     * @param fe Framework element
     * @return Is framework element an interface?
     */
    public boolean isInterface(FrameworkElement fe) {
        return fe.getFlag(CoreFlags.EDGE_AGGREGATOR | EdgeAggregator.IS_INTERFACE);
    }

    @Override @InCppFile
    public void runtimeChange(byte changeType, FrameworkElement element) {
        if (element.isChildOf(this.threadContainer)) {
            reschedule = true;
        }
    }

    @Override @InCppFile
    public void runtimeEdgeChange(byte changeType, AbstractPort source, AbstractPort target) {
        if (source.isChildOf(this.threadContainer) && target.isChildOf(this.threadContainer)) {
            reschedule = true;
        }
    }

    @Override
    public void stopThread() {
        synchronized (this.threadContainer.getRegistryLock()) {
            this.threadContainer.getRuntime().removeListener(this);
            super.stopThread();
        }
    }
}