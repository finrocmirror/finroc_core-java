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

import org.rrlib.finroc_core_utils.jc.ArrayWrapper;
import org.rrlib.finroc_core_utils.jc.GarbageCollector;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.MutexLockOrder;
import org.rrlib.finroc_core_utils.jc.container.SafeConcurrentlyIterableList;
import org.rrlib.finroc_core_utils.jc.container.SimpleList;
import org.rrlib.finroc_core_utils.jc.container.SimpleListWithMutex;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.jc.thread.ThreadUtil;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.log.LogLevel;
import org.rrlib.finroc_core_utils.log.LogStream;
import org.rrlib.finroc_core_utils.serialization.OutputStreamBuffer;

import org.finroc.core.parameter.ConfigFile;
import org.finroc.core.parameter.ConfigNode;
import org.finroc.core.parameter.ConstructorParameters;
import org.finroc.core.parameter.StaticParameterBase;
import org.finroc.core.parameter.StaticParameterList;
import org.finroc.core.plugin.CreateFrameworkElementAction;
import org.finroc.core.port.AbstractPort;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author Max Reichardt
 *
 * Base functionality of Ports, PortSets, Modules, Groups and the Runtime.
 *
 * When dealing with unknown framework elements - check isReady() to make sure
 * they are fully initialized and not already deleted.
 * Init needs to be called before framework elements can be used, as well as being visible
 * to remote runtime environments/clients.
 *
 * Framework elements are arranged in a tree.
 * They may be linked/referenced from other parts of the tree.
 *
 * Everything is thread-safe as long as methods are used.
 *
 * To prevent deleting of framework element while using it over a longer period of time,
 * lock it - or the complete runtime environment.
 */
public class FrameworkElement extends Annotatable {

    /** Uid of thread that created this framework element */
    protected final long createrThreadUid;

    // Splitting flags up might allow compiler optimizations??

    /** Flags - see FrameworkElementFlags */
    protected int flags;

    public static class Flag extends FrameworkElementFlags {}; // instead of 'typedef FrameworkElementFlags Flag' in C++

    /**
     * @author Max Reichardt
     *
     * Framework elements are inserted as children of other framework element using this connector class.
     * Ratio: this allows links in the tree
     *
     * Framework elements "own" links - they are deleted with framework element
     */
    public class Link implements HasDestructor {

        /** Name of Framework Element - in link context */
        private String name;

        /** Parent - Element in which this link was inserted */
        private FrameworkElement parent;

        /** Next link for this framework element (=> singly-linked list) */
        private Link next;

        /**
         * @return Element that this link points to
         */
        public FrameworkElement getChild() {
            return FrameworkElement.this;
        }

        /**
         * @return Name of Framework Element - in link context
         */
        public String getName() {
            return name;
        }

        /**
         * @return Parent - Element in which this link was inserted
         */
        public FrameworkElement getParent() {
            return parent;
        }

        /**
         * @return Is this a primary link?
         */
        public boolean isPrimaryLink() {
            return this == getChild().primary;
        }

        @Override
        public void delete() {}

        public String toString() {
            return name;
        }
    }

    /** Primary link to framework element - the place at which it actually is in FrameworkElement tree - contains name etc. */
    private final Link primary = new Link();

    /** children - may contain null entries (for efficient thread-safe unsynchronized iteration) */
    protected final SafeConcurrentlyIterableList<Link> children;

    /**
     * Defines lock order in which framework elements can be locked.
     * Generally the framework element tree is locked from root to leaves.
     * So children's lock level needs to be larger than their parent's.
     *
     * The secondary component is the element's unique handle in local runtime environment.
     * ("normal" elements have negative handle, while ports have positive ones)
     */
    public final MutexLockOrder objMutex;

    /**
     * Extra Mutex for changing flags
     */
    private final Object flagMutex = new Object();

    /** Log domain for this class */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("framework_elements");

    /** Log domain for edges */
    public static final LogDomain edgeLog = LogDefinitions.finroc.getSubDomain("edges");

    public FrameworkElement(FrameworkElement parent, String name) {
        this(parent, name, 0, -1);
    }

    /**
     * @param name Name of framework element (will be shown in browser etc.) - may not be null
     * @param parent_ Parent of framework element (only use non-initialized parent! otherwise null and addChild() later; meant only for convenience)
     * @param flags_ Any special flags for framework element
     * @param lockOrder_ Custom value for lock order (needs to be larger than parent's) - negative indicates unused.
     */
    @SuppressWarnings("unchecked")
    public FrameworkElement(FrameworkElement parent_, String name, int flags_, int lockOrder_) {
        createrThreadUid = ThreadUtil.getCurrentThreadId();
        flags = flags_;
        assert((flags_ & Flag.STATUS_FLAGS) == 0);

        if (name == null) {
            name = "";
        }

        primary.name = name;

        objMutex = new MutexLockOrder(getLockOrder(flags_, parent_, lockOrder_), getFlag(Flag.RUNTIME) ? Integer.MIN_VALUE : RuntimeEnvironment.getInstance().registerElement(this));

        //treeNode = RuntimeSettings.CREATE_TREE_NODES_FOR_RUNTIME_ELEMENTS.get() ? createTreeNode() : null;
        // save memory in case of port
        children = (!getFlag(Flag.PORT)) ? new SafeConcurrentlyIterableList<Link>(4, 4) :
                   SafeConcurrentlyIterableList.getEmptyInstance();

        if (!getFlag(Flag.RUNTIME)) {
            FrameworkElement parent = (parent_ != null) ? parent_ : RuntimeEnvironment.getInstance().unrelated;
            if (lockOrder_ < 0) {
                lockOrder_ = parent.getLockOrder() + 1;
            }
            parent.addChild(primary);
        }

//      if (parent != null) {
//          assert !parent.isInitialized() : "shouldn't add half-constructed objects to initialized/operating parent - could cause concurrency problems in init process";
//          parent.addChild(primary);
//      }

        log(LogLevel.DEBUG_VERBOSE_1, logDomain, "Constructing FrameworkElement");
    }

    /**
     * Helper for constructor (required for initializer-list in C++)
     *
     * @return Primary lock order
     */
    private static int getLockOrder(int flags, FrameworkElement parent, int lockOrder) {
        if ((flags & Flag.RUNTIME) == 0) {
            parent = (parent != null) ? parent : RuntimeEnvironment.getInstance().unrelated;
            if (lockOrder < 0) {
                return parent.getLockOrder() + 1;
            }
            return lockOrder;
        } else {
            return lockOrder;
        }
    }

    public FrameworkElement(String name) {
        this(null, name);
    }

    public FrameworkElement() {
        this(null, "");
    }

    /**
     * @param flag Flag to set
     * @param value Value to set value to
     */
    protected void setFlag(int flag, boolean value) {
        if (value) {
            setFlag(flag);
        } else {
            removeFlag(flag);
        }
    }

    /**
     * (Needs to be synchronized because change operation is not atomic).
     *
     * @param flag Flag to set
     */
    protected void setFlag(int flag) {
        synchronized (flagMutex) {
            assert(flag & Flag.CONSTANT_FLAGS) == 0;
            flags |= flag;
        }
    }

    /**
     * (Needs to be synchronized because change operation is not atomic).
     *
     * @param flag Flag to remove
     */
    protected void removeFlag(int flag) {
        synchronized (flagMutex) {
            assert(flag & Flag.CONSTANT_FLAGS) == 0;
            flags &= ~flag;
        }
    }

    /**
     * Is specified flag set?
     * for convenience - don't use in places with absolute maximum performance requirements (?)
     *
     * @param flag Flag to check
     * @return Answer
     */
    public boolean getFlag(int flag) {
        return (flags & flag) == flag;
    }

    /**
     * @return Name of this framework element
     */
    public String getName() {
        if (isReady() || getFlag(Flag.RUNTIME)) {
            return primary.name.length() == 0 ? "(anonymous)" : primary.name;
        } else {
            synchronized (getRegistryLock()) { // synchronize, while name can be changed (C++ strings may not be thread safe...)
                if (isDeleted()) {
                    return "(deleted element)";
                }
                return primary.name.length() == 0 ? "(anonymous)" : primary.name;
            }
        }
    }

    /**
     * Write name of link number i to Output stream
     *
     * @param os OutputStream
     * @param i Link Number (0 is primary link/name)
     */
    public void writeName(OutputStreamBuffer os, int i) {
        if (isReady()) {
            os.writeString(getLink(i).name);
        } else {
            synchronized (getRegistryLock()) { // synchronize, while name can be changed (C++ strings may not be thread safe...)
                os.writeString(isDeleted() ? "deleted element" : getLink(i).name);
            }
        }
    }

    /**
     * @param name New Port name
     * (only valid/possible before, element is initialized)
     */
    public void setName(String name) {
        assert(!getFlag(Flag.RUNTIME));
        synchronized (getRegistryLock()) { // synchronize, C++ strings may not be thread safe...
            assert(isConstructing());
            assert(isCreator());
            primary.name = name;
        }
    }

    /**
     * @param name New Port name
     * @param linkNo Index of link to set name of
     * (only valid/possible before, element is initialized)
     */
    public void setName(String name, int linkIndex) {
        assert(!getFlag(Flag.RUNTIME));
        synchronized (getRegistryLock()) { // synchronize, C++ strings may not be thread safe...
            assert(isConstructing());
            assert(isCreator());
            getLink(linkIndex).name = name;
        }
    }

    /**
     * @return Is current thread the thread that created this object?
     */
    private boolean isCreator() {
        return ThreadUtil.getCurrentThreadId() == createrThreadUid;
    }

    public String toString() {
        return getName();
    }

    /**
     * @return Is element a port?
     */
    public boolean isPort() {
        return (flags & Flag.PORT) > 0;
    }

    /**
     * @return Is framework element ready/fully initialized and not yet deleted?
     */
    public boolean isReady() {
        return (flags & Flag.READY) > 0;
    }

    /**
     * @return Has framework element been deleted? - dangerous if you actually encounter this in C++...
     */
    public boolean isDeleted() {
        return (flags & Flag.DELETED) > 0;
    }

    /**
     * Add Child to framework element
     * (It will be initialized as soon as this framework element is - or instantly if this has already happened)
     *
     * @param fe Framework element to add (must not have been initialized already - structure is fixes in this case)
     */
    public void addChild(FrameworkElement fe) {
        addChild(fe.primary);
    }

    /**
     * Adds child to parent (automatically called by constructor - may be called again though)
     * using specified link
     *
     * - Removes child from any former parent
     * - Init() method of child is not called - this has to be done
     *   separately prior to operation
     *
     * @param Link link to child to use
     */
    private void addChild(Link child) {

        if (child.parent == this) {
            return;
        }

        // lock runtime (required to perform structural changes)
        synchronized (getRegistryLock()) {

            // perform checks
            assert(child.getChild().isConstructing()) : "tree structure is fixed for initialized children - is child initialized twice (?)";
            assert(child.getChild().isCreator()) : "may only be called by child creator thread";
            if (isDeleted() || (child.parent != null && child.parent.isDeleted()) || child.getChild().isDeleted()) {
                throw new RuntimeException("Child has been deleted or has deleted parent. Thread exit is likely the intended behaviour.");
            }
            if (child.parent != null) {
                assert(child.getChild().lockAfter(child.parent)) : "lockOrder level of child needs to be higher than of former parent";
            }
            assert(child.getChild().lockAfter(this)) : "lockOrder level of child needs to be higher than of parent";
            // avoid cycles
            assert child.getChild() != this;
            assert(!this.isChildOf(child.getChild()));

            // detach from former parent
            if (child.parent != null) {
                //assert(!child.parent.isInitialized()) : "This is truly strange - should not happen";
                child.parent.children.remove(child);

                // JavaOnlyBlock
                /*if (treeNode != null) {
                    child.parent.treeNode.remove(treeNode);
                }*/
            }

            // Check if child with same name already exists and possibly rename (?)
            if (getFlag(Flag.AUTO_RENAME) && (!getFlag(Flag.PORT)) && getChild(child.getName()) != null) {
                String pointerBuffer = " (" + child.getChild().hashCode() + ")";
                child.getChild().setName(child.getChild().getName() + pointerBuffer);
                while (getChild(child.getName()) != null) {
                    log(LogLevel.DEBUG_WARNING, logDomain, "Spooky framework elements name duplicates: " + child.getName());
                    child.getChild().setName(child.getChild().getName() + pointerBuffer);
                }
            }

            child.parent = this;
            children.add(child, false);
            // child.init(); - do this separately

            // JavaOnlyBlock
            /*if (treeNode != null) {
                 treeNode.add(child.getChild().treeNode);
            }*/
        }
    }

    /**
     * Can this framework element be locked after the specified one has been locked?
     *
     * @param fe Specified other framework element
     * @return Answer
     */
    public boolean lockAfter(FrameworkElement fe) {
        return objMutex.validAfter(fe.objMutex);
    }

    /**
     * @param name (relative) Qualified name
     * @param onlyGloballyUniqueChildren Only return child with globally unique link?
     * @return Framework element - or null if non-existent
     */
    public FrameworkElement getChildElement(String name, boolean onlyGloballyUniqueChildren) {
        return getChildElement(name, 0, onlyGloballyUniqueChildren, RuntimeEnvironment.getInstance());
    }

    /**
     * Helper for above
     *
     * @param name (relative) Qualified name
     * @param nameIndex Current index in string
     * @param onlyGloballyUniqueChildren Only return child with globally unique link?
     * @param Link root
     * @return Framework element - or null if non-existent
     */
    protected FrameworkElement getChildElement(String name, int nameIndex, boolean onlyGloballyUniqueChildren, FrameworkElement root) {

        // lock runtime (might not be absolutely necessary... ensures, however, that result is valid)
        synchronized (getRegistryLock()) {

            if (isDeleted()) {
                return null;
            }

            if (name.charAt(nameIndex) == '/') {
                return root.getChildElement(name, nameIndex + 1, onlyGloballyUniqueChildren, root);
            }

            onlyGloballyUniqueChildren &= (!getFlag(Flag.GLOBALLY_UNIQUE_LINK));
            ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                Link child = iterable.get(i);
                if (child != null && name.regionMatches(nameIndex, child.name, 0, child.name.length()) && (!child.getChild().isDeleted())) {
                    if (name.length() == nameIndex + child.name.length()) {
                        if (!onlyGloballyUniqueChildren || child.getChild().getFlag(Flag.GLOBALLY_UNIQUE_LINK)) {
                            return child.getChild();
                        }
                    }
                    if (name.charAt(nameIndex + child.name.length()) == '/') {
                        FrameworkElement result = child.getChild().getChildElement(name, nameIndex + child.name.length() + 1, onlyGloballyUniqueChildren, root);
                        if (result != null) {
                            return result;
                        }
                        // continue, because links may contain '/'... (this is slightly ugly... better solution? TODO)
                    }
                }
            }
            return null;

        }
    }

    /**
     * Create link to this framework element
     *
     * @param parent Parent framework element
     * @param linkName name of link
     */
    protected void link(FrameworkElement parent, String linkName) {
        assert(isCreator()) : "may only be called by creator thread";
        assert(lockAfter(parent));

        // lock runtime (required to perform structural changes)
        synchronized (getRegistryLock()) {
            if (isDeleted() || parent.isDeleted()) {
                throw new RuntimeException("Element and/or parent has been deleted. Thread exit is likely the intended behaviour.");
            }

            Link l = new Link();
            l.name = linkName;
            l.parent = null; // will be set in addChild
            Link lprev = getLinkInternal(getLinkCount() - 1);
            assert lprev.next == null;
            lprev.next = l;
            parent.addChild(l);
            //RuntimeEnvironment.getInstance().link(this, linkName);
        }
    }

    public void delete() {
        super.delete();
        assert(getFlag(Flag.DELETED) || getFlag(Flag.RUNTIME)) : "Frameworkelement was not deleted with managedDelete()";
        log(LogLevel.DEBUG_VERBOSE_1, logDomain, "FrameworkElement destructor");
        if (!getFlag(Flag.RUNTIME)) {
            // synchronizes on runtime - so no elements will be deleted while runtime is locked
            RuntimeEnvironment.getInstance().unregisterElement(this);
        }

        // delete links
        Link l = primary.next;
        while (l != null) {
            @SuppressWarnings("unused")
            Link tmp = l;
            l = l.next;
            //Cpp delete tmp;
        }
    }

    /**
     * Initialize this framework element and all framework elements in sub tree that were created by this thread
     * and weren't initialized already.
     *
     * This must be called prior to using framework elements - and in order to them being published.
     */
    public void init() {
        synchronized (getRuntime().getRegistryLock()) {
            //SimpleList<FrameworkElement> publishThese = new SimpleList<FrameworkElement>();
            // assert(getFlag(CoreFlags.IS_RUNTIME) || getParent().isReady());
            if (isDeleted()) {
                throw new RuntimeException("Cannot initialize deleted element");
            }

            initImpl();

            checkPublish();

            /*for (@SizeT int i = 0, n = publishThese.size(); i < n; i++) {
                publishThese.get(i).publishUpdatedInfo(RuntimeListener.ADD);
            }*/
        }
    }

    /**
     * Helper method for deleting.
     * For some elements we need to lock runtime registry before locking this element.
     *
     * @return Returns runtime registry if this is the case - otherwise this-pointer.
     */
    private Object runtimeLockHelper() {

        if (objMutex.validAfter(getRuntime().getRegistryHelper().objMutex)) {
            return getRegistryLock();
        }
        return this;
    }

    /**
     * Initializes element and all child elements that were created by this thread
     * (helper method for init())
     * (may only be called in runtime-registry-synchronized context)
     */
    private void initImpl() {
        //System.out.println("init: " + toString() + " " + parent.toString());
        assert(!isDeleted()) : "Deleted element cannot be reinitialized";

        boolean initThis = !isReady() && isCreator();

        if (initThis) {
            preChildInit();
            RuntimeEnvironment.getInstance().preElementInit(this);
        }

        if (initThis || isReady()) {
            ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                Link child = iterable.get(i);
                if (child != null && child.isPrimaryLink() && (!child.getChild().isDeleted())) {
                    child.getChild().initImpl();
                }
            }
        }

        if (initThis) {
            postChildInit();
            //System.out.println("Setting Ready " + toString() + " Thread: " + ThreadUtil.getCurrentThreadId());
            synchronized (flagMutex) {
                flags |= Flag.READY;
            }

            doStaticParameterEvaluation();

            notifyAnnotationsInitialized();
        }

    }

    /**
     * Initializes element and all child elements that were created by this thread
     * (helper method for init())
     * (may only be called in runtime-registry-synchronized context)
     */
    private void checkPublish() {

        if (isReady()) {

            if (!getFlag(Flag.PUBLISHED) && allParentsReady()) {
                setFlag(Flag.PUBLISHED);
                log(LogLevel.DEBUG_VERBOSE_1, logDomain, "Publishing");
                publishUpdatedInfo(RuntimeListener.ADD);
            }

            // publish any children?
            if (getFlag(Flag.PUBLISHED)) {
                ArrayWrapper<Link> iterable = children.getIterable();
                for (int i = 0, n = iterable.size(); i < n; i++) {
                    Link child = iterable.get(i);
                    if (child != null && (!child.getChild().isDeleted()) /*&& child.isPrimaryLink()*/) {
                        child.getChild().checkPublish();
                    }
                }
            }
        }

    }

    /**
     * @return Have all parents (including link parents) been initialized?
     * (may only be called in runtime-registry-synchronized context)
     */
    private boolean allParentsReady() {
        if (getFlag(Flag.RUNTIME)) {
            return true;
        }
        for (Link l = primary; l != null; l = l.next) {
            if (l.getParent() == null || (!l.getParent().isReady())) {
                return false;
            }
            if (!l.getParent().allParentsReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Initializes this runtime element.
     * The tree structure should be established by now (Uid is valid)
     * so final initialization can be made.
     *
     * Called before children are initialized
     * (called in runtime-registry-synchronized context)
     */
    protected void preChildInit() {}

    /**
     * Initializes this runtime element.
     * The tree structure should be established by now (Uid is valid)
     * so final initialization can be made.
     *
     * Called before children are initialized
     * (called in runtime-registry-synchronized context)
     */
    protected void postChildInit() {}

    /**
     * Deletes element and all child elements
     */
    public void managedDelete() {
        managedDelete(null);
    }

    /**
     * Deletes element and all child elements
     *
     * @param dontDetach Don't detach this link from parent (typically, because parent will clear child list)
     */
    private void managedDelete(Link dontDetach) {

        synchronized (runtimeLockHelper()) {
            synchronized (this) {

                if (isDeleted()) { // can happen if two threads delete concurrently - no problem, since this is - if at all - called when GarbageCollector-safety period has just started
                    return;
                }

                log(LogLevel.DEBUG_VERBOSE_1, logDomain, "FrameworkElement managedDelete");

                // synchronizes on runtime - so no elements will be deleted while runtime is locked
                synchronized (getRegistryLock()) {

                    notifyAnnotationsDelete();

                    log(LogLevel.DEBUG_VERBOSE_1, logDomain, "Deleting");
                    //System.out.println("Deleting " + toString() + " (" + hashCode() + ")");
                    assert !getFlag(Flag.DELETED);
                    assert((primary.getParent() != null) | getFlag(Flag.RUNTIME));

                    synchronized (flagMutex) {
                        flags = (flags | Flag.DELETED) & ~Flag.READY;
                    }

                    if (!getFlag(Flag.RUNTIME)) {
                        RuntimeEnvironment.getInstance().markElementDeleted(this);
                    }
                }

                // perform custom cleanup (stopping/deleting threads can be done here)
                prepareDelete();

                // remove children (thread-safe, because delete flag is set - and addChild etc. checks that)
                deleteChildren();

                // synchronized on runtime for removement from hierarchy
                synchronized (getRegistryLock()) {

                    // remove element itself
                    publishUpdatedInfo(RuntimeListener.REMOVE);

                    // remove from hierarchy
                    for (Link l = primary; l != null;) {
                        if (l != dontDetach && l.parent != null) {
                            l.parent.children.remove(l);
                        }
                        l = l.next;
                    }

                    // TODO
                    //              // JavaOnlyBlock
                    //              if (treeNode != null) {
                    //                  parent.treeNode.remove(treeNode);
                    //              }

                    primary.parent = null;

                }
            }
        }

        // add garbage collector task
        GarbageCollector.deleteDeferred(this);
    }

    /**
     * Deletes all children of this framework element.
     *
     * (may only be called in runtime-registry-synchronized context)
     */
    private void deleteChildren() {

        ArrayWrapper<Link> iterable = children.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            Link child = iterable.get(i);
            if (child != null /*&& child.getChild().isReady()*/) {
                child.getChild().managedDelete(child);
            }
        }

        children.clear();

        // JavaOnlyBlock
        /*if (treeNode != null) {
            treeNode.removeAllChildren();
        }*/
    }

    /**
     * Prepares element for deletion.
     * Port, for instance, are removed from edge lists etc.
     * The final deletion will be done by the GarbageCollector thread after
     * a few seconds (to ensure no other thread is working on this object
     * any more).
     *
     * Is called _BEFORE_ prepareDelete of children
     *
     * (is called with lock on this framework element and possibly all of its parent,
     *  but not runtime-registry. Keep this in mind when cleaning up & joining threads.
     *  This is btw the place to do that.)
     *
     */
    protected void prepareDelete() {}

    /**
     * (for convenience)
     * @return The one and only RuntimeEnvironment
     */
    public RuntimeEnvironment getRuntime() {
        //return getParent(RuntimeEnvironment.class);
        return RuntimeEnvironment.getInstance();
    }

    /**
     * (for convenience)
     * @return Registry of the one and only RuntimeEnvironment - Structure changing operations need to be synchronized on this object!
     * (Only lock runtime for minimal periods of time!)
     */
    public RuntimeEnvironment.Registry getRegistryLock() {
        //return getParent(RuntimeEnvironment.class);
        return RuntimeEnvironment.getInstance().getRegistryHelper();
    }

    /**
     * (for convenience)
     * @return List with all thread local caches - Some cleanup methods require that this is locked
     * (Only lock runtime for minimal periods of time!)
     */
    public SimpleListWithMutex<?> getThreadLocalCacheInfosLock() {
        //return getParent(RuntimeEnvironment.class);
        return RuntimeEnvironment.getInstance().getRegistryHelper().infosLock;
    }

    /**
     * Returns first parent of specified class
     *
     * @param c Class
     * @return Parent or null
     */
    @SuppressWarnings("unchecked")
    public <T extends FrameworkElement> T getParent(Class<T> c) {
        synchronized (getRegistryLock()) { // not really necessary after element has been initialized
            if (isDeleted()) {
                return null;
            }

            FrameworkElement result = primary.parent;
            while (!(c.isAssignableFrom(result.getClass()))) {
                result = result.primary.parent;
                if (result == null || result.isDeleted()) {
                    break;
                }
            }
            return (T)result;
        }
    }

    /**
     * Returns first parent that has the specified flags
     *
     * @param flags Flags to look for
     * @return Parent or null
     */
    public FrameworkElement getParentWithFlags(int flags) {
        if (primary.parent == null) {
            return null;
        }
        synchronized (getRegistryLock()) { // not really necessary after element has been initialized
            if (isDeleted()) {
                return null;
            }

            FrameworkElement result = primary.parent;
            while (!((result.getAllFlags() & flags) == flags)) {
                result = result.primary.parent;
                if (result == null || result.isDeleted()) {
                    break;
                }
            }
            return result;
        }
    }

    /**
     * @return Primary parent framework element
     */
    public FrameworkElement getParent() {
        return primary.parent;
    }

    /**
     * @param linkIndex Link that is referred to (0 = primary link)
     * @return Parent of framework element using specified link
     */
    public FrameworkElement getParent(int linkIndex) {
        synchronized (getRegistryLock()) { // absolutely safe this way
            if (isDeleted()) {
                return null;
            }
            return getLink(linkIndex).parent;
        }
    }

    /**
     * Is Runtime element a child of the specified Runtime element?
     * (also considers links)
     *
     * @param re Possible parent of this Runtime element
     * @return Answer
     */
    public boolean isChildOf(FrameworkElement re) {
        return isChildOf(re, false);
    }

    /**
     * Is Runtime element a child of the specified Runtime element?
     * (also considers links)
     *
     * @param re Possible parent of this Runtime element
     * @param ignoreDeleteFlag Perform check even if delete flag is already set on object (deprecated in C++ - except of directly calling on runtime change)
     * @return Answer
     */
    public boolean isChildOf(FrameworkElement re, boolean ignoreDeleteFlag) {
        synchronized (getRegistryLock()) { // absolutely safe this way
            if ((!ignoreDeleteFlag) && isDeleted()) {
                return false;
            }
            for (Link l = primary; l != null; l = l.next) {
                if (l.parent == re) {
                    return true;
                } else if (l.parent == null) {
                    continue;
                } else {
                    if (l.parent.isChildOf(re)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * @return true before element is officially declared as being initialized
     */
    public boolean isConstructing() {
        return !getFlag(Flag.READY);
    }

    /**
     * @return true after the element has been initialized - equivalent to isReady()
     */
    public boolean isInitialized() {
        return isReady();
    }

    /**
     * @return Number of children (this is only upper bound after deletion of objects - since some entries can be null)
     */
    public int childEntryCount() {
        return children.size();
    }

    /**
     * @return Number of children (includes ones that are not initialized yet)
     * (thread-safe, however, call with runtime registry lock to get exact result when other threads might concurrently add/remove children)
     */
    public int childCount() {
        int count = 0;
        ArrayWrapper<Link> iterable = children.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            if (iterable.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return Element's handle in local runtime environment
     * ("normal" elements have negative handle, while ports have positive ones)
     */
    public int getHandle() {
        return objMutex.getSecondary();
    }

    /**
     * @return Order value in which element needs to be locked (higher means later/after)
     */
    public int getLockOrder() {
        return objMutex.getPrimary();
    }

    /**
     * @param name Name
     * @return Returns first child with specified name - null if none exists
     */
    public FrameworkElement getChild(String name) {
        ArrayWrapper<Link> iterable = children.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            Link child = iterable.get(i);
            if (child != null && child.getChild().isReady()) {
                if (child.getName().equals(name)) {
                    return child.getChild();
                }
            } else if (child != null) {
                synchronized (getRegistryLock()) {
                    if (isDeleted()) {
                        return null;
                    }
                    if (child.getChild().isDeleted()) {
                        continue;
                    }
                    if (child.getName().equals(name)) {
                        return child.getChild();
                    }
                }
            }
        }
        return null;
    }

    /**
     * (Should only be called by ChildIterator and FrameworkElementTreeFilter)
     * @return Array with child elements (careful, do not modify array!)
     */
    ArrayWrapper<Link> getChildren() {
        return children.getIterable();
    }

    /**
     * @return Returns constant and non-constant flags
     */
    public int getAllFlags() {
        return flags;
    }

    /**
     * (Use StringBuilder version if efficiency or real-time is an issue)
     * @return Concatenation of parent names and this element's name
     */
    public String getQualifiedName() {
        StringBuilder sb = new StringBuilder();
        getQualifiedName(sb);
        return sb.toString();
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     */
    public void getQualifiedName(StringBuilder sb) {
        getQualifiedName(sb, primary);
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param linkIndex Index of link to start with
     */
    public void getQualifiedName(StringBuilder sb, int linkIndex) {
        getQualifiedName(sb, getLink(linkIndex));
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param start Link to start with
     */
    public void getQualifiedName(StringBuilder sb, Link start) {
        getQualifiedName(sb, start, true);
    }

    /**
     * (Use StringBuilder version if efficiency or real-time is an issue)
     * @return Qualified link to this element (may be shorter than qualified name, if object has a globally unique link)
     */
    public String getQualifiedLink() {
        StringBuilder sb = new StringBuilder();
        getQualifiedLink(sb);
        return sb.toString();
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @return Is this link globally unique?
     */
    public boolean getQualifiedLink(StringBuilder sb) {
        return getQualifiedLink(sb, primary);
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param linkIndex Index of link to start with
     * @return Is this link globally unique?
     */
    public boolean getQualifiedLink(StringBuilder sb, int linkIndex) {
        return getQualifiedLink(sb, getLink(linkIndex));
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param start Link to start with
     * @return Is this link globally unique?
     */
    public boolean getQualifiedLink(StringBuilder sb, Link start) {
        return getQualifiedName(sb, start, false);
    }


    /**
     * Very efficient implementation of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param start Link to start with
     * @param forceFullLink Return full link from root (even if object has shorter globally unique link?)
     * @return Is this a globally unique link?
     */
    private boolean getQualifiedName(StringBuilder sb, Link start, boolean forceFullLink) {
        if (isReady()) {
            return getQualifiedNameImpl(sb, start, forceFullLink);
        } else {
            synchronized (getRegistryLock()) { // synchronize while element is under construction
                return getQualifiedNameImpl(sb, start, forceFullLink);
            }
        }
    }

    /**
     * Very efficient implementation of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param start Link to start with
     * @param forceFullLink Return full link from root (even if object has shorter globally unique link?)
     * @return Is this a globally unique link?
     */
    private boolean getQualifiedNameImpl(StringBuilder sb, Link start, boolean forceFullLink) {
        int length = 0;
        boolean abortAtLinkRoot = false;
        for (Link l = start; l.parent != null && !(abortAtLinkRoot && l.getChild().getFlag(Flag.ALTERNATIVE_LINK_ROOT)); l = l.parent.primary) {
            abortAtLinkRoot |= (!forceFullLink) && l.getChild().getFlag(Flag.GLOBALLY_UNIQUE_LINK);
            if (abortAtLinkRoot && l.getChild().getFlag(Flag.ALTERNATIVE_LINK_ROOT)) { // if unique_link element is at the same time a link root
                break;
            }
            length += l.name.length() + 1;
        }
        sb.delete(0, sb.length());
        sb.ensureCapacity(length);

        getNameHelper(sb, start, abortAtLinkRoot);
        assert(sb.length() == length);

        // remove any characters if buffer is too long
//      if (len2 < sb.length()) {
//          sb.delete(len2, sb.length());
//      }
        return abortAtLinkRoot;
    }

    /**
     * @param linkIndex Index of link (0 = primary)
     * @return Link with specified index
     * (should be called in synchronized context)
     */
    public Link getLink(int linkIndex) {
        synchronized (getRegistryLock()) { // absolutely safe this way
            if (isDeleted()) {
                return null;
            }
            Link l = primary;
            for (int i = 0; i < linkIndex; i++) {
                l = l.next;
                if (l == null) {
                    return null;
                }
            }
            return l;
        }
    }

    /**
     * same as above, but non-const
     * (should be called in synchronized context)
     */
    private Link getLinkInternal(int linkIndex) {
        Link l = primary;
        for (int i = 0; i < linkIndex; i++) {
            l = l.next;
            if (l == null) {
                return null;
            }
        }
        return l;
    }

    /**
     * @return Number of links to this port
     * (should be called in synchronized context)
     */
    public int getLinkCount() {
        if (isReady()) {
            return getLinkCountHelper();
        } else {
            synchronized (getRegistryLock()) { // absolutely safe this way
                return getLinkCountHelper();
            }
        }
    }

    /**
     * @return Number of links to this port
     * (should be called in synchronized context)
     */
    private int getLinkCountHelper() {
        if (isDeleted()) {
            return 0;
        }
        int i = 0;
        for (Link l = primary; l != null; l = l.next) {
            i++;
        }
        return i;
    }

    /**
     * Recursive Helper function for above
     *
     * @param sb StringBuilder storing result
     * @param l Link to continue with
     * @param abortAtLinkRoot Abort when an alternative link root is reached?
     */
    private static void getNameHelper(StringBuilder sb, Link l, boolean abortAtLinkRoot) {
        if (l.parent == null || (abortAtLinkRoot && l.getChild().getFlag(Flag.ALTERNATIVE_LINK_ROOT))) { // runtime?
            return;
        }
        getNameHelper(sb, l.parent.primary, abortAtLinkRoot);
        sb.append('/');
        sb.append(l.name);
    }

    /**
     * Publish updated edge information
     *
     * @param changeType Type of change (see Constants in RuntimeListener class)
     * @param target Target of edge (this is source)
     *
     * (should only be called by AbstractPort class)
     */
    protected void publishUpdatedEdgeInfo(byte changeType, AbstractPort target) {
        if (getFlag(Flag.PUBLISHED)) {
            RuntimeEnvironment.getInstance().runtimeChange(changeType, this, target);
        }
    }

    /**
     * Publish updated port information
     *
     * @param changeType Type of change (see Constants in RuntimeListener class)
     *
     * (should only be called by FrameworkElement class)
     */
    protected void publishUpdatedInfo(byte changeType) {
        if (changeType == RuntimeListener.ADD || getFlag(Flag.PUBLISHED)) {
            RuntimeEnvironment.getInstance().runtimeChange(changeType, this, null);
        }
    }

    /**
     * Initializes all unitialized framework elements created by this thread
     */
    public static void initAll() {
        RuntimeEnvironment.getInstance().init();
    }

    /**
     * Helper for Debugging: Prints structure below this framework element to console
     */
    public void printStructure() {
        printStructure(LogLevel.USER);
    }

    /**
     * Helper for Debugging: Prints structure below this framework element to log domain
     *
     * @param ll Loglevel with which to print
     */
    public void printStructure(LogLevel ll) {
        LogStream ls = logDomain.getLogStream(ll, getLogDescription());
        ls.appendln("");
        printStructure(0, ls);
        ls.close();
    }

    /**
     * Helper for above
     *
     * @param indent Current indentation level
     * @param output Only used in C++ for streaming
     */
    protected void printStructure(int indent, LogStream output) {

        synchronized (getRegistryLock()) {

            // print element info
            for (int i = 0; i < indent; i++) {
                output.append(" ");
            }

            if (isDeleted()) {
                output.appendln("deleted FrameworkElement");
                return;
            }

            output.append(getName()).append(" (").append((isReady() ? (getFlag(Flag.PUBLISHED) ? "published" : "ready") : isDeleted() ? "deleted" : "constructing")).appendln(")");

            // print child element info
            ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                Link child = iterable.get(i);
                if (child != null) {
                    child.getChild().printStructure(indent + 2, output);
                }
            }
        }
    }

    /**
     * Are name of this element and String 'other' identical?
     * (result is identical to getName().equals(other); but more efficient in C++)
     *
     * @param other Other String
     * @return Result
     */
    public boolean nameEquals(String other) {
        if (isReady()) {
            return primary.name.equals(other);
        } else {
            synchronized (getRegistryLock()) {
                if (isDeleted()) {
                    return false;
                }
                return primary.name.equals(other);
            }
        }
    }

    public String getLogDescription() {
        if (getFlag(Flag.RUNTIME)) {
            return "Runtime";
        } else {
            return getQualifiedName();
        }
    }

    @Override
    public int hashCode() {
        return getHandle();
    }

    /**
     * Trigger evaluation of static parameters in this framework element and all of its children.
     * (This must never be called when thread in surrounding thread container is running.)
     */
    public void doStaticParameterEvaluation() {
        synchronized (getRuntime().getRegistryLock()) {

            // all parameters attached to any of the module's parameters
            SimpleList<StaticParameterBase> attachedParameters = new SimpleList<StaticParameterBase>();
            SimpleList<StaticParameterBase> attachedParametersTmp = new SimpleList<StaticParameterBase>();

            StaticParameterList spl = (StaticParameterList)this.getAnnotation(StaticParameterList.TYPE);
            if (spl != null) {

                // Reevaluate parameters and check whether they have changed
                boolean changed = false;
                for (int i = 0; i < spl.size(); i++) {
                    spl.get(i).loadValue();
                    changed |= spl.get(i).hasChanged();
                    spl.get(i).getAllAttachedParameters(attachedParametersTmp);
                    attachedParameters.addAll(attachedParametersTmp);
                }

                if (changed) {
                    evaluateStaticParameters();

                    // Reset change flags for all parameters
                    for (int i = 0; i < spl.size(); i++) {
                        spl.get(i).resetChanged();
                    }

                    // initialize any new child elements
                    if (isReady()) {
                        init();
                    }
                }
            }

            // evaluate children's static parameters
            ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                Link child = iterable.get(i);
                if (child != null && child.isPrimaryLink() && (!child.getChild().isDeleted())) {
                    child.getChild().doStaticParameterEvaluation();
                }
            }

            // evaluate any attached parameters that have changed, too
            for (int i = 0; i < attachedParameters.size(); i++) {
                if (attachedParameters.get(i).hasChanged()) {
                    attachedParameters.get(i).getParentList().getAnnotated().doStaticParameterEvaluation();
                }
            }
        }
    }

    /**
     * Called whenever static parameters of this framework element need to be (re)evaluated.
     * (can be overridden to handle this event)
     *
     * This typically happens at initialization and when user changes them via finstruct.
     * (This is never called when thread in surrounding thread container is running.)
     * (This must only be called with lock on runtime registry.)
     */
    protected void evaluateStaticParameters() {}

    /**
     * Releases all automatically acquired locks
     */
    public void releaseAutoLocks() {
        ThreadLocalCache.getFast().releaseAllLocks();
    }

    /**
     * Mark element as finstructed
     * (should only be called by AdminServer and CreateModuleActions)
     *
     * @param createAction Action with which framework element was created
     * @param params Parameters that module was created with (may be null)
     */
    public void setFinstructed(CreateFrameworkElementAction createAction, ConstructorParameters params) {
        assert(!getFlag(Flag.FINSTRUCTED));
        StaticParameterList list = StaticParameterList.getOrCreate(this);
        list.setCreateAction(createAction);
        setFlag(Flag.FINSTRUCTED);
        if (params != null) {
            addAnnotation(params);
        }
    }

    /**
     * @param node Common parent config file node for all child parameter config entries (starting with '/' => absolute link - otherwise relative).
     */
    public void setConfigNode(String node) {
        synchronized (getRuntime().getRegistryLock()) {
            ConfigNode cn = (ConfigNode)getAnnotation(ConfigNode.TYPE);
            if (cn != null) {
                if (cn.node.equals(node)) {
                    return;
                }
                cn.node = node;
            } else {
                cn = new ConfigNode(node);
                addAnnotation(cn);
            }

            // reevaluate static parameters
            doStaticParameterEvaluation();

            // reload parameters
            if (isReady()) {
                ConfigFile cf = ConfigFile.find(this);
                if (cf != null) {
                    cf.loadParameterValues(this);
                }
            }
        }
    }

    /**
     * @author Max Reichardt
     *
     * Used to iterate over a framework element's children.
     */
    public static class ChildIterator {

        /** Array with children */
        protected ArrayWrapper<Link> array;

        /** next position in array */
        protected int pos;

        /** FrameworkElement that is currently iterated over */
        protected FrameworkElement curParent;

        /** Relevant flags */
        private int flags;

        /** Expected result when ANDing with flags */
        private int result;

        public ChildIterator(FrameworkElement parent) {
            reset(parent);
        }

        /**
         * @param parent Framework element over whose child to iterate
         * @param onlyReadyElements Include only children that are fully initialized?
         */
        public ChildIterator(FrameworkElement parent, boolean onlyReadyElements) {
            reset(parent, onlyReadyElements);
        }

        /**
         * @param parent Framework element over whose child to iterate
         * @param flags Flags that children must have in order to be considered
         */
        public ChildIterator(FrameworkElement parent, int flags) {
            reset(parent, flags);
        }

        /**
         * @param parent Framework element over whose child to iterate
         * @param flags Relevant flags
         * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
         */
        public ChildIterator(FrameworkElement parent, int flags, int result) {
            reset(parent, flags, result);
        }

        /**
         * @param parent Framework element over whose child to iterate
         * @param flags Relevant flags
         * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
         * @param onlyReadyElements Include only children that are fully initialized?
         */
        public ChildIterator(FrameworkElement parent, int flags, int result, boolean onlyReadyElements) {
            reset(parent, flags, result, onlyReadyElements);
        }

        /**
         * @return Next child - or null if there are no more children left
         */
        public FrameworkElement next() {
            while (pos < array.size()) {
                Link fe = array.get(pos);
                if (fe != null && (fe.getChild().getAllFlags() & flags) == result) {
                    pos++;
                    return fe.getChild();
                }
                pos++;
            }

            return null;
        }

        /**
         * @return Next child that is a port - or null if there are no more children left
         */
        public AbstractPort nextPort() {
            while (true) {
                FrameworkElement result = next();
                if (result == null) {
                    return null;
                }
                if (result.isPort()) {
                    return (AbstractPort)result;
                }
            }
        }

        /**
         * Use iterator again on same framework element
         */
        public void reset() {
            reset(curParent);
        }

        /**
         * Use Iterator for different framework element
         * (or same and reset)
         *
         * @param parent Framework element over whose child to iterate
         */
        public void reset(FrameworkElement parent) {
            reset(parent, true);
        }

        /**
         * Use Iterator for different framework element
         * (or same and reset)
         *
         * @param parent Framework element over whose child to iterate
         */
        public void reset(FrameworkElement parent, boolean onlyReadyElements) {
            reset(parent, 0, 0, onlyReadyElements);
        }

        /**
         * Use Iterator for different framework element
         * (or same and reset)
         *
         * @param parent Framework element over whose child to iterate
         * @param flags Flags that children must have in order to be considered
         */
        public void reset(FrameworkElement parent, int flags) {
            reset(parent, flags, flags);
        }

        /**
         * Use Iterator for different framework element
         * (or same and reset)
         *
         * @param parent Framework element over whose child to iterate
         * @param flags Relevant flags
         * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
         */
        public void reset(FrameworkElement parent, int flags, int result) {
            reset(parent, flags, result, true);
        }

        /**
         * Use Iterator for different framework element
         * (or same and reset)
         *
         * @param parent Framework element over whose child to iterate
         * @param flags Relevant flags
         * @param result Result that ANDing flags with flags must bring (allows specifying that certain flags should not be considered)
         * @param includeNonReady Include children that are not fully initialized yet?
         */
        public void reset(FrameworkElement parent, int flags, int result, boolean onlyReadyElements) {
            assert(parent != null);
            this.flags = flags | Flag.DELETED;
            this.result = result;
            if (onlyReadyElements) {
                this.flags |= Flag.READY;
                this.result |= Flag.READY;
            }
            curParent = parent;

            array = parent.getChildren();
            pos = 0;
        }
    }
}
