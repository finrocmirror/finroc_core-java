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
package org.finroc.core.port.rpc;

import org.finroc.jc.annotation.Const;
import org.finroc.jc.annotation.CppType;
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.annotation.Inline;
import org.finroc.jc.annotation.Ptr;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.core.port.ThreadLocalCache;

/**
 * @author max
 *
 * This class contains the logic for triggering synchronous (method) calls
 * (possibly over the net & without blocking further threads etc.)
 */
public class SynchMethodCallLogic {

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"rpc\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    /**
     * Perform synchronous call. Thread will wait for return value (until timeout has passed).
     *
     * @param <T> Call type
     * @param call Actual Call object
     * @param callMe "Thing" that will be invoked/called with Call object
     * @param timeout Timeout for call
     * @return Returns call object - might be the same as in call parameter (likely - if call wasn't transferred via network)
     */
    @SuppressWarnings("unchecked") @Inline
    public static <T extends AbstractCall> T performSynchCall(T call, Callable<T> callMe, long timeout) throws MethodCallException {
        //Cpp assert(((void*)(static_cast<AbstractCall*>(call))) == ((void*)call)); // ensure safety for Callable cast
        @InCpp("Callable<AbstractCall>* tmp = reinterpret_cast<Callable<AbstractCall>*>(callMe);")
        Callable<AbstractCall> tmp = (Callable<AbstractCall>)callMe;
        return (T)performSynchCallImpl(call, tmp, timeout);
    }

    /**
     * (Private Helper method - because we'd need a .hpp otherwise)
     * Perform synchronous call. Thread will wait for return value (until timeout has passed).
     *
     * @param <T> Call type
     * @param call Actual Call object
     * @param callMe "Thing" that will be invoked/called with Call object
     * @param timeout Timeout for call
     * @return Returns call object - might be the same as in call parameter (likely - if call wasn't transferred via network)
     */
    private static AbstractCall performSynchCallImpl(AbstractCall call, Callable<AbstractCall> callMe, long timeout) throws MethodCallException {
        @Ptr MethodCallSyncher mcs = ThreadLocalCache.get().getMethodSyncher();
        @Ptr AbstractCall ret = null;
        synchronized (mcs) {
            call.setupSynchCall(mcs);
            mcs.currentMethodCallIndex = call.getMethodCallIndex();
            assert(mcs.methodReturn == null);
            callMe.invokeCall(call);
            try {
                mcs.wait(timeout);
            } catch (InterruptedException e) {
                logDomain.log(LogLevel.LL_ERROR, getLogDescription(), "Synch method call interrupted... this shouldn't happen... usually");
            }

            // reset stuff for next call
            mcs.getAndUseNextCallIndex(); // Invalidate results of any incoming outdated returns
            ret = mcs.methodReturn;
            mcs.methodReturn = null;

            if (ret == null) {

                // JavaOnlyBlock
                logDomain.log(LogLevel.LL_DEBUG, getLogDescription(), "Thread " + Thread.currentThread().toString() + ": Call timed out");

                // (recycling is job of receiver)
                throw new MethodCallException(MethodCallException.Type.TIMEOUT);
            }
        }
        return ret;
    }

    /**
     * @return Description for logging
     */
    @CppType("char*") @Const
    private static String getLogDescription() {
        return "SynchMethodCallLogic";
    }

    /**
     * Deliver/pass return value to calling/waiting thread.
     *
     * @param call Call object that (possibly) contains some return value
     */
    public static void handleMethodReturn(AbstractCall call) {
        // return value
        @Ptr MethodCallSyncher mcs = MethodCallSyncher.get(call.getSyncherID());
        mcs.returnValue(call);
    }
}
