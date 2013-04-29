package org.finroc.core.port.rpc;

/**
 * Status of call a future is waiting for
 */
public enum FutureStatus {

    PENDING, //!< value is yet to be returned
    READY,   //!< value is ready and can be obtained

    // Exceptions
    NO_CONNECTION,         //!< There is no server port connected to client port
    TIMEOUT,               //!< Call timed out
    BROKEN_PROMISE,        //!< Promise was destructed and did not provide any value before
    INVALID_FUTURE,        //!< Called on an invalid future object
    INTERNAL_ERROR,        //!< Internal error; if this occurs, there is a bug in the finroc implementation
    INVALID_CALL,          //!< Function was called that was not allowed
    INVALID_DATA_RECEIVED  //!< Invalid data received from other process (via network)
}
