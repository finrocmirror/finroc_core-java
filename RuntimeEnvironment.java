/**
 * You received this file as part of an advanced experimental
 * robotics framework prototype ('finroc')
 *
 * Copyright (C) 2007-2010 Max Reichardt,
 *   Robotics Research Lab, University of Kaiserslautern
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.finroc.core;

import java.lang.ref.WeakReference;

import org.rrlib.finroc_core_utils.jc.GarbageCollector;
import org.rrlib.finroc_core_utils.jc.MutexLockOrder;
import org.rrlib.finroc_core_utils.jc.Time;
import org.rrlib.finroc_core_utils.jc.annotation.AtFront;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.CppPrepend;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.CppUnused;
import org.rrlib.finroc_core_utils.jc.annotation.ForwardDecl;
import org.rrlib.finroc_core_utils.jc.annotation.Friend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.JavaOnly;
import org.rrlib.finroc_core_utils.jc.annotation.Mutable;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SharedPtr;
import org.rrlib.finroc_core_utils.jc.annotation.SizeT;
import org.rrlib.finroc_core_utils.jc.container.BoundedQElementContainer;
import org.rrlib.finroc_core_utils.jc.container.ConcurrentMap;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.jc.container.SimpleListWithMutex;
import org.rrlib.finroc_core_utils.jc.stream.ChunkedBuffer;
import org.rrlib.finroc_core_utils.log.LogLevel;

import org.finroc.core.admin.AdminServer;
import org.finroc.core.datatype.Constant;
import org.finroc.core.datatype.Unit;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.rpc.MethodCallSyncher;
import org.finroc.core.port.stream.StreamCommitThread;
import org.finroc.core.portdatabase.DataTypeUtil;
import org.finroc.core.thread.ExecutionControl;

/**
 * @author max
 *
 * This is an application's main central class (root object). It contains groups and modules.
 */
/*@HPrepend({"class DeleteLastFunctor {",
           "public:",
           "    void operator()(finroc::util::Object* t);",
           "};"})*/
@CppPrepend( {/*"void DeleteLastFunctor::operator()(finroc::util::Object* t) {",
             "    RuntimeEnvironment::deleteLast(t);",
             "}","",*/
                 "RuntimeEnvironment::StaticDeleter::~StaticDeleter() {",
                 "    RuntimeEnvironment::shutdown();",
                 "}"
             })
@ForwardDecl( {ThreadLocalCache.class})
@Friend(LinkEdge.class)
public class RuntimeEnvironment extends FrameworkElement implements FrameworkElementTreeFilter.Callback<Boolean> { /*implements Runtime*/

    /**
     * Contains diverse registers/lookup tables of runtime.
     * They are moved to extra class in order to be separately lockable
     * (necessary for systematic dead-lock avoidance)
     */
    @AtFront @PassByValue @Friend(RuntimeEnvironment.class)
    public class Registry {

        /** Global register of all ports. Allows accessing ports with simple handle. */
        @SharedPtr private final CoreRegister<AbstractPort> ports = new CoreRegister<AbstractPort>(true);

        /** Global register of all framework elements (except of ports) */
        private final CoreRegister<FrameworkElement> elements = new CoreRegister<FrameworkElement>(false);

        /** Edges dealing with linked ports */
        //@Elems({PassByValue.class, Ptr.class, Ptr.class})
        private final ConcurrentMap<String, LinkEdge> linkEdges = new ConcurrentMap<String, LinkEdge>(null);

        /** List with runtime listeners */
        private final RuntimeListenerManager listeners = new RuntimeListenerManager();

        /** Temporary buffer - may be used in synchronized context */
        private StringBuilder tempBuffer = new StringBuilder();

        /** Lock to thread local cache list */
        @CppType("util::SimpleListWithMutex<ThreadLocalCache*>")
        @SharedPtr SimpleListWithMutex<WeakReference<ThreadLocalCache>> infosLock;

        /** Alternative roots for links (usually remote runtime environments mapped into this one) */
        private SimpleList<FrameworkElement> alternativeLinkRoots = new SimpleList<FrameworkElement>();

        /** Mutex */
        @Mutable
        public final MutexLockOrder objMutex = new MutexLockOrder(LockOrderLevels.RUNTIME_REGISTER);
    }

    /** Single final instance of above */
    private final Registry registry = new Registry();

    /** Singleton instance of Runtime environment - shared pointer so that is cleanly deleted at shutdown */
    private static @SharedPtr RuntimeEnvironment instance;

    /** Raw pointer to above - that also exists during destruction */
    private static @Ptr RuntimeEnvironment instanceRawPtr = null;

    /** Framework element that contains all framework elements that have no parent specified */
    FrameworkElement unrelated = null;

    /** Timestamp when runtime environment was created */
    private final long creationTime;

    /** Is RuntimeEnvironment currently active (and needs to be deleted?) */
    @SuppressWarnings("unused")
    private static boolean active = false;

    /** Mutex for static methods */
    @SuppressWarnings("unused")
    private static final MutexLockOrder staticClassMutex = new MutexLockOrder(LockOrderLevels.FIRST);

    /**
     * Initializes the runtime environment. Needs to be called before any
     * other operation (especially getInstance()) is called.
     *
     * @parem conffile Config file (see etc directory)
     * @return Singleton instance of Runtime environment
     */
    public synchronized static RuntimeEnvironment initialInit(/*InputStream conffile*/) {
        assert(!shuttingDown());

        // Finish initializing static members of classes
        Unit.staticInit(); // can safely be done first
        Constant.staticInit(); // needs to be done after unit
        Time.getInstance(); // (possibly) init timing thread
        GarbageCollector.createAndStartInstance();
        @CppType("util::SimpleListWithMutex<ThreadLocalCache*>")
        @SharedPtr SimpleListWithMutex<WeakReference<ThreadLocalCache>> infosLock = ThreadLocalCache.staticInit(); // can safely be done first
        MethodCallSyncher.staticInit(); // dito
        BoundedQElementContainer.staticInit();
        ChunkedBuffer.staticInit();
        DataTypeUtil.initCCTypes();

        //JavaOnlyBlock
        new RuntimeEnvironment(); // should be done before any ports/elements are added

        //Cpp new RuntimeEnvironment(); // should be done before any ports/elements are added
        instance.registry.infosLock = infosLock;

        // add uninitialized child
        instance.unrelated = new FrameworkElement(instance, "Unrelated");
        @SuppressWarnings("unused")
        @CppUnused
        AdminServer as = new AdminServer();

        // init thread-local-cache for main thread */
        ThreadLocalCache.get();

        //ConfigFile.init(conffile);
        RuntimeSettings.staticInit(); // can be done now... or last
        FrameworkElement.initAll();

        StreamCommitThread.staticInit();
        // Start thread
        StreamCommitThread.getInstance().start();

        //Load plugins
        Plugins.staticInit();
        //deleteLast(RuntimeSettings.getInstance());

        instance.setFlag(CoreFlags.READY);

        return instance;
    }

    public void delete() {
        super.delete();
        active = false;
        //Cpp util::Thread::stopThreads();

        // delete all children - (runtime settings last)
        ChildIterator ci = new ChildIterator(this, false);
        FrameworkElement next = null;
        while ((next = ci.next()) != null) {
            if (next != RuntimeSettings.getInstance()) {
                next.managedDelete();
            }
        }
        RuntimeSettings.getInstance().managedDelete();
        instanceRawPtr = null;
    }

    /**
     * (IMPORTANT: This should not be called during static initialization)
     *
     * @return Singleton instance of Runtime environment
     */
    public static RuntimeEnvironment getInstance() {
        if (instanceRawPtr == null) {
            //throw new RuntimeException("Runtime Environment not initialized");
            initialInit();
        }
        return instanceRawPtr;
    }

    private RuntimeEnvironment() {
        super(null, "Runtime", CoreFlags.ALLOWS_CHILDREN | CoreFlags.IS_RUNTIME, LockOrderLevels.RUNTIME_ROOT);
        assert instance == null;
        instance = this;
        instanceRawPtr = this;
        creationTime = Time.getPrecise();
        active = true;
    }

    /**
     * Start executing all Modules and Thread Containers in runtime
     */
    public void startExecution() {
        synchronized (registry) {
            @PassByValue FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter();
            fet.traverseElementTree(this, this, true);
        }
    }

    /**
     * Stop executing all Modules and Thread Containers in runtime
     */
    public void stopExecution() {
        synchronized (registry) {
            @PassByValue FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter();
            fet.traverseElementTree(this, this, false);
        }
    }

    @Override
    public void treeFilterCallback(FrameworkElement fe, Boolean start) {

        // callback from startExecution() or stopExecution()
        ExecutionControl ec = (ExecutionControl)fe.getAnnotation(ExecutionControl.TYPE);
        if (ec != null) {
            if (start) {
                ec.start();
            } else {
                ec.pause();
            }
        }
    }


    @JavaOnly public static void main(String[] args) {
        getInstance();
    }

    /**
     * Register framework element at RuntimeEnvironment.
     * This is done automatically and should not be called by a user.
     *
     * @param frameworkElement Element to register
     * @return Handle of Framework element
     */
    int registerElement(FrameworkElement fe) {
        synchronized (registry) {
            return fe.isPort() ? registry.ports.add((AbstractPort)fe) : registry.elements.add(fe);
        }
    }

    /**
     * Mark element as (soon completely) deleted at RuntimeEnvironment
     * This is done automatically and should not be called by a user.
     *
     * @param frameworkElement Element to mark deleted
     */
    void markElementDeleted(FrameworkElement fe) {
        synchronized (registry) {
            if (fe.isPort()) {
                registry.ports.markDeleted(fe.getHandle());
            } else {
                registry.elements.markDeleted(fe.getHandle());
            }
        }
    }

    /**
     * Unregister framework element at RuntimeEnvironment.
     * This is done automatically and should not be called by a user.
     *
     * @param frameworkElement Element to remove
     */
    void unregisterElement(FrameworkElement fe) {
        synchronized (registry) {
            if (fe.isPort()) {
                registry.ports.remove(fe.getHandle());
            } else {
                registry.elements.remove(fe.getHandle());
            }
        }
    }

    /**
     * get Port by handle
     *
     * @param portHandle port handle
     * @return Port - if port with such handle exists - otherwise null
     */
    public AbstractPort getPort(int portHandle) {
        AbstractPort p = registry.ports.get(portHandle);
        if (p == null) {
            return null;
        }
        return p.isReady() ? p : null;
    }


    /**
     * @param linkName (relative) Fully qualified name of port
     * @return Port with this name - or null if it does not exist
     */
    public AbstractPort getPort(@Const @Ref String linkName) {
        synchronized (registry) {

            FrameworkElement fe = getChildElement(linkName, false);
            if (fe == null) {
                for (@SizeT int i = 0; i < registry.alternativeLinkRoots.size(); i++) {
                    FrameworkElement altRoot = registry.alternativeLinkRoots.get(i);
                    fe = altRoot.getChildElement(linkName, 0, true, altRoot);
                    if (fe != null && !fe.isDeleted()) {
                        assert fe.isPort();
                        return (AbstractPort)fe;
                    }
                }
                return null;
            }
            assert fe.isPort();
            return (AbstractPort)fe;
        }
    }

    /**
     * (usually only called by LinkEdge)
     * Add link edge that is interested in specific link
     *
     * @param link link that edge is interested in
     * @param edge Edge to add
     */
    protected void addLinkEdge(@Const @Ref String link, LinkEdge edge) {
        edgeLog.log(LogLevel.LL_DEBUG_VERBOSE_1, getLogDescription(), "Adding link edge connecting to " + link);
        synchronized (registry) {
            LinkEdge interested = registry.linkEdges.get(link);
            if (interested == null) {
                // add first edge
                registry.linkEdges.put(link, edge);
            } else {
                // insert edge
                LinkEdge next = interested.getNextEdge();
                interested.setNextEdge(edge);
                edge.setNextEdge(next);
            }

            // directly notify link edge?
            AbstractPort p = getPort(link);
            if (p != null) {
                edge.linkAdded(this, link, p);
            }
        }
    }

    /**
     * (usually only called by LinkEdge)
     * Remove link edge that is interested in specific link
     *
     * @param link link that edge is interested in
     * @param edge Edge to add
     */
    protected void removeLinkEdge(@Const @Ref String link, LinkEdge edge) {
        synchronized (registry) {
            LinkEdge current = registry.linkEdges.get(link);
            if (current == edge) {
                if (current.getNextEdge() == null) { // remove entries for this link completely
                    registry.linkEdges.remove(link);
                } else { // remove first element
                    registry.linkEdges.put(link, current.getNextEdge());
                }
            } else { // remove element out of linked list
                LinkEdge prev = current;
                current = current.getNextEdge();
                while (current != null) {
                    if (current == edge) {
                        prev.setNextEdge(current.getNextEdge());
                        return;
                    }
                    prev = current;
                    current = current.getNextEdge();
                }
                log(LogLevel.LL_DEBUG_WARNING, logDomain, "warning: Could not remove link edge for link: " + link);
            }
        }
    }

    /**
     * Add runtime listener
     *
     * @param listener Listener to add
     */
    public void addListener(RuntimeListener listener) {
        synchronized (registry) {
            registry.listeners.add(listener);
        }
    }

    /**
     * Remove runtime listener
     *
     * @param listener Listener to remove
     */
    public void removeListener(RuntimeListener listener) {
        synchronized (registry) {
            registry.listeners.remove(listener);
        }
    }

    /**
     * Get Framework element from handle
     *
     * @param handle Handle of framework element
     * @return Pointer to framework element - or null if it has been deleted
     */
    public FrameworkElement getElement(int handle) {
        if (handle == this.getHandle()) {
            return this;
        }
        FrameworkElement fe = handle >= 0 ? registry.ports.get(handle) : registry.elements.get(handle);
        if (fe == null) {
            return null;
        }
        return fe.isReady() ? fe : null;
    }

    /**
     * Called before a framework element is initialized - can be used to create links etc. to this element etc.
     *
     * @param element Framework element that will be initialized soon
     */
    void preElementInit(FrameworkElement element) {
        synchronized (registry) {
            registry.listeners.notify(element, null, RuntimeListener.PRE_INIT);
        }
    }

    /**
     * Called whenever a framework element was added/removed or changed
     *
     * @param changeType Type of change (see Constants in Transaction class)
     * @param element FrameworkElement that changed
     * @param edgeTarget Target of edge, in case of EDGE_CHANGE
     *
     * (Is called in synchronized (Runtime & Element) context in local runtime... so method should not block)
     * (should only be called by FrameworkElement class)
     */
    void runtimeChange(byte changeType, FrameworkElement element, AbstractPort edgeTarget) {
        synchronized (registry) {
            if (!shuttingDown()) {

                if (element.getFlag(CoreFlags.ALTERNATE_LINK_ROOT)) {
                    if (changeType == RuntimeListener.ADD) {
                        registry.alternativeLinkRoots.add(element);
                    } else if (changeType == RuntimeListener.REMOVE) {
                        registry.alternativeLinkRoots.removeElem(element);
                    }
                }

                if (changeType == RuntimeListener.ADD && element.isPort()) { // check links
                    AbstractPort ap = (AbstractPort)element;
                    for (@SizeT int i = 0; i < ap.getLinkCount(); i++) {
                        ap.getQualifiedLink(registry.tempBuffer, i);
                        String s = registry.tempBuffer.toString();
                        edgeLog.log(LogLevel.LL_DEBUG_VERBOSE_2, getLogDescription(), "Checking link " + s + " with respect to link edges");
                        LinkEdge le = registry.linkEdges.get(s);
                        while (le != null) {
                            le.linkAdded(this, s, ap);
                            le = le.getNextEdge();
                        }
                    }

                }

                registry.listeners.notify(element, edgeTarget, changeType);
            }
        }
    }

    /**
     * (irrelevant for Java - we need no memory cleanup there)
     *
     * @return Is runtime environment currently shutting down?
     */
    @InCpp("return util::Thread::stoppingThreads();")
    public static boolean shuttingDown() {
        return instance != null && instance.isDeleted();
    }

    /**
     * @return Timestamp when runtime environment was created
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Using only the basic constructs from this framework - things should shutdown
     * cleanly without calling anything.
     *
     * However, if things like the TCP server are used, this method should be called
     * at the end of the program in order to shut everything down cleanly.
     */
    public static void shutdown() {
        /*Cpp
        util::Thread::stopThreads();
        if (active) {
            instance._reset();
        }
         */
    }

    /**
     * (Should only be called by ThreadLocalCache class - needed for clean cleanup - port register needs to exists longer than runtime environment)
     * @return Port register
     */
    @SharedPtr @Const public CoreRegister<AbstractPort> getPorts() {
        return registry.ports;
    }

    /**
     * @param name Name of command line argument
     * @return Value of argument - or "" if not set.
     */
    public String getCommandLineArgument(String name) {
        // TODO
        return "";
    }

    /**
     * @return Lock order of registry
     */
    @Const @Ptr Registry getRegistryHelper() {
        return registry;
    }

    /*Cpp

    //! Can be placed in classes in order to ensure that RuntimeEnvironment will be deleted before its static members
    class StaticDeleter {
    public:
        ~StaticDeleter();
    };
    */
}
