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
package org.finroc.core.port;

import org.finroc.core.FinrocAnnotation;
import org.finroc.core.FrameworkElement;
import org.finroc.core.port.AbstractPort.ConnectDirection;
import org.finroc.core.portdatabase.FinrocTypeInfo;
import org.rrlib.finroc_core_utils.jc.HasDestructor;
import org.rrlib.finroc_core_utils.jc.log.LogDefinitions;
import org.rrlib.finroc_core_utils.log.LogDomain;
import org.rrlib.finroc_core_utils.rtti.DataTypeBase;

/**
 * @author Max Reichardt
 *
 * Port classes are not directly used by an application developer.
 * Rather a wrapped class based on this class is used.
 * This has the following advantages:
 * - Wrapped wraps and manages pointer to actual port. No pointers are needed to work with these classes
 * - Only parts of the API meant to be used by the application developer are exposed.
 * - Connect() methods can be hidden/reimplemented (via name hiding). This can be used to enforce that only certain connections can be created at compile time.
 */
public class PortWrapperBase implements HasDestructor {

    /** Wrapped port */
    protected AbstractPort wrapped;

    /** Log domain for this class */
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("ports");

    @Override
    public void delete() {
        //wrapped.managedDelete(); - this is unsuitable for pass by value; it should work without anyway
    }

    /**
     * @return Wrapped port. For rare case that someone really needs to access ports.
     */
    public AbstractPort getWrapped() {
        return wrapped;
    }

    /**
     * Same as getName()
     * (except that we return a const char* in C++)
     */
    public String getCName() {
        return wrapped.getName();
    }

    /**
     * @return Name of this framework element
     */
    public String getName() {
        return wrapped.getName();
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
    public boolean isReady() {
        return wrapped.isReady();
    }

    /**
     * @return Has port been deleted? (you should not encounter this)
     */
    public boolean isDeleted() {
        return wrapped.isDeleted();
    }

    /**
     * (relevant for input ports only)
     *
     * @return Has port changed since last changed-flag-reset?
     */
    public boolean hasChanged() {
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
    public DataTypeBase getDataType() {
        return wrapped.getDataType();
    }

    /**
     * @return Additional type info for port data
     */
    public FinrocTypeInfo getDataTypeInfo() {
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
    public boolean pushStrategy() {
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
    public boolean reversePushStrategy() {
        return wrapped.reversePushStrategy();
    }

    /**
     * (slightly expensive)
     * @return Is port currently connected?
     */
    public boolean isConnected() {
        return wrapped.isConnected();
    }

    /**
     * Are name of this element and String 'other' identical?
     * (result is identical to getName().equals(other); but more efficient in C++)
     *
     * @param other Other String
     * @return Result
     */
    public boolean nameEquals(String other) {
        return wrapped.nameEquals(other);
    }

    /**
     * @return
     * @see org.finroc.core.FrameworkElement#getLogDescription()
     */
    public String getLogDescription() {
        return wrapped.getLogDescription();
    }

    /**
     * Connect port to specified partner port
     *
     * @param to Port to connect this port to
     */
    public void connectTo(AbstractPort to) {
        wrapped.connectTo(to);
    }

    /**
     * Connect port to specified partner port
     *
     * @param partnerPortParent Parent of port to connect to
     * @param partnerPortName Name of port to connect to
     * @param warnIfNotAvailable Print warning message if connection cannot be established
     */
    public void connectToTarget(FrameworkElement partnerPortParent, String partnerPortName, boolean warnIfNotAvailable) {
        wrapped.connectTo(partnerPortParent, partnerPortName, warnIfNotAvailable, ConnectDirection.AUTO);
    }

    /**
     * Connect port to specified partner port
     *
     * @param to Port to connect this port to
     */
    public void connectTo(PortWrapperBase to) {
        wrapped.connectTo(to.wrapped);
    }

    /**
     * Connect port to specified partner port
     * (connection is (re)established when link is available)
     *
     * @param linkName Link name of target port (relative to parent framework element)
     */
    public void connectTo(String linkName) {
        wrapped.connectTo(linkName);
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
    public <A extends FinrocAnnotation> A getAnnotation(DataTypeBase dt) {
        return (A)wrapped.getAnnotation(dt);
    }

    /**
     * @return Primary parent framework element
     */
    public FrameworkElement getParent() {
        return wrapped.getParent();
    }
}
