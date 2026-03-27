/**
 * Process.java
 * Represents a single process in the OS simulation.
 * Holds all scheduling-related attributes and the current state.
 */
public class Process {

    // ── Core identifiers ──────────────────────────────────────────────────────
    private final String pid;     // Process ID (e.g. "P1")
    private int arrivalTime;      // Time at which the process arrives in the system
    private int burstTime;        // Total CPU time the process requires
    private int priority;         // Lower number = higher priority (used in Priority scheduling)

    // ── Runtime attributes (updated during simulation) ───────────────────────
    private int remainingTime;    // CPU time still needed
    private int completionTime;   // Time at which the process finished
    private int waitingTime;      // Total time spent waiting in the ready queue
    private int turnaroundTime;   // completionTime - arrivalTime

    // ── Process lifecycle state ───────────────────────────────────────────────
    private ProcessState state;

    // ── Constructor ───────────────────────────────────────────────────────────
    /**
     * Creates a new Process.
     *
     * @param pid         Unique process identifier string
     * @param arrivalTime Time unit when this process enters the system
     * @param burstTime   Total CPU burst required
     * @param priority    Scheduling priority (lower = higher priority)
     */
    public Process(String pid, int arrivalTime, int burstTime, int priority) {
        this.pid           = pid;
        this.arrivalTime   = arrivalTime;
        this.burstTime     = burstTime;
        this.priority      = priority;
        this.remainingTime = burstTime;  // Initially equals burst time
        this.state         = ProcessState.NEW;
    }

    // ── Copy constructor (used by schedulers to avoid mutating original list) ─
    public Process(Process other) {
        this.pid            = other.pid;
        this.arrivalTime    = other.arrivalTime;
        this.burstTime      = other.burstTime;
        this.priority       = other.priority;
        this.remainingTime  = other.remainingTime;
        this.completionTime = other.completionTime;
        this.waitingTime    = other.waitingTime;
        this.turnaroundTime = other.turnaroundTime;
        this.state          = other.state;
    }

    // ── Resets runtime fields so a process can be re-scheduled ───────────────
    public void reset() {
        this.remainingTime  = this.burstTime;
        this.completionTime = 0;
        this.waitingTime    = 0;
        this.turnaroundTime = 0;
        this.state          = ProcessState.NEW;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String       getPid()            { return pid; }
    public int          getArrivalTime()    { return arrivalTime; }
    public int          getBurstTime()      { return burstTime; }
    public int          getPriority()       { return priority; }
    public int          getRemainingTime()  { return remainingTime; }
    public int          getCompletionTime() { return completionTime; }
    public int          getWaitingTime()    { return waitingTime; }
    public int          getTurnaroundTime() { return turnaroundTime; }
    public ProcessState getState()          { return state; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setRemainingTime(int t)  { this.remainingTime  = t; }
    public void setCompletionTime(int t) { this.completionTime = t; }
    public void setWaitingTime(int t)    { this.waitingTime    = t; }
    public void setTurnaroundTime(int t) { this.turnaroundTime = t; }
    public void setState(ProcessState s) { this.state          = s; }

    @Override
    public String toString() {
        return String.format("Process[%s | Arrival=%d | Burst=%d | Priority=%d | State=%s]",
                pid, arrivalTime, burstTime, priority, state);
    }
}
