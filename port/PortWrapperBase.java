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
package org.finroc.core.port;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.annotation.Const;
import org.rrlib.finroc_core_utils.jc.annotation.ConstMethod;
import org.rrlib.finroc_core_utils.jc.annotation.CppDefault;
import org.rrlib.finroc_core_utils.jc.annotation.CppType;
import org.rrlib.finroc_core_utils.jc.annotation.HAppend;
import org.rrlib.finroc_core_utils.jc.annotation.InCpp;
import org.rrlib.finroc_core_utils.jc.annotation.Inline;
import org.rrlib.finroc_core_utils.jc.annotation.NoCpp;
import org.rrlib.finroc_core_utils.jc.annotation.PassByValue;
import org.rrlib.finroc_core_utils.jc.annotation.Ptr;
import org.rrlib.finroc_core_utils.jc.annotation.RawTypeArgs;
import org.rrlib.finroc_core_utils.jc.annotation.Ref;
import org.rrlib.finroc_core_utils.jc.annotation.SkipArgs;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.serialization.DataTypeBase;

/**
 * @author max
 *
 * Port classes are not directly used by an application developer.
 * Rather a wrapped class based on this class is used.
 * This has the following advantages:
 * - Wrapped wraps and manages pointer to actual port. No pointers are needed to work with these classes
 * - Only parts of the API meant to be used by the application developer are exposed.
 * - Connect() methods can be hidden/reimplemented (via name hiding). This can be used to enforce that only certain connections can be created at compile time.
 */
@Inline @NoCpp @PassByValue @RawTypeArgs
@HAppend("template <typename T> inline bool operator==(const AbstractPort* p, PortWrapperBase<T> pw) { return pw == p; }")
public class PortWrapperBase<T extends AbstractPort> implements HasDestructor {

    /** Wrapped port */
    @Ptr protected T wrapped;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"ports\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("ports");

    @Override
    public void delete() {
        //wrapped.managedDelete(); - this is unsuitable for pass by value; it should work without anyway
    }

    /**
     * @return Wrapped port. For rare case that someone really needs to access ports.
     */
    public @Ptr T getWrapped() {
        return wrapped;
    }

    /**
     * same as getDescription()
     * except that we return a const char* in C++ - this way, no memory needs to be allocated
     */
    @Const @CppType("char*") @ConstMethod
    public String getCDescription() {
        return wrapped.getCDescription();
    }

    /**
     * @return Name/Description
     */
    @ConstMethod public @Const String getDescription() {
        return wrapped.getDescription();
    }

    /**
     * (relevant for input ports only)
     *
     * Sets changed flag
     */
    public void setChanged() {
        wrapped.setChanged();
    }

    /**
     * @return Is framework element ready/fully initialized and not yet deleted?
     */
    @ConstMethod public boolean isReady() {
        return wrapped.isReady();
    }

    /**
     * @return Has port been deleted? (you should not encounter this)
     */
    @ConstMethod public boolean isDeleted() {
        return wrapped.isDeleted();
    }

    /**
     * (relevant for input ports only)
     *
     * @return Has port changed since last changed-flag-reset?
     */
    @ConstMethod public boolean hasChanged() {
        return wrapped.hasChanged();
    }

    /**
     * (relevant for input ports only)
     *
     * Reset changed flag.
     */
    public void resetChanged() {
        wrapped.resetChanged();
    }

    /**
     * @return Type of port data
     */
    @ConstMethod public @Const DataTypeBase getDataType() {
        return wrapped.getDataType();
    }

    /**
     * @return Additional type info for port data
     */
    @ConstMethod public FinrocTypeInfo getDataTypeInfo() {
        return FinrocTypeInfo.get(wrapped.getDataType());
    }

    /**
     * Set whether data should be pushed or pulled
     *
     * @param push Push data?
     */
    public void setPushStrategy(boolean push) {
        wrapped.setPushStrategy(push);
    }

    /**
     * Is data to this port pushed or pulled?
     *
     * @return Answer
     */
    @ConstMethod public boolean pushStrategy() {
        return wrapped.pushStrategy();
    }

    /**
     * Set whether data should be pushed or pulled in reverse direction
     *
     * @param push Push data?
     */
    public void setReversePushStrategy(boolean push) {
        wrapped.setReversePushStrategy(push);
    }

    /**
     * Is data to this port pushed or pulled (in reverse direction)?
     *
     * @return Answer
     */
    @ConstMethod public boolean reversePushStrategy() {
        return wrapped.reversePushStrategy();
    }

    /**
     * (slightly expensive)
     * @return Is port currently connected?
     */
    @ConstMethod public boolean isConnected() {
        return wrapped.isConnected();
    }

    /**
     * Are description of this element and String 'other' identical?
     * (result is identical to getDescription().equals(other); but more efficient in C++)
     *
     * @param other Other String
     * @return Result
     */
    @ConstMethod public boolean descriptionEquals(@Const @Ref String other) {
        return wrapped.descriptionEquals(other);
    }

    /**
     * @return
     * @see org.finroc.core.FrameworkElement#getLogDescription()
     */
    @ConstMethod public String getLogDescription() {
        return wrapped.getLogDescription();
    }

    /**
     * Connect port to specified target port
     *
     * @param target Target port
     */
    public void connectToTarget(AbstractPort target) {
        wrapped.connectToTarget(target);
    }

    /**
     * Connect port to specified source port
     *
     * @param srcPortParent Parent of source port
     * @param srcPortName Name of source port
     * @param warnIfNotAvailable Print warning message if connection cannot be established
     */
    public void connectToSource(FrameworkElement srcPortParent, String srcPortName, boolean warnIfNotAvailable) {
        wrapped.connectToSource(srcPortParent, srcPortName, warnIfNotAvailable);
    }

    /**
     * Connect port to specified destination port
     *
     * @param destPortParent Parent of destination port
     * @param destPortName Name of destination port
     * @param warnIfNotAvailable Print warning message if connection cannot be established
     */
    public void connectToTarget(FrameworkElement destPortParent, String destPortName, boolean warnIfNotAvailable) {
        wrapped.connectToTarget(destPortParent, destPortName, warnIfNotAvailable);
    }

    /**
     * Connect port to specified target port
     *
     * @param target Target port
     */
    public void connectToTarget(@Const @Ref PortWrapperBase<T> target) {
        wrapped.connectToTarget(target.wrapped);
    }

    /**
     * Connect port to specified source port
     *
     * @param source Source port
     */
    public void connectToSource(AbstractPort source) {
        wrapped.connectToSource(source);
    }

    /**
     * Connect port to specified source port
     *
     * @param source Source port
     */
    public void connectToSource(@Const @Ref PortWrapperBase<T> source) {
        wrapped.connectToSource(source.wrapped);
    }

    /**
     * Connect port to specified source port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of source port (relative to parent framework element)
     */
    public void connectToSource(String srcLink) {
        wrapped.connectToSource(srcLink);
    }

    /**
     * Connect port to specified target port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of target port (relative to parent framework element)
     */
    public void connectToTarget(String destLink) {
        wrapped.connectToTarget(destLink);
    }

    /**
     * @param interval2 Minimum Network Update Interval
     */
    public void setMinNetUpdateInterval(int interval2) {
        wrapped.setMinNetUpdateInterval(interval2);
    }

    /**
     * @return Minimum Network Update Interval (only-port specific one; -1 if there's no specific setting for port)
     */
    public short getMinNetUpdateInterval() {
        return wrapped.getMinNetUpdateInterval();
    }

    /**
     * Initialize this port.
     * This must be called prior to using port
     * (usually done by initializing parent)
     * - and in order port being published.
     */
    public void init() {
        wrapped.init();
    }

    /*Cpp
    // using this operator, it can be checked conveniently in PortListener's portChanged()
    // whether origin port is the same port as this object wraps
    bool operator ==(const AbstractPort* p) const {
        return wrapped == p;
    }
     */

    /**
     * @return Number of connections to this port (incoming and outgoing)
     */
    public int getConnectionCount() {
        return wrapped.getConnectionCount();
    }

    /**
     * Releases all automatically acquired locks
     */
    public void releaseAutoLocks() {
        ThreadLocalCache.getFast().releaseAllLocks();
    }

    /**
     * Add annotation to this port
     *
     * @param ann Annotation
     */
    public void addAnnotation(FinrocAnnotation ann) {
        wrapped.addAnnotation(ann);
    }

    /**
     * Get annotation of specified type
     *
     * @param type Data type of annotation we're looking for
     * @return Annotation. Null if port has no annotation of this type.
     */
    @SuppressWarnings("unchecked")
    @SkipArgs("1")
    public <A extends FinrocAnnotation> A getAnnotation(@CppDefault("rrlib::serialization::DataType<A>()") DataTypeBase dt) {
        return (A)wrapped.getAnnotation(dt);
    }
}
