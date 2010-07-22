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

import org.finroc.jc.ArrayWrapper;
import org.finroc.jc.GarbageCollector;
import org.finroc.jc.HasDestructor;
import org.finroc.jc.MutexLockOrder;
import org.finroc.jc.annotation.AtFront;
import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.ConstMethod;
import org.finroc.jc.annotation.CppDefault;
import org.finroc.jc.annotation.CppInclude;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.ForwardDecl;
import org.finroc.jc.annotation.Friend;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.InCppFile;
import org.finroc.jc.annotation.Include;
import org.finroc.jc.annotation.Init;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.JavaOnly;
import org.finroc.jc.annotation.Managed;
import org.finroc.jc.annotation.Mutable;
import org.finroc.jc.annotation.NoSuperclass;
import org.finroc.jc.annotation.PassByValue;
import org.finroc.jc.annotation.Protected;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.annotation.Ref;
import org.finroc.jc.annotation.SizeT;
import org.finroc.jc.annotation.SpinLock;
import org.finroc.jc.annotation.Virtual;
import org.finroc.jc.container.SafeConcurrentlyIterableList;
import org.finroc.jc.container.SimpleListWithMutex;
import org.finroc.jc.thread.ThreadUtil;

import org.finroc.core.buffer.CoreOutput;

/**
 * @author max
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
@Ptr
@Friend( {GarbageCollector.class, ChildIterator.class, RuntimeEnvironment.class})
@ForwardDecl( {RuntimeEnvironment.class})
@Include( {"datatype/CoreNumber.h", "rrlib/finroc_core_utils/container/SafeConcurrentlyIterableList.h"})
@CppInclude( {"RuntimeEnvironment.h"})
//@HPrepend({"class NumberPort;", "class RuntimeEnvironment;"})
public class FrameworkElement implements HasDestructor {

    /** Uid of thread that created this framework element */
    @Const protected final long createrThreadUid;

    // Splitting flags up might allow compiler optimizations??

    /** Constant Flags - see CoreFlags */
    @Const protected final int constFlags;

    /** Variable Flags - see CoreFlags */
    protected int flags;

    /**
     * @author max
     *
     * Framework elements are inserted as children of other framework element using this connector class.
     * Ratio: this allows links in the tree
     *
     * Framework elements "own" links - they are deleted with framework element
     */
    @Ptr @NoSuperclass @AtFront @Friend(FrameworkElement.class)
    public class Link implements HasDestructor {

        /** Description of Framework Element - in link context */
        private String description;

        /** Parent - Element in which this link was inserted */
        private @Ptr FrameworkElement parent;

        /** Next link for this framework element (=> singly-linked list) */
        private @Ptr Link next;

        /**
         * @return Element that this link points to
         */
        @ConstMethod public FrameworkElement getChild() {
            return FrameworkElement.this;
        }

        /**
         * @return Description of Framework Element - in link context
         */
        @ConstMethod public String getDescription() {
            return description;
        }

        /**
         * @return Parent - Element in which this link was inserted
         */
        @ConstMethod public @Ptr FrameworkElement getParent() {
            return parent;
        }

        /**
         * @return Is this a primary link?
         */
        @ConstMethod public boolean isPrimaryLink() {
            return this == getChild().primary;
        }

        @Override
        public void delete() {}

        @JavaOnly
        public String toString() {
            return description;
        }
    }

    //Cpp friend class Lock;

    /** Primary link to framework element - the place at which it actually is in FrameworkElement tree - contains description etc. */
    @PassByValue
    private final Link primary = new Link();

    /** children - may contain null entries (for efficient thread-safe unsynchronized iteration) */
    protected final SafeConcurrentlyIterableList<Link> children;

    /** RuntimeElement as TreeNode (only available if set in RuntimeSettings) */
    //@JavaOnly private final DefaultMutableTreeNode treeNode;

    /** State Constants */
    //public enum State { CONSTRUCTING, READY, DELETED };

    /** Element's state */
    //protected volatile State state = State.CONSTRUCTING;

    /** Type Constants */
    //public enum Type { STANDARD_ELEMENT, RUNTIME, PORT }

    /**
     * Element's handle in local runtime environment
     * ("normal" elements have negative handle, while ports have positive ones)
     * Now stored in objMutex
     */
    //@Const protected final int handle;

    /**
     * Defines lock order in which framework elements can be locked.
     * Generally the framework element tree is locked from root to leaves.
     * So children's lock level needs to be larger than their parent's.
     *
     * The secondary component is the element's unique handle in local runtime environment.
     * ("normal" elements have negative handle, while ports have positive ones)
     */
    @Mutable
    public final MutexLockOrder objMutex;

    /**
     * Extra Mutex for changing flags
     */
    @CppType("util::Mutex") @PassByValue @Mutable
    private final Object flagMutex = new Object();

    /** This flag is set to true when the element has been initialized */
    //private boolean initialized;

    /** This flag is set to true when element is deleted */
    //protected volatile boolean deleted;

    /** Is RuntimeElement member of remote runtime; -1 = unknown, 0 = no, 1 = yes */
    //private int remote = -1;

    /**
     * List with owned ports.
     *
     * Methods that want to prevent a current port value (that this RuntimeElement manages)
     * being outdated and reused while they are
     * asynchronously getting it should synchronize with this list.
     * Port methods do this.
     *
     * The owner module will synchronize on this list, every time it updates valuesBeforeAreUnused
     * variable of the ports.
     */
    //protected final SafeArrayList<Port<?>> ownedPorts;

    /** Main Thread. This thread can write to the runtime element's ports without synchronization */
    //protected Thread mainThread;

    @JavaOnly public FrameworkElement(@Const @Ref String description, @Ptr FrameworkElement parent) {
        this(description, parent, CoreFlags.ALLOWS_CHILDREN, -1);
    }

    /**
     * @param description_ Description of framework element (will be shown in browser etc.) - may not be null
     * @param parent_ Parent of framework element (only use non-initialized parent! otherwise null and addChild() later; meant only for convenience)
     * @param flags_ Any special flags for framework element
     * @param lockOrder_ Custom value for lock order (needs to be larger than parent's) - negative indicates unused.
     */
    @SuppressWarnings("unchecked")
    @Init( {/*"description(description_.length() > 0 ? description_ : util::String(\"(anonymous)\"))",*/
        "children(getFlag(CoreFlags::ALLOWS_CHILDREN) ? 4 : 0, getFlag(CoreFlags::ALLOWS_CHILDREN) ? 4 : 0)"
    })
    public FrameworkElement(@Const @Ref @CppDefault("\"\"") String description_, @Ptr @CppDefault("NULL") FrameworkElement parent_,
                            @CppDefault("CoreFlags::ALLOWS_CHILDREN") int flags_, @CppDefault("-1") int lockOrder_) {
        createrThreadUid = ThreadUtil.getCurrentThreadId();
        constFlags = flags_ & CoreFlags.CONSTANT_FLAGS;
        flags = flags_ & CoreFlags.NON_CONSTANT_FLAGS;
        assert((flags_ & CoreFlags.STATUS_FLAGS) == 0);

        // JavaOnlyBlock
        if (description_ == null) {
            description_ = "";
        }

        primary.description = description_;

        objMutex = new MutexLockOrder(getLockOrder(flags_, parent_, lockOrder_), getFlag(CoreFlags.IS_RUNTIME) ? Integer.MIN_VALUE : RuntimeEnvironment.getInstance().registerElement(this));

        // JavaOnlyBlock
        //treeNode = RuntimeSettings.CREATE_TREE_NODES_FOR_RUNTIME_ELEMENTS.get() ? createTreeNode() : null;
        // save memory in case of port
        children = getFlag(CoreFlags.ALLOWS_CHILDREN) ? new SafeConcurrentlyIterableList<Link>(4, 4) :
                   SafeConcurrentlyIterableList.getEmptyInstance();

        if (!getFlag(CoreFlags.IS_RUNTIME)) {
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

        if (RuntimeSettings.DISPLAY_CONSTRUCTION_DESTRUCTION.get()) {
            System.out.println("Constructing FrameworkElement: " + getDescription());
        }
    }

    /**
     * Helper for constructor (required for initializer-list in C++)
     *
     * @return Primary lock order
     */
    private static int getLockOrder(int flags, FrameworkElement parent, int lockOrder) {
        if ((flags & CoreFlags.IS_RUNTIME) == 0) {
            parent = (parent != null) ? parent : RuntimeEnvironment.getInstance().unrelated;
            if (lockOrder < 0) {
                return parent.getLockOrder() + 1;
            }
            return lockOrder;
        } else {
            return lockOrder;
        }
    }

    @JavaOnly public FrameworkElement(@Const @Ref String description) {
        this(description, null);
    }

    @JavaOnly public FrameworkElement() {
        this("", null);
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
    @SpinLock
    protected void setFlag(int flag) {
        synchronized (flagMutex) {
            assert(flag & CoreFlags.CONSTANT_FLAGS) == 0;
            flags |= flag;
        }
    }

    /**
     * (Needs to be synchronized because change operation is not atomic).
     *
     * @param flag Flag to remove
     */
    @SpinLock
    protected void removeFlag(int flag) {
        synchronized (flagMutex) {
            assert(flag & CoreFlags.NON_CONSTANT_FLAGS) != 0;
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
    @Inline @ConstMethod public boolean getFlag(@Const int flag) {
        if (flag <= CoreFlags.CONSTANT_FLAGS) {
            return (constFlags & flag) == flag;
        } else {
            return (flags & flag) == flag;
        }
    }

    /**
     * @return RuntimeElement as TreeNode (only available if set in RuntimeSettings)
     */
    /*@JavaOnly public DefaultMutableTreeNode asTreeNode() {
        return treeNode;
    }*/

    /**
     * @param managesPorts Is RuntimeElement responsible for releasing unused port values?
     */
    //@SuppressWarnings("unchecked")
    //public FrameworkElement(String description, boolean managesPorts, Thread mainThread) {
    //ownedPorts = managesPorts ? new SafeArrayList<Port<?>>() : null;
    //mainThread = mainThread == null ? (managesPorts ? Thread.currentThread() : null) : mainThread;
    //}

//    /**
//     * @return Tree Node representation of this runtime object
//     */
//    @JavaOnly protected DefaultMutableTreeNode createTreeNode() {
//        return new DefaultMutableTreeNode(primary.description);
//    }

    /*Cpp
    //! same as below - except that we return a const char* - this way, no memory needs to be allocated
    const char* getCDescription() {
        return primary.description.length() == 0 ? "(anonymous)" : primary.description.getCString();
    }
     */

    /**
     * @return Name/Description
     */
    @ConstMethod public @Const String getDescription() {
        if (isReady() || getFlag(CoreFlags.IS_RUNTIME)) {
            return primary.description.length() == 0 ? "(anonymous)" : primary.description;
        } else {
            synchronized (getRegistryLock()) { // synchronize, while description can be changed (C++ strings may not be thread safe...)
                if (isDeleted()) {
                    return "(deleted element)";
                }
                return primary.description.length() == 0 ? "(anonymous)" : primary.description;
            }
        }
    }

    /**
     * Write description of link number i to Output stream
     *
     * @param os OutputStream
     * @param i Link Number (0 is primary link/description)
     */
    @ConstMethod public void writeDescription(CoreOutput os, int i) {
        if (isReady()) {
            os.writeString(getLink(i).description);
        } else {
            synchronized (getRegistryLock()) { // synchronize, while description can be changed (C++ strings may not be thread safe...)
                os.writeString(isDeleted() ? "deleted element" : getLink(i).description);
            }
        }
    }

    /**
     * @param description New Port description
     * (only valid/possible before, element is initialized)
     */
    public void setDescription(@Const @Ref String description) {
        assert(!getFlag(CoreFlags.IS_RUNTIME));
        synchronized (getRegistryLock()) { // synchronize, C++ strings may not be thread safe...
            assert(isConstructing());
            assert(isCreator());
            primary.description = description;
        }

        // JavaOnlyBlock
        /*if (treeNode != null) {
            treeNode.setUserObject(description);
        }*/
    }

    /**
     * @return Is current thread the thread that created this object?
     */
    @ConstMethod private boolean isCreator() {
        return ThreadUtil.getCurrentThreadId() == createrThreadUid;
    }

    public String toString() {
        return getDescription();
    }

    /**
     * @return Is element a port?
     */
    @Inline @ConstMethod public boolean isPort() {
        return (constFlags & CoreFlags.IS_PORT) > 0;
    }

    /**
     * @return Is framework element ready/fully initialized and not yet deleted?
     */
    @Inline @ConstMethod public boolean isReady() {
        return (flags & CoreFlags.READY) > 0;
    }

    /**
     * @return Has framework element been deleted? - dangerous if you actually encounter this in C++...
     */
    @Inline @ConstMethod public boolean isDeleted() {
        return (flags & CoreFlags.DELETED) > 0;
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
    private void addChild(@Ptr Link child) {

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
            if (getFlag(CoreFlags.AUTO_RENAME)) {
                String childDesc = child.getDescription();
                int postfixIndex = 1;
                @Ptr ArrayWrapper<Link> ch = children.getIterable();
                for (int i = 0, n = ch.size(); i < n; i++) {
                    @Ptr Link re = ch.get(i);
                    if (re != null && re.getDescription().equals(childDesc)) {
                        // name clash
                        /*if (postfixIndex == 1) {
                            System.out.println("Warning: name conflict in " + getUid() + " - " + child.getDescription());
                        }*/
                        re.getChild().setDescription(childDesc + "[" + postfixIndex + "]");
                        postfixIndex++;
                        continue;
                    }
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
    @ConstMethod public boolean lockAfter(@Const FrameworkElement fe) {
        return objMutex.validAfter(fe.objMutex);
    }

//  /**
//   * Does element with specified qualified name exist?
//   *
//   * @param name (relative) Qualified name
//   * @return Answer
//   */
//  public boolean elementExists(@Const @Ref String name) {
//      return getChildElement(name, false) != null;
//  }

    /**
     * @param name (relative) Qualified name
     * @param onlyGloballyUniqueChildren Only return child with globally unique link?
     * @return Framework element - or null if non-existent
     */
    @InCppFile
    public FrameworkElement getChildElement(@Const @Ref String name, boolean onlyGloballyUniqueChildren) {
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
    protected FrameworkElement getChildElement(@Const @Ref String name, int nameIndex, boolean onlyGloballyUniqueChildren, FrameworkElement root) {

        // lock runtime (might not be absolutely necessary... ensures, however, that result is valid)
        synchronized (getRegistryLock()) {

            if (isDeleted()) {
                return null;
            }

            if (name.charAt(nameIndex) == '/') {
                return root.getChildElement(name, nameIndex + 1, onlyGloballyUniqueChildren, root);
            }

            onlyGloballyUniqueChildren &= (!getFlag(CoreFlags.GLOBALLY_UNIQUE_LINK));
            @Ptr ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                @Ptr Link child = iterable.get(i);
                if (child != null && name.regionMatches(nameIndex, child.description, 0, child.description.length()) && (!child.getChild().isDeleted())) {
                    if (name.length() == nameIndex + child.description.length()) {
                        if (!onlyGloballyUniqueChildren || child.getChild().getFlag(CoreFlags.GLOBALLY_UNIQUE_LINK)) {
                            return child.getChild();
                        }
                    }
                    if (name.charAt(nameIndex + child.description.length()) == '/') {
                        FrameworkElement result = child.getChild().getChildElement(name, nameIndex + child.description.length() + 1, onlyGloballyUniqueChildren, root);
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
    protected void link(FrameworkElement parent, @Const @Ref String linkName) {
        assert(isCreator()) : "may only be called by creator thread";
        assert(lockAfter(parent));

        // lock runtime (required to perform structural changes)
        synchronized (getRegistryLock()) {
            if (isDeleted() || parent.isDeleted()) {
                throw new RuntimeException("Element and/or parent has been deleted. Thread exit is likely the intended behaviour.");
            }

            @Managed Link l = new Link();
            l.description = linkName;
            l.parent = null; // will be set in addChild
            Link lprev = getLinkInternal(getLinkCount() - 1);
            assert lprev.next == null;
            lprev.next = l;
            parent.addChild(l);
            //RuntimeEnvironment.getInstance().link(this, linkName);
        }
    }

    @Protected
    public void delete() {
        assert(getFlag(CoreFlags.DELETED) || getFlag(CoreFlags.IS_RUNTIME)) : "Frameworkelement was not deleted with managedDelete()";
        System.out.println("FrameworkElement destructor: " + getDescription());
        if (!getFlag(CoreFlags.IS_RUNTIME)) {
            // synchronizes on runtime - so no elements will be deleted while runtime is locked
            RuntimeEnvironment.getInstance().unregisterElement(this);
        }

        // delete links
        @Ptr Link l = primary.next;
        while (l != null) {
            @SuppressWarnings("unused")
            @Ptr Link tmp = l;
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
    @CppType("util::MutexLockOrder") @ConstMethod @Ref
    private Object runtimeLockHelper() {

        if (objMutex.validAfter(getRuntime().getRegistryHelper().objMutex)) {
            return getRegistryLock();
        }

        //JavaOnlyBlock
        return this;

        //Cpp return this->objMutex;
    }

//  /**
//   * Initialize this framework element and all framework elements in sub tree that were created by this thread
//   * and weren't initialized already - provided that the parent element already is initialized.
//   * (If parent is not initialized, nothing happens)
//   *
//   * Initializing must be done prior to using framework elements - and in order to them being published.
//   */
//  public void initIfParentIs() {
//      FrameworkElement parent = getParent();
//      synchronized(parent.children) {
//          if (parent.isReady()) {
//              init();
//          }
//      }
//  }

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
            @Ptr ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                @Ptr Link child = iterable.get(i);
                if (child != null && child.isPrimaryLink() && (!child.getChild().isDeleted())) {
                    child.getChild().initImpl();
                }
            }
        }

        if (initThis) {
            postChildInit();
            //System.out.println("Setting Ready " + toString() + " Thread: " + ThreadUtil.getCurrentThreadId());
            synchronized (flagMutex) {
                flags |= CoreFlags.READY;
            }
        }

    }

    /**
     * Initializes element and all child elements that were created by this thread
     * (helper method for init())
     * (may only be called in runtime-registry-synchronized context)
     */
    private void checkPublish() {

        if (isReady()) {

            if (!getFlag(CoreFlags.PUBLISHED) && allParentsReady()) {
                setFlag(CoreFlags.PUBLISHED);
                System.out.println("Publishing " + getQualifiedName());
                publishUpdatedInfo(RuntimeListener.ADD);
            }

            // publish any children?
            if (getFlag(CoreFlags.PUBLISHED)) {
                @Ptr ArrayWrapper<Link> iterable = children.getIterable();
                for (int i = 0, n = iterable.size(); i < n; i++) {
                    @Ptr Link child = iterable.get(i);
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
        if (getFlag(CoreFlags.IS_RUNTIME)) {
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
    @Virtual protected void preChildInit() {}

    /**
     * Initializes this runtime element.
     * The tree structure should be established by now (Uid is valid)
     * so final initialization can be made.
     *
     * Called before children are initialized
     * (called in runtime-registry-synchronized context)
     */
    @Virtual protected void postChildInit() {}

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

                if (RuntimeSettings.DISPLAY_CONSTRUCTION_DESTRUCTION.get()) {
                    System.out.println("FrameworkElement managedDelete: " + getQualifiedName());
                }

                // synchronizes on runtime - so no elements will be deleted while runtime is locked
                synchronized (getRegistryLock()) {

                    System.out.println("Deleting " + toString() + " (" + hashCode() + ")");
                    assert !getFlag(CoreFlags.DELETED);
                    assert((primary.getParent() != null) | getFlag(CoreFlags.IS_RUNTIME));

                    synchronized (flagMutex) {
                        flags = (flags | CoreFlags.DELETED) & ~CoreFlags.READY;
                    }

                    if (!getFlag(CoreFlags.IS_RUNTIME)) {
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

        @Ptr ArrayWrapper<Link> iterable = children.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            @Ptr Link child = iterable.get(i);
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
    @Virtual protected void prepareDelete() {}

    /**
     * (for convenience)
     * @return The one and only RuntimeEnvironment
     */
    @InCppFile
    @ConstMethod public RuntimeEnvironment getRuntime() {
        //return getParent(RuntimeEnvironment.class);
        return RuntimeEnvironment.getInstance();
    }

    /**
     * (for convenience)
     * @return Registry of the one and only RuntimeEnvironment - Structure changing operations need to be synchronized on this object!
     * (Only lock runtime for minimal periods of time!)
     */
    @InCppFile @CppType("util::MutexLockOrder")
    @InCpp("return RuntimeEnvironment::getInstance()->getRegistryHelper()->objMutex;")
    @ConstMethod @Ref public RuntimeEnvironment.Registry getRegistryLock() {
        //return getParent(RuntimeEnvironment.class);
        return RuntimeEnvironment.getInstance().getRegistryHelper();
    }

    /**
     * (for convenience)
     * @return List with all thread local caches - Some cleanup methods require that this is locked
     * (Only lock runtime for minimal periods of time!)
     */
    @InCppFile @CppType("util::MutexLockOrder")
    @InCpp("return RuntimeEnvironment::getInstance()->getRegistryHelper()->infosLock->objMutex;")
    @ConstMethod @Ref public SimpleListWithMutex<?> getThreadLocalCacheInfosLock() {
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
    @JavaOnly public <T extends FrameworkElement> T getParent(Class<T> c) {
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
     * @return Primary parent framework element
     */
    @ConstMethod public FrameworkElement getParent() {
        return primary.parent;
    }

    /**
     * @param linkIndex Link that is referred to (0 = primary link)
     * @return Parent of framework element using specified link
     */
    @ConstMethod public FrameworkElement getParent(int linkIndex) {
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
    @ConstMethod public boolean isChildOf(@Ptr FrameworkElement re) {
        return isChildOf(re, false);
    }

    /**
     * Is Runtime element a child of the specified Runtime element?
     * (also considers links)
     *
     * @param re Possible parent of this Runtime element
     * @param ignoreDeleteFlag Perform check even if delete flag is already set on object (deprecated in C++!)
     * @return Answer
     */
    @Protected
    @ConstMethod public boolean isChildOf(@Ptr FrameworkElement re, boolean ignoreDeleteFlag) {
        synchronized (getRegistryLock()) { // absolutely safe this way
            if ((!ignoreDeleteFlag) && isDeleted()) {
                return false;
            }
            for (@Const Link l = primary; l != null; l = l.next) {
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


//  /**
//   * @return Returns the element's uid, which is the concatenated description of it and all parents.
//   * It is only valid as long as the structure and descriptions stay the same.
//   */
//  public String getUid() {
//      return getUid(false, RuntimeEnvironment.class).toString();
//  }
//
//  /**
//   * @return Returns the element's uid, which is the concatenated description of it and all parents.
//   * In case it is part of a remote runtime environment, the uid in the remote environment is returned.
//   */
//  public String getOriginalUid() {
//      return getUid(false, core.Runtime.class).toString();
//  }
//
//  /**
//   * Optimized helper method for Uid creation
//   *
//   * @param includeSeparator Include separator at end?
//   * @param upToParent Type of parent up to which uid is created
//   * @return Returns the element's uid, which is the concatenated description of it and all parents.
//   */
//  protected StringBuilder getUid(boolean includeSeparator, Class<?> upToParent) {
//      StringBuilder temp = null;
//      if (upToParent.isAssignableFrom(getClass())) {
//          return new StringBuilder();
//      } else if (parent == null || upToParent.isAssignableFrom(parent.getClass())) {
//          temp = new StringBuilder(description);
//      } else {
//          temp = parent.getUid(true, upToParent).append(description);
//      }
//      if (includeSeparator) {
//          temp.append(getUidSeparator());
//      }
//      return temp;
//  }

//  /**
//   * @return Is RuntimeElement member of remote runtime
//   */
//  public boolean isRemote() {
//      if (remote >= 0) {
//          return remote > 0;
//      }
//      if (this instanceof core.Runtime) {
//          remote = (this instanceof RuntimeEnvironment) ? 0 : 1;
//          return remote > 0;
//      }
//      remote = parent.isRemote() ? 1 : 0;
//      return remote > 0;
//  }
//
//  /**
//   * @return Character Sequence that separates the UID after this class;
//   */
//  protected char getUidSeparator() {
//      return '.';
//  }
//
//  /**
//   * @return Returns true when element is deleted
//   */
//  public boolean isDeleted() {
//      return deleted;
//  }
//
//  /**
//   * Get all tasks from children... for thread migration... not a very nice way of doing it... but well
//   */
//  protected void collectTasks(List<Task> t) {
//      List<FrameworkElement> l = getChildren();
//      for (int i = 0; i < l.size(); i++) {
//          FrameworkElement child = l.get(i);
//          if (child != null) {
//              child.collectTasks(t);
//          }
//      }
//  }
//
//  /**
//   * @return Parent
//   */
//  public FrameworkElement getParent() {
//      return parent;
//  }
//
//  /**
//   * @return Umodifiable list of children. Can and should be used for fast
//   * safe unsynchronized iteration. May contain null-entries.
//   */
//  public List<FrameworkElement> getChildren() {
//      return children.getFastUnmodifiable();
//  }
//
//  /**
//   * @param childIndex Index
//   * @return Child at specified index
//   */
//  public FrameworkElement getChildAt(int childIndex) {
//      return children.get(childIndex);
//  }
//
//  /**
//   * Get Child with specified UID
//   * (optimized so that no new Strings need to be created in relative mode)
//   *
//   * @param uid UID
//   * @param b absolute UID? (rather than a relative one)
//   * @return Child (null if does not exists)
//   */
//  public FrameworkElement getChild(String uid, boolean absolute) {
//      if (absolute) {
//          String myUid = getUid(true, RuntimeEnvironment.class).toString();
//          if (!uid.startsWith(myUid)) {
//              return uid.equals(getUid()) ? this : null;
//          } else if (uid.length() == myUid.length()) {
//              return this;
//          }
//          uid = uid.substring(myUid.length());  // cut off separator
//      }
//
//      // uid now relative
//      List<FrameworkElement> l = getChildren();
//      for (int i = 0; i < l.size(); i++) {
//          FrameworkElement child = l.get(i);
//          if (child == null) {
//              continue;
//          }
//          String childDesc = child.getDescription();
//          if (uid.length() < childDesc.length()) {
//              continue;
//          } else if (uid.length() >= childDesc.length()) {
//              if (uid.startsWith(childDesc)) {
//                  if (uid.length() == childDesc.length()) {
//                      return child;
//                  } else if (uid.charAt(childDesc.length()) == child.getUidSeparator()){
//                      return child.getChild(uid.substring(childDesc.length() + 1), false);
//                  }
//              }
//          }
//      }
//      return null;
//  }
//
//  /**
//   * Get All children of the specified class
//   *
//   * @param childClass Class
//   * @return List of children
//   */
//  public <T extends FrameworkElement> List<T> getAllChildren(Class<T> childClass) {
//      List<T> result = new ArrayList<T>();
//      getAllChildrenHelper(result, childClass);
//      return result;
//  }
//
//  /**
//   * Recursive helper function to above function
//   *
//   * @param result List with results (only needs to be allocated once)
//   * @param childClass Class
//   */
//  @SuppressWarnings("unchecked")
//  private <T extends FrameworkElement> void getAllChildrenHelper(List<T> result, Class<T> childClass) {
//      for (int i = 0, n = children.size(); i < n; i++) {
//          FrameworkElement child = children.get(i);
//          if (child == null) {
//              continue;
//          }
//          if (childClass == null || childClass.isAssignableFrom(child.getClass())) {
//              result.add((T)child);
//          }
//          child.getAllChildrenHelper(result, childClass);
//      }
//  }
//
//  /**
//   * Serialize the runtime's uid to the specified output stream.
//   * This is very efficient, since no new objects need to be allocated
//   * to construct the uid
//   *
//   * @param oos Stream to serialize to.
//   */
//  public void serializeUid(CoreOutputStream oos) throws IOException  {
//      serializeUid(oos, true);
//  }
//
//  /**
//   * Helper method for above
//   *
//   * @param oos Stream to serialize to.
//   * @param firstCall Object the method was called on?
//   */
//  protected void serializeUid(CoreOutputStream oos, boolean firstCall) throws IOException {
//      if (parent != null) {
//          parent.serializeUid(oos, false);
//      }
//      oos.write8BitStringPart(description);
//      if (firstCall) {
//          oos.write8BitString(""); // end string
//      } else {
//          oos.writeByte(getUidSeparator());
//      }
//  }
//
//  /**
//   * @return Methods that want to prevent a current port value (that this RuntimeElement manages)
//   * being outdated and reused while they are
//   * asynchronously getting it should synchronize with this object.
//   */
//  public Object getPortManagerSynchInstance() {
//      return ownedPorts;
//  }
//
//  /**
//   * @return Is RuntimeElement responsible for releasing unused port values?
//   */
//  public boolean isPortManager() {
//      return ownedPorts != null;
//  }
//
//  /**
//   * @param p Port that this RuntimeElement owns
//   */
//  protected void addOwnedPort(Port<?> p) {
//      if (isPortManager()) {
//          if (p.isOutputPort()) {
//              p.setMainThread(getMainThread());
//          }
//          ownedPorts.add(p);
//      } else {
//          parent.addOwnedPort(p);
//      }
//  }
//
//  /**
//   * @param p Port that this RuntimeElement does not own any longer
//   */
//  protected void removeOwnedPort(Port<?> p) {
//      if (isPortManager()) {
//          ownedPorts.remove(p);
//      } else {
//          parent.removeOwnedPort(p);
//      }
//  }
//
//  /**
//   * @return Main Thread. This thread can write to the runtime element's ports without synchronization
//   */
//  public Thread getMainThread() {
//      return mainThread;
//  }
//
//  /**
//   * @param Main Thread. This thread can write to the runtime element's ports without synchronization
//   */
//  public void setMainThread(Thread mainThread) {
//      // update main thread of ports
//      this.mainThread = mainThread;
//      if (isPortManager()) {
//          synchronized(ownedPorts) {
//              for (int i = 0; i < ownedPorts.size(); i++) {
//                  PortBase<?> p = ownedPorts.get(i);
//                  if (p != null && p != this) {
//                      ownedPorts.get(i).setMainThread(mainThread);
//                  }
//              }
//          }
//      }
//  }

    /**
     * @return true before element is officially declared as being initialized
     */
    @ConstMethod @Inline public boolean isConstructing() {
        return !getFlag(CoreFlags.READY);
    }

    /**
     * @return true after the element has been initialized - equivalent to isReady()
     */
    @ConstMethod @Inline public boolean isInitialized() {
        return isReady();
    }

    /**
     * @return Number of children (this is only upper bound after deletion of objects - since some entries can be null)
     */
    @ConstMethod public @SizeT int childEntryCount() {
        return children.size();
    }

    /**
     * @return Number of children (includes ones that are not initialized yet)
     * (thread-safe, however, call with runtime registry lock to get exact result when other threads might concurrently add/remove children)
     */
    @ConstMethod public @SizeT int childCount() {
        int count = 0;
        @Ptr ArrayWrapper<Link> iterable = children.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            if (iterable.get(i) != null) {
                count++;
            }
        }
        return count;
    }

//  ///////////////////// real factory methods /////////////////////////
//  //public Port<?>
//
//  protected NumberPort addNumberPort(boolean output, @Const @Ref String description, @CppDefault("CoreNumber::ZERO") @Const @Ref Number defaultValue, @CppDefault("NULL") Unit unit) {
//      return addNumberPort(output ? PortFlags.OUTPUT_PORT : PortFlags.INPUT_PORT, description, defaultValue, unit);
//  }
//
//  protected NumberPort addNumberPort(int flags, @Const @Ref String description, @CppDefault("CoreNumber::ZERO") @Const @Ref Number defaultValue, @CppDefault("NULL") Unit unit) {
//      PortCreationInfo pci = new PortCreationInfo(description, this, flags);
//      //PortDataCreationInfo.get().set(DataTypeRegister2.getDataTypeEntry(CoreNumberContainer.class), owner, prototype);
//      //pci.defaultValue = new CoreNumberContainer(new CoreNumber2(defaultValue, unit));
//      pci.unit = unit;
//      //pci.parent = this;
//      NumberPort np = new NumberPort(pci);
//      np.getDefaultBuffer().setValue(defaultValue, unit != null ? unit : Unit.NO_UNIT);
//      //addChild(np);
//      return np;
//  }
//
//  ///////////////////// convenience factory methods /////////////////////////
//  @JavaOnly public NumberPort addNumberInputPort(@Const @Ref String description) {
//      return addNumberInputPort(description, CoreNumber.ZERO, Unit.NO_UNIT);
//  }
//  @JavaOnly public NumberPort addNumberInputPort(@Const @Ref String description, @Const @Ref Number defaultValue) {
//      return addNumberInputPort(description, defaultValue, Unit.NO_UNIT);
//  }
//  @JavaOnly public NumberPort addNumberInputPort(@Const @Ref String description, @Ptr Unit unit) {
//      return addNumberInputPort(description, CoreNumber.ZERO, unit);
//  }
//  public NumberPort addNumberInputPort(@Const @Ref String description, @CppDefault("CoreNumber::ZERO") @Const @Ref Number defaultValue, @CppDefault("NULL") @Ptr Unit unit) {
//      return addNumberPort(false, description, defaultValue, unit);
//  }
//  /*public NumberPort addNumberInputPort(String description, Number defaultValue, Unit unit, double min, double max) {
//
//  }*/
//
//  @JavaOnly public NumberPort addNumberOutputPort(@Const @Ref String description) {
//      return addNumberOutputPort(description, CoreNumber.ZERO, Unit.NO_UNIT);
//  }
//  @JavaOnly public NumberPort addNumberOutputPort(@Const @Ref String description, @Const @Ref Number defaultValue) {
//      return addNumberOutputPort(description, defaultValue, Unit.NO_UNIT);
//  }
//  @JavaOnly public NumberPort addNumberOutputPort(@Const @Ref String description, @Ptr Unit unit) {
//      return addNumberOutputPort(description, CoreNumber.ZERO, unit);
//  }
//  public NumberPort addNumberOutputPort(@Const @Ref String description, @CppDefault("CoreNumber::ZERO") @Const @Ref Number defaultValue, @CppDefault("NULL") @Ptr Unit unit) {
//      return addNumberPort(true, description, defaultValue, unit);
//  }
//
//  public NumberPort addNumberProxyPort(boolean output, @Const @Ref String description, @CppDefault("CoreNumber::ZERO") @Const @Ref Number defaultValue, @CppDefault("NULL") @Ptr Unit unit) {
//      return addNumberPort(output ? PortFlags.OUTPUT_PROXY : PortFlags.INPUT_PROXY, description, defaultValue, unit);
//  }

    /**
     * @return Element's handle in local runtime environment
     * ("normal" elements have negative handle, while ports have positive ones)
     */
    @ConstMethod @Inline public int getHandle() {
        return objMutex.getSecondary();
    }

    /**
     * @return Order value in which element needs to be locked (higher means later/after)
     */
    @ConstMethod @Inline public int getLockOrder() {
        return objMutex.getPrimary();
    }

    /**
     * @param name Description
     * @return Returns first child with specified description - null if none exists
     */
    @ConstMethod public FrameworkElement getChild(String name) {
        @Ptr ArrayWrapper<Link> iterable = children.getIterable();
        for (int i = 0, n = iterable.size(); i < n; i++) {
            Link child = iterable.get(i);
            if (child.getChild().isReady()) {
                if (child.getDescription().equals(name)) {
                    return child.getChild();
                }
            } else {
                synchronized (getRegistryLock()) {
                    if (isDeleted()) {
                        return null;
                    }
                    if (child.getChild().isDeleted()) {
                        continue;
                    }
                    if (child.getDescription().equals(name)) {
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
    @Const @ConstMethod @Ptr ArrayWrapper<Link> getChildren() {
        return children.getIterable();
    }

    /**
     * @return Returns constant and non-constant flags
     */
    @ConstMethod public int getAllFlags() {
        return flags | constFlags;
    }

//  /**
//   * Get all children with specified flags set/unset
//   * (Don't store this array for longer than possible
//   *
//   * @param result Children will be copied to this array (if there are more, array will be filled completely)
//   * @param checkFlags Flags to check
//   * @param checkResult The result the check has to have in order to add child to result
//   * @return Number of elements in result.
//   */
//  public @SizeT int getChildrenFlagged(@Ref FrameworkElement[] result, int checkFlags, int checkResult) {
//
//  }

    /**
     * (Use StringBuilder version if efficiency or real-time is an issue)
     * @return Concatenation of parent descriptions and this element's description
     */
    @Inline @ConstMethod public String getQualifiedName() {
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
    @Inline @ConstMethod public void getQualifiedName(@Ref StringBuilder sb) {
        getQualifiedName(sb, primary);
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param linkIndex Index of link to start with
     */
    @ConstMethod public void getQualifiedName(@Ref StringBuilder sb, @SizeT int linkIndex) {
        getQualifiedName(sb, getLink(linkIndex));
    }

    /**
     * Efficient variant of above.
     * (StringBuilder may be reused)
     *
     * @param sb StringBuilder that will store result
     * @param start Link to start with
     */
    @ConstMethod public void getQualifiedName(@Ref StringBuilder sb, @Const Link start) {
        getQualifiedName(sb, start, true);
    }

    /**
     * (Use StringBuilder version if efficiency or real-time is an issue)
     * @return Qualified link to this element (may be shorter than qualified name, if object has a globally unique link)
     */
    @Inline @ConstMethod public String getQualifiedLink() {
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
    @Inline @ConstMethod public boolean getQualifiedLink(@Ref StringBuilder sb) {
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
    @ConstMethod public boolean getQualifiedLink(@Ref StringBuilder sb, @SizeT int linkIndex) {
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
    @ConstMethod public boolean getQualifiedLink(@Ref StringBuilder sb, @Const Link start) {
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
    @ConstMethod private boolean getQualifiedName(@Ref StringBuilder sb, @Const Link start, boolean forceFullLink) {
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
    @ConstMethod private boolean getQualifiedNameImpl(@Ref StringBuilder sb, @Const Link start, boolean forceFullLink) {
        @SizeT int length = 0;
        boolean abortAtLinkRoot = false;
        for (@Const Link l = start; l.parent != null && !(abortAtLinkRoot && l.getChild().getFlag(CoreFlags.ALTERNATE_LINK_ROOT)); l = l.parent.primary) {
            abortAtLinkRoot |= (!forceFullLink) && l.getChild().getFlag(CoreFlags.GLOBALLY_UNIQUE_LINK);
            if (abortAtLinkRoot && l.getChild().getFlag(CoreFlags.ALTERNATE_LINK_ROOT)) { // if unique_link element is at the same time a link root
                break;
            }
            length += l.description.length() + 1;
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
    @ConstMethod public @Ptr @Const Link getLink(@SizeT int linkIndex) {
        synchronized (getRegistryLock()) { // absolutely safe this way
            if (isDeleted()) {
                return null;
            }
            @Const Link l = primary;
            for (@SizeT int i = 0; i < linkIndex; i++) {
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
    private @Ptr Link getLinkInternal(@SizeT int linkIndex) {
        Link l = primary;
        for (@SizeT int i = 0; i < linkIndex; i++) {
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
    @ConstMethod public @SizeT int getLinkCount() {
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
    @ConstMethod private @SizeT int getLinkCountHelper() {
        if (isDeleted()) {
            return 0;
        }
        @SizeT int i = 0;
        for (@Const Link l = primary; l != null; l = l.next) {
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
    @ConstMethod private static void getNameHelper(@Ref StringBuilder sb, @Ptr @Const Link l, boolean abortAtLinkRoot) {
        if (l.parent == null || (abortAtLinkRoot && l.getChild().getFlag(CoreFlags.ALTERNATE_LINK_ROOT))) { // runtime?
            return;
        }
        getNameHelper(sb, l.parent.primary, abortAtLinkRoot);
        sb.append('/');
        sb.append(l.description);
    }

//  /**
//   * Called whenever an asynchronous call returns.
//   * May be overriden/implemented by subclasses.
//   * Default behaviour is throwing an Exception.
//   * (Should only be called by framework-internal classes)
//   *
//   * @param pc Call Object containing various parameters
//   */
//  @InCppFile
//  @Virtual public void handleCallReturn(AbstractCall pc) {
//      throw new RuntimeException("This FrameworkElement cannot handle call returns");
//  }

    /**
     * Publish updated port information
     *
     * @param changeType Type of change (see Constants in RuntimeListener class)
     *
     * (should only be called by FrameworkElement class)
     */
    @InCppFile
    protected void publishUpdatedInfo(byte changeType) {
        if (changeType == RuntimeListener.ADD || getFlag(CoreFlags.PUBLISHED)) {
            RuntimeEnvironment.getInstance().runtimeChange(changeType, this);
        }
    }

    /**
     * Initializes all unitialized framework elements created by this thread
     */
    @InCppFile
    public static void initAll() {
        RuntimeEnvironment.getInstance().init();
    }

    /**
     * Helper for Debugging: Prints structure below this framework element to console
     */
    public void printStructure() {
        printStructure(0);
    }

    /**
     * Helper for above
     *
     * @param indent Current indentation level
     */
    @Virtual
    protected void printStructure(int indent) {

        synchronized (getRegistryLock()) {

            // print element info
            for (int i = 0; i < indent; i++) {
                System.out.print(" ");
            }

            if (isDeleted()) {
                System.out.println("deleted FrameworkElement");
                return;
            }

            System.out.print(getDescription());
            System.out.print(" (");
            System.out.print(isReady() ? (getFlag(CoreFlags.PUBLISHED) ? "published" : "ready") : isDeleted() ? "deleted" : "constructing");
            System.out.println(")");

            // print child element info
            @Ptr ArrayWrapper<Link> iterable = children.getIterable();
            for (int i = 0, n = iterable.size(); i < n; i++) {
                Link child = iterable.get(i);
                if (child != null) {
                    child.getChild().printStructure(indent + 2);
                }
            }
        }
    }

    /**
     * Are description of this element and String 'other' identical?
     * (result is identical to getDescription().equals(other); but more efficient in C++)
     *
     * @param other Other String
     * @return Result
     */
    public boolean descriptionEquals(String other) {
        if (isReady()) {
            return primary.description.equals(other);
        } else {
            synchronized (getRegistryLock()) {
                if (isDeleted()) {
                    return false;
                }
                return primary.description.equals(other);
            }
        }
    }
}
