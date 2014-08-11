//
// You received this file as part of Finroc
// A framework for intelligent robot control
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.finroc.core;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.rrlib.finroc_core_utils.jc.GarbageCollector;
import org.rrlib.finroc_core_utils.jc.MutexLockOrder;
import org.rrlib.finroc_core_utils.jc.Time;
import org.rrlib.finroc_core_utils.jc.container.BoundedQElementContainer;
import org.rrlib.finroc_core_utils.jc.stream.ChunkedBuffer;
import org.rrlib.logging.Log;
import org.rrlib.logging.LogLevel;

import org.finroc.core.admin.AdminServer;
import org.finroc.core.datatype.CoreNumber;
import org.finroc.core.datatype.CoreString;
import org.finroc.core.datatype.XML;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.stream.StreamCommitThread;
import org.finroc.core.portdatabase.DataTypeUtil;
import org.finroc.core.remote.RemoteTypeAdapter;
import org.finroc.core.thread.ExecutionControl;

/**
 * @author Max Reichardt
 *
 * This is an application's main central class (root object). It contains groups and modules.
 */
public class RuntimeEnvironment extends FrameworkElement implements FrameworkElementTreeFilter.Callback<Boolean> { /*implements Runtime*/

    /**
     * Contains diverse registers/lookup tables of runtime.
     * They are moved to extra class in order to be separately lockable
     * (necessary for systematic dead-lock avoidance)
     */
    public class Registry {

        /** Global register of all ports. Allows accessing ports with simple handle. */
        private final CoreRegister<AbstractPort> ports = new CoreRegister<AbstractPort>(true);

        /** Global register of all framework elements (except of ports) */
        private final CoreRegister<FrameworkElement> elements = new CoreRegister<FrameworkElement>(false);

        /** Edges dealing with linked ports */
        private final ConcurrentHashMap<String, LinkEdge> linkEdges = new ConcurrentHashMap<String, LinkEdge>();

        /** List with runtime listeners */
        private final RuntimeListenerManager listeners = new RuntimeListenerManager();

        /** Temporary buffer - may be used in synchronized context */
        private StringBuilder tempBuffer = new StringBuilder();

        /** Lock to thread local cache list */
        ArrayList<WeakReference<ThreadLocalCache>> infosLock;

        /** Alternative roots for links (usually remote runtime environments mapped into this one) */
        private ArrayList<FrameworkElement> alternativeLinkRoots = new ArrayList<FrameworkElement>();

        /** Mutex */
        public final MutexLockOrder objMutex = new MutexLockOrder(LockOrderLevels.RUNTIME_REGISTER);
    }

    /** Single final instance of above */
    private final Registry registry = new Registry();

    /** Singleton instance of Runtime environment - shared pointer so that is cleanly deleted at shutdown */
    private static RuntimeEnvironment instance;

    /** Raw pointer to above - that also exists during destruction */
    private static RuntimeEnvironment instanceRawPtr = null;

    /** Framework element that contains all framework elements that have no parent specified */
    FrameworkElement unrelated = null;

    /** Timestamp when runtime environment was created */
    private final long creationTime;

    /** Name of program/process (setting it is optional) */
    private String programName;

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
        Time.getInstance(); // (possibly) init timing thread
        GarbageCollector.createAndStartInstance();
        ArrayList<WeakReference<ThreadLocalCache>> infosLock = ThreadLocalCache.staticInit(); // can safely be done first
        BoundedQElementContainer.staticInit();
        ChunkedBuffer.staticInit();
        DataTypeUtil.initCCTypes();
        new RemoteTypeAdapter.Default();

        new RuntimeEnvironment(); // should be done before any ports/elements are added

        instance.registry.infosLock = infosLock;

        // add uninitialized child
        instance.unrelated = new FrameworkElement(instance, "Unrelated");
        @SuppressWarnings("unused")
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

        instance.setFlag(Flag.READY);

        if (RuntimeSettings.ANDROID_PLATFORM) {
            new CoreString();
            new XML();
            new CoreNumber();
        }

        return instance;
    }

    public void delete() {
        super.delete();
        active = false;

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
        super(null, "Runtime", Flag.RUNTIME, LockOrderLevels.RUNTIME_ROOT);
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
            FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter();
            fet.traverseElementTree(this, this, true);
        }
    }

    /**
     * Stop executing all Modules and Thread Containers in runtime
     */
    public void stopExecution() {
        synchronized (registry) {
            FrameworkElementTreeFilter fet = new FrameworkElementTreeFilter();
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


    public static void main(String[] args) {
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
    public AbstractPort getPort(String linkName) {
        synchronized (registry) {

            FrameworkElement fe = getChildElement(linkName, false);
            if (fe == null) {
                for (int i = 0; i < registry.alternativeLinkRoots.size(); i++) {
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
    protected void addLinkEdge(String link, LinkEdge edge) {
        Log.log(LogLevel.DEBUG_VERBOSE_1, this, "Adding link edge connecting to " + link);
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
    protected void removeLinkEdge(String link, LinkEdge edge) {
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
                Log.log(LogLevel.DEBUG_WARNING, this, "Could not remove link edge for link: " + link);
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

                if (element.getFlag(Flag.ALTERNATIVE_LINK_ROOT)) {
                    if (changeType == RuntimeListener.ADD) {
                        registry.alternativeLinkRoots.add(element);
                    } else if (changeType == RuntimeListener.REMOVE) {
                        registry.alternativeLinkRoots.remove(element);
                    }
                }

                if (changeType == RuntimeListener.ADD && element.isPort()) { // check links
                    AbstractPort ap = (AbstractPort)element;
                    for (int i = 0; i < ap.getLinkCount(); i++) {
                        ap.getQualifiedLink(registry.tempBuffer, i);
                        String s = registry.tempBuffer.toString();
                        Log.log(LogLevel.DEBUG_VERBOSE_2, this, "Checking link " + s + " with respect to link edges");
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
    }

    /**
     * (Should only be called by ThreadLocalCache class - needed for clean cleanup - port register needs to exists longer than runtime environment)
     * @return Port register
     */
    public CoreRegister<AbstractPort> getPorts() {
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
    Registry getRegistryHelper() {
        return registry;
    }

    /**
     * @return Name of program/process (setting it is optional)
     */
    public String getProgramName() {
        return programName;
    }

    /**
     * @param programName Name of program/process
     */
    public void setProgramName(String programName) {
        this.programName = programName;
    }
}
