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

import org.finroc.jc.MutexLockOrder;
import org.finroc.jc.Time;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppPrepend;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.CppUnused;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Mutable;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SharedPtr;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.container.BoundedQElementContainer;
import org.finroc.jc.container.ConcurrentMap;
import org.finroc.jc.container.SimpleList;
import org.finroc.jc.container.SimpleListWithMutex;
import org.finroc.jc.stream.ChunkedBuffer;
import org.finroc.log.LogLevel;

import org.finroc.core.admin.AdminServer;
import org.finroc.core.datatype.Constant;
import org.finroc.core.datatype.Unit;
import org.finroc.core.plugin.Plugins;
import org.finroc.core.port.ThreadLocalCache;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.rpc.MethodCallSyncher;
import org.finroc.core.port.stream.StreamCommitThread;
import org.finroc.core.portdatabase.SerializationHelper;
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

//  /*Cpp
//  // Static elements to delete after all elements in runtime environment
//  std::tr1::shared_ptr<util::SimpleList<finroc::util::Object*> > deleteLastList;
//   */

    /** Loop threads in this runtime */
    //private final SimpleList<CoreLoopThread> loopThreads = new SimpleList<CoreLoopThread>();

    /** Event threads in this runtime */
    //private final SimpleList<CoreEventThread> eventThreads = new SimpleList<CoreEventThread>();

    /** Global list of all ports (also place holders for those that are currently not available) */
    //private final Map<String, PortContainer> ports = new FastMap<String, PortContainer>();

    /**
     * Global list of all ports (also place holders for those that are currently not available)
     * allows accessing ports with simple index. This index is unique during runtime.
     * List will grow... somewhat memory leak like... but still with 100.000 different port uids it's only 400kb
     *
     * Thread-safe for iteration.
     */
    //private final FastTable<PortContainer> indexedPorts = new FastTable<PortContainer>();

    /** List with Ports that actually exists - thread-safe for iteration - may contain null entries */
    //private final SafeArrayList<Port<?>> existingPorts = new SafeArrayList<Port<?>>();

    /** Global list of all modules (also place holders for those that are currently not available) */
    //private final FastMap<String, ModuleContainer> modules = new FastMap<String, ModuleContainer>();

    /** Number of active modules */
    //private int activeModuleCount;

    /** All edges in runtime environment */
    //private final List<Edge> edges = new ArrayList<Edge>();

    /** Flexible edges in runtime environment */
    //private final List<FlexEdge> flexEdges = new ArrayList<FlexEdge>();

    /** Singleton instance of Runtime environment - shared pointer so that is cleanly deleted at shutdown */
    private static @SharedPtr RuntimeEnvironment instance;

    /** Raw pointer to above - that also exists during destruction */
    private static @Ptr RuntimeEnvironment instanceRawPtr = null;

    /** Runtime settings */
    //private final RuntimeSettings settings;

    /** Runtime listeners and Parameters */
    //private final ListenerManager listeners = new ListenerManager();
    //private final Byte ADD = 1, REMOVE = 2;

//  /** Links to framework elements */
//  private final ConcurrentMap<String, AbstractPort> links = new ConcurrentMap<String, AbstractPort>();

//  /** True, when Runtime environment is shutting down */
//  public static boolean shuttingDown = false;

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
        SerializationHelper.staticInit(); // can safely be done first
        Unit.staticInit(); // can safely be done first
        Constant.staticInit(); // needs to be done after unit
//      CoreNumber.staticInit(); // can be after data type register has been created
        Time.getInstance(); // (possibly) init timing thread
//      MethodCall.staticInit();
//      PullCall.staticInit();
        @CppType("util::SimpleListWithMutex<ThreadLocalCache*>")
        @SharedPtr SimpleListWithMutex<WeakReference<ThreadLocalCache>> infosLock = ThreadLocalCache.staticInit(); // can safely be done first
        MethodCallSyncher.staticInit(); // dito
        BoundedQElementContainer.staticInit();
        ChunkedBuffer.staticInit();
        StreamCommitThread.staticInit();
        //TransactionPacket.staticInit();

        //JavaOnlyBlock
        new RuntimeEnvironment(); // should be done before any ports/elements are added

        //Cpp new RuntimeEnvironment(); // should be done before any ports/elements are added
        instance.registry.infosLock = infosLock;

        // Start thread - because it needs a thread local cache
        StreamCommitThread.getInstance().start();

        // add uninitialized child
        instance.unrelated = new FrameworkElement("Unrelated", instance);
        @SuppressWarnings("unused")
        @CppUnused
        AdminServer as = new AdminServer();
        FrameworkElement.initAll();

        // init thread-local-cache for main thread */
        ThreadLocalCache.get();

        //ConfigFile.init(conffile);
        RuntimeSettings.staticInit(); // can be done now... or last

        //Load plugins
        Plugins.staticInit();
        //deleteLast(RuntimeSettings.getInstance());

        instance.setFlag(CoreFlags.READY);

        return instance;
    }

    /*Cpp
    virtual ~RuntimeEnvironment() {
        active = false;
        util::Thread::stopThreads();
        //shuttingDown = true;
        //util::GarbageCollector::deleteGarbageCollector(); // safer, this way
        deleteChildren();
        instanceRawPtr = NULL;

    //      // delete thread local caches mainly
    //      for (size_t i = 0; i < deleteLastList->size(); i++) {
    //          delete deleteLastList->get(i);
    //      }
    //      deleteLastList->clear();
    //      deleteLastList._reset();
        // stopStreamThread();
        // instance = NULL; will happen automatically
    }
     */

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

//  @SuppressWarnings("unused")
//  @InCppFile
//  private void stopStreamThread() {
//      StreamCommitThread.staticStop();
//  }

    //@Init("deleteLastList(new util::SimpleList<finroc::util::Object*>())")
    private RuntimeEnvironment() {
        super("Runtime", null, CoreFlags.ALLOWS_CHILDREN | CoreFlags.IS_RUNTIME, LockOrderLevels.RUNTIME_ROOT);
        assert instance == null;
        instance = this;
        instanceRawPtr = this;
        creationTime = Time.getPrecise();
        active = true;

//      for (int i = 0; i < RuntimeSettings.NUM_OF_LOOP_THREADS; i++) {
//          loopThreads.add(new CoreLoopThread(i, i < RuntimeSettings.SELF_UPDATING_LOOP_THREADS));
//      }
//      for (int i = 0; i < RuntimeSettings.NUM_OF_EVENT_THREADS; i++) {
//          eventThreads.add(new CoreEventThread(i));
//      }

        // init runtime settings
        //settings = new RuntimeSettings();
        //addChild(settings);

        // init Plugins etc.
        //Plugins.getInstance().addPluginsFromApplicationDir();
    }

    /**
     * Get Port container. Create if not yet existent.
     *
     * @param uid Port UID
     * @return Port container
     */
    /*public PortContainer getPort(String uid) {
        PortContainer result = ports.get(uid);
        if (result == null) {
            synchronized (ports) {  // make port container creation thread-safe - only part in code that modifies ports and indexedPorts
                result = ports.get(uid);
                if (result == null) {
                    result = new PortContainer(getModule(Module.getModulePartOfUID(uid)), uid);
                    ports.put(uid, result);
                    indexedPorts.add(result.getIndex(), result); // result.getIndex() should always be last index
                }
            }
        }
        return result;
    }*/

//  /**
//   * Adds edge
//   *
//   * @param sourceUid UID of source port
//   * @param destUid UID of destination port
//   * @return added edge
//   */
//  public Edge addEdge(String sourceUid, String destUid) {
//      Edge e = new Edge(sourceUid, destUid);
//      addEdge(e);
//      return e;
//  }

    /**
     * Add edge.
     *
     * @param e Edge
     */
//  public void addEdge(Edge e) {
//      synchronized(edges) {
//          edges.add(e);
//          e.register(getPort(e.getSourceUID()), getPort(e.getDestinationUID()));
//      }
//      reschedule();
//
//  }
//
//  /**
//   * remove Edge
//   *
//   * @param e Edge
//   */
//  public void removeEdge(Edge e) {
//      synchronized(edges) {
//          edges.remove(e);
//          e.unregister(edges);
//      }
//      reschedule();
//  }
//
//  /**
//   * Add flexbible edge.
//   *
//   * @param e Edge
//   */
//  public synchronized void addFlexEdge(FlexEdge e) {
//      flexEdges.add(e);
//      addListener(e, true);
//  }
//
//  /**
//   * remove flexible Edge
//   *
//   * @param e Edge
//   */
//  public synchronized void removeFlexEdge(FlexEdge e) {
//      removeListener(e);
//      flexEdges.remove(e);
//      e.delete();
//  }
//
//  /**
//   * Called by ports when they are initialized.
//   * Attaches them to PortContainer
//   *
//   * @param port Port to register
//   * @return PortContainer to which this port was attached
//   */
//  public synchronized <T extends PortData> PortContainer registerPort(Port<T> port) {
//      PortContainer pc = getPort(port.getUid());
//      port.setIndex(pc.getIndex());
//      pc.setPort(port);
//      if (port.isShared()) {
//          settings.sharedPorts.add(new PortInfo(pc));
//      }
//      existingPorts.add(port);
//      listeners.fireEvent(ADD, port);
//      reschedule();
//      return pc;
//  }
//
//  /**
//   * Called by ports when they are deleted.
//   * Detaches them from PortContainer
//   *
//   * @param port Port to unregister
//   */
//  public void unregisterPort(Port<?> port) {
//      listeners.fireEvent(REMOVE, port);
//      existingPorts.remove(port);
//      PortContainer pc = getPort(port.getUid());
//      if (pc.getPort() == port) {
//          pc.setPort(null);
//          if (port.isShared()) {
//              settings.sharedPorts.remove(port.getIndex());
//          }
//      }
//      reschedule();
//  }

//  /**
//   * @param index Index
//   * @return Loop Thread with specified index
//   */
//  public @Ref CoreLoopThread getLoopThread(int index) {
//      return loopThreads.get(index);
//  }
//
//  /**
//   * @param index Index
//   * @return Loop Thread with specified index
//   */
//  public @Ref CoreEventThread getEventThread(int index) {
//      return eventThreads.get(index);
//  }

//  /**
//   * Called by modules when they are initialized.
//   * Attaches them to ModuleContainer
//   *
//   * @param port Module to register
//   */
//  public void registerModule(Module module) {
//      ModuleContainer mc = getModule(module.getUid());
//      mc.setModule(module);
//      activeModuleCount++;
//      reschedule();
//  }
//
//  /**
//   * Get Module container. Create if not yet existent.
//   *
//   * @param uid Module UID
//   * @return Module container
//   */
//  public ModuleContainer getModule(String uid) {
//      ModuleContainer result = modules.get(uid);
//      if (result == null) {
//          synchronized (modules) {  // make port container creation thread-safe
//              result = modules.get(uid);
//              if (result == null) {
//                  result = new ModuleContainer(uid);
//                  modules.put(uid, result);
//              }
//          }
//      }
//      return result;
//  }
//
//  /**
//   * Called by modules when they are deleted.
//   * Detaches them from ModuleContainer
//   *
//   * @param port Module to unregister
//   */
//  public void unregisterModule(Module module) {
//      ModuleContainer mc = getModule(module.getUid());
//      if (mc.getModule() == module) {
//          mc.setModule(null);
//          activeModuleCount--;
//      }
//      reschedule();
//  }

//  /**
//   * Set reschedule flags for all loop threads
//   */
//  public void reschedule() {
//      for (int i = 0; i < loopThreads.size(); i++) {
//          loopThreads.get(i).setRescheduleFlag();
//      }
//  }
//

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

//  @Override
//  protected void serializeUid(CoreOutputStream oos, boolean firstCall) throws IOException {
//      return;  // do not include Runtime Description in UIDs
//  }
//
//  /**
//   * @return Returns unmodifiable list of Ports.
//   */
//  public List<PortContainer> getPorts() {
//      return indexedPorts.unmodifiable();
//  }
//
//  /**
//   * @param index Port Index
//   * @return Returns PortContainer with specified global index
//   */
//  public PortContainer getPort(int index) {
//      return indexedPorts.get(index);
//  }

//  /**
//   * @return Runtime Settings module
//   */
//  public RuntimeSettings getSettings() {
//      return settings;
//  }
//
//
//  private class ListenerManager extends WeakRefListenerManager<RuntimeListener> {
//
//      @Override
//      protected void notifyObserver(RuntimeListener observer, Object... param) {
//          if (param[0] == ADD) {
//              observer.portAdded((Port<?>)param[1]);
//          } else {
//              observer.portRemoved((Port<?>)param[1]);
//          }
//      }
//  }
//
//  /**
//   * @param l
//   * @see core.util.WeakRefListenerManager#addListener(java.util.EventListener)
//   */
//  public synchronized void addListener(RuntimeListener l, boolean applyExistingPorts) {
//      listeners.addListener(l);
//      if (applyExistingPorts) {
//          for (int i = 0, n = indexedPorts.size(); i < n; i++) {
//              Port<?> p = indexedPorts.get(i).getPort();
//              if (p != null) {
//                  l.portAdded(p);
//              }
//          }
//      }
//  }
//
//  /**
//   * @param l
//   * @see core.util.WeakRefListenerManager#removeListener(java.util.EventListener)
//   */
//  public synchronized void removeListener(RuntimeListener l) {
//      listeners.removeListener(l);
//  }
//
//  /**
//   * @return Unmodifiable List with Ports that actually exists - thread-safe for iteration - may contain null entries
//   */
//  public List<Port<?>> getExistingPorts() {
//      return existingPorts.getFastUnmodifiable();
//  }
//
//  /**
//   * @return the activeModuleCount
//   */
//  public int getActiveModuleCount() {
//      return activeModuleCount;
//  }
//
//  public void resetLoopThreads() {
//      for (FastMap.Entry<String, ModuleContainer> e = modules.head(), end = modules.tail(); (e = e.getNext()) != end;) {
//          e.getValue().setLoopThreadInfo(null);
//      }
//  }

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

//  /**
//   * (should only be called by thread local-cache)
//   * get port by raw index
//   *
//   * @param i Raw index
//   * @return Port
//   */
//  public AbstractPort getPortByRawIndex(int i) {
//      return ports.getByRawIndex(i);
//  }

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

//  /**
//   * (Should only be called by AbstractPort)
//   * Create link to port
//   *
//   * @param port Port
//   * @param linkName Name of link
//   */
//  public synchronized void link(AbstractPort port, String linkName) {
//      assert(!links.contains(linkName));
//      links.put(linkName, port);
//
//      // notify link listeners
//
//      for (@SizeT int i = 0; i < listeners.size(); i++) {
//          listeners.get(i).linkAdded(linkName, port);
//      }
//
//      // notify edges
//      LinkEdge interested = linkEdges.getPtr(linkName);
//      while(interested != null) {
//          interested.linkAdded(this, linkName, port);
//      }
//  }

//  /**
//   * (Should only be called by AbstractPort)
//   * Remove link to port
//   *
//   * @param linkName Name of link
//   */
//  public synchronized void removeLink(String linkName) {
//      AbstractPort ap = links.remove(linkName);
//
//      // notify link listeners
//      for (@SizeT int i = 0; i < linkListeners.size(); i++) {
//          linkListeners.get(i).linkRemoved(linkName, ap);
//      }
//  }

    /**
     * (usually only called by LinkEdge)
     * Add link edge that is interested in specific link
     *
     * @param link link that edge is interested in
     * @param edge Edge to add
     */
    protected void addLinkEdge(@Const @Ref String link, LinkEdge edge) {
        synchronized (registry) {
            LinkEdge interested = registry.linkEdges.get(link);
            if (interested == null) {
                // add first edge
                registry.linkEdges.put(link, edge);
            } else {
                // insert edge
                LinkEdge next = interested.getNext();
                interested.setNext(edge);
                edge.setNext(next);
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
                if (current.getNext() == null) { // remove entries for this link completely
                    registry.linkEdges.remove(link);
                } else { // remove first element
                    registry.linkEdges.put(link, current.getNext());
                }
            } else { // remove element out of linked list
                LinkEdge prev = current;
                current = current.getNext();
                while (current != null) {
                    if (current == edge) {
                        prev.setNext(current.getNext());
                        return;
                    }
                    prev = current;
                    current = current.getNext();
                }
                log(LogLevel.LL_DEBUG_WARNING, logDomain, "warning: Could not remove link edge for link: " + link);
            }
        }
    }

    /**
     * Remove linked edges from specified link to specified partner port
     *
     * @param link Link
     * @param partnerPort connected port
     */
    public void removeLinkEdge(@Const @Ref String link, AbstractPort partnerPort) {
        synchronized (registry) {
            for (LinkEdge current = registry.linkEdges.get(link); current != null; current = current.getNext()) {
                if (current.getPortHandle() == partnerPort.getHandle()) {
                    current.delete();
                    return;
                }
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
        FrameworkElement fe = handle >= 0 ? registry.ports.get(handle) : registry.elements.get(handle);
        if (fe == null) {
            return null;
        }
        return fe.isReady() ? fe : null;
    }

//  /**
//   * @return Iterator to iterate over links
//   */
//  public ConcurrentMap<String, AbstractPort>.MapIterator getLinkIterator() {
//      return links.getIterator();
//  }

//  /**
//   * Delete this object after all Framework elements
//   */
//  static void deleteLast(@Ptr Object t) {
//      /*Cpp
//      if (instance == NULL || instance->deleteLastList._get() == NULL) {
//          delete t;
//      } else {
//          instance->deleteLastList->add(t);
//      }
//      */
//  }

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
                        LinkEdge le = registry.linkEdges.get(s);
                        while (le != null) {
                            le.linkAdded(this, s, ap);
                            le = le.getNext();
                        }
                    }

                }

                registry.listeners.notify(element, null, changeType);
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
