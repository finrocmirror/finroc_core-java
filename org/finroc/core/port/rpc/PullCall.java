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
import org.finroc.jc.annotation.InCpp;
import org.finroc.jc.log.LogDefinitions;
import org.finroc.jc.thread.Task;
import org.finroc.log.LogDomain;
import org.finroc.log.LogLevel;
import org.finroc.core.buffer.CoreOutput;
import org.finroc.core.buffer.CoreInput;
import org.finroc.core.port.cc.CCInterThreadContainer;
import org.finroc.core.port.cc.CCPortBase;
import org.finroc.core.port.net.NetPort;
import org.finroc.core.port.std.PortBase;
import org.finroc.core.port.std.PortData;

/**
 * @author max
 *
 * This class is used for port-pull-requests/calls - locally and over the net.
 *
 * (Caller stack will contain every port in chain - pulled value will be assigned to each of them)
 */
public class PullCall extends AbstractCall implements Task {

//  /** Maximum size of caller stack */
//  private final static int MAX_CALL_DEPTH = 16;

//  /** Data type of method */
//  public static DataType METHOD_TYPE;

//  /** Stores information about pulled port data */
//  public final @PassByValue PublishCache info = new PublishCache();
//
//  /** Reference to pulled "cheap copy" port data */
//  public CCInterThreadContainer<?> ccData;
//
//  /** Reference to pulled port data */
//  public PortData data;
//
//  /** ThreadLocalCache - is != null - if it has been set up to perform assignments with current cc data */
//  public ThreadLocalCache tc;

    /** Assign pulled value to ports in between? */
    public boolean intermediateAssign;

    /** Is this a pull call for a cc port? */
    public boolean ccPull;

    /** when received through network and executed in separate thread: Port to call pull on and port to send result back over */
    public NetPort port;

    /** Log domain for this class */
    @InCpp("_RRLIB_LOG_CREATE_NAMED_DOMAIN(logDomain, \"rpc\");")
    public static final LogDomain logDomain = LogDefinitions.finroc.getSubDomain("rpc");

    public PullCall() {
        super(/*MAX_CALL_DEPTH*/);
        reset();
    }

//  public static void staticInit() {
//      // JavaOnlyBlock
//      METHOD_TYPE = DataTypeRegister.getInstance().addDataType(MethodCall.class);
//
//      //Cpp METHOD_TYPE = DataTypeRegister::getInstance()->addDataType<MethodCall>("MethodCall");
//  }
//
//  @Override @JavaOnly
//  public DataType getType() {
//      return METHOD_TYPE;
//  }

    /**
     * Reset all variable in order to reuse object
     */
    private void reset() {
        recycleParameters();
//      data = null;
//      ccData = null;
//      info.curRef = null;
//      info.curRefCounter = null;
//      info.lockEstimate = 5;
//      info.setLocks = 1;
//      tc = null;
    }

//  /**
//   * Deserializes PullCall.
//   * If skipObject is true - the call's object (when returning) is not deserialized.
//   * In this case, the caller should skip the stream to the next mark.
//   *
//   * @param is Input Stream
//   * @param skipObject Skip Object? (see above)
//   */
//  public void possiblyIncompleteDeserialize(@Ref CoreInput is, boolean skipObject) {
//      super.deserialize(is);
//      intermediateAssign = is.readBoolean();
//      ccPull = is.readBoolean();
//      if (!skipObject && isReturning(true)) {
//          if (ccPull) {
//              data = (CCInterThreadContainer<?>)is.readObjectInInterThreadContainer();
//          } else {
//              PortData tmp = (PortData)is.readObject();
//              if (tmp != null) {
//                  info.curRef = tmp.getCurReference();
//                  //info.lockEstimate = 5; // just take 5... performance is not critical here
//                  //info.setLocks = 1; // one for this call
//                  info.curRefCounter = info.curRef.getRefCounter();
//                  info.curRefCounter.setLocks((byte)5);
//              } else {
//                  info.curRef = null;
//                  //info.lockEstimate = 5; // just take 5... performance is not critical here
//                  //info.setLocks = 1; // one for this call
//                  info.curRefCounter = null;
//              }
//          }
//      }
//  }

    /* (non-Javadoc)
     * @see core.port7.rpc.AbstractCall#deserialize(core.buffers.CoreInput)
     */
    @Override
    public void deserialize(CoreInput is) {
        super.deserialize(is);
        intermediateAssign = is.readBoolean();
        ccPull = is.readBoolean();
    }

    /* (non-Javadoc)
     * @see core.port7.rpc.AbstractCall#serialize(core.buffers.CoreBuffer)
     */
    @Override
    public void serialize(CoreOutput oos) {
        super.serialize(oos);
        oos.writeBoolean(intermediateAssign);
        oos.writeBoolean(ccPull);
//      if (isReturning(true)) {
//          oos.writeObject(ccPull ? (TypedObject)data : (TypedObject)info.curRef.getManager().getData());
//      }
    }

    /**
     * Prepare Execution of call received over network in extra thread
     *
     * @param port Port to execute pull on and to return value over later
     */
    public void prepareForExecution(NetPort port) {
        this.port = port;
    }

    @Override
    public void executeTask() {
        assert(port != null);
        synchronized (port.getPort()) {
            if (!port.getPort().isReady()) {
                log(LogLevel.LL_DEBUG, logDomain, "pull call received for port that will soon be deleted");
                recycle();
            }

            if (port.getPort().getDataType().isCCType()) {
                CCPortBase cp = (CCPortBase)port.getPort();
                CCInterThreadContainer<?> cpd = cp.getPullInInterthreadContainerRaw(true);
                recycleParameters();

                //JavaOnlyBlock
                addParamForSending(cpd);

                //Cpp addParamForSending(cpd);

                sendParametersComplete();
                setStatusReturn();
                port.sendCallReturn(this);
            } else if (port.getPort().getDataType().isStdType()) {
                PortBase p = (PortBase)port.getPort();
                @Const PortData pd = p.getPullLockedUnsafe(true);
                recycleParameters();

                //JavaOnlyBlock
                addParamForSending(pd);

                //Cpp addParamForSending(pd);

                sendParametersComplete();
                setStatusReturn();
                port.sendCallReturn(this);
            } else {
                log(LogLevel.LL_WARNING, logDomain, "pull call received for port with invalid data type");
                recycle();
            }
        }
    }

//  @Override
//  public void genericRecycle() {
//      if (isResponsible()) {
//          //System.out.println("Recycling pull call: " + toString());
//          if (ccPull) {
//              if (data != null) {
//                  data.recycle2();
//              }
//          } else if (info.curRef != null) {
//              info.setLocks--; // release pull call's lock
//              info.releaseObsoleteLocks();
//          }
//          reset();
//          super.recycle();
//      }
//  }

//  /**
//   * Initializes thread local cache in order to perform assignments in current runtime environment
//   *
//   * New buffer in thread local cache won't be locked - since only current thread may recycle it
//   */
//  @InCppFile
//  public void setupThreadLocalCache() {
//      if (tc == null) {
//          tc = ThreadLocalCache.getFast();
//          tc.data = tc.getUnusedBuffer(data.getType());
//          tc.data.setRefCounter(0);
//          tc.data.assign(data.getDataPtr());
//          tc.ref = tc.data.getCurrentRef();
//      } else {
//          assert(ThreadLocalCache.getFast() == tc) : "Programming error";
//      }
//  }

    public String toString() {
        return "PullCall (" + getStatusString() + ", callid: " + super.getMethodCallIndex() + ", threaduid: " + super.getThreadUid() + ")";
    }
}
