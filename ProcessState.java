/**
 * ProcessState.java
 * Enum representing the lifecycle states of a process in the OS simulation.
 */
public enum ProcessState {
    NEW,        // Process has just been created
    READY,      // Process is in the ready queue, waiting for CPU
    RUNNING,    // Process is currently being executed by the CPU
    WAITING,    // Process is waiting for I/O or other events
    TERMINATED  // Process has finished execution
}
