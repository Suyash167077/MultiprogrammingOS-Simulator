import java.util.*;

/**
 * Scheduler.java
 * ──────────────────────────────────────────────────────────────────────────────
 * Houses all CPU-scheduling algorithm implementations.
 *
 * Each method operates on a fresh copy of the process list so the original
 * data is never mutated during simulation. All methods return:
 *   • A List<int[]> representing the Gantt chart. Each entry is:
 *         { pidIndex, startTime, endTime }
 *     where pidIndex is the 0-based index into the sorted process list.
 *   • The Process objects in the list passed in are updated in place with
 *     computed waiting time, turnaround time, completion time, and final state.
 *
 * State-transition log messages are appended to a provided StringBuilder.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class Scheduler {

    // ── Helper: deep-copy a list of Process objects ───────────────────────────
    private static List<Process> deepCopy(List<Process> original) {
        List<Process> copy = new ArrayList<>();
        for (Process p : original) {
            Process clone = new Process(p.getPid(), p.getArrivalTime(),
                                        p.getBurstTime(), p.getPriority());
            clone.setRemainingTime(p.getBurstTime());
            copy.add(clone);
        }
        return copy;
    }

    // ── Helper: log a state transition ────────────────────────────────────────
    private static void log(StringBuilder sb, int time, String pid, ProcessState from, ProcessState to) {
        sb.append(String.format("Time %3d : %-4s  %-12s → %s%n",
                time, pid, from, to));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. FIRST COME FIRST SERVED (FCFS) — non-preemptive
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Runs FCFS scheduling on a copy of the process list.
     *
     * @param processes List of processes (read-only — results written back via index)
     * @param results   Same list — results (WT, TAT, CT, state) written here
     * @param log       StringBuilder to append state-transition messages
     * @return Gantt chart list: each int[] = { pidIndex, startTime, endTime }
     */
    public static List<int[]> runFCFS(List<Process> processes,
                                       List<Process> results,
                                       StringBuilder log) {
        // Sort by arrival time
        List<Process> sorted = deepCopy(processes);
        sorted.sort(Comparator.comparingInt(Process::getArrivalTime));
        results.clear();
        results.addAll(sorted);

        List<int[]> gantt = new ArrayList<>();
        int currentTime = 0;

        for (int i = 0; i < sorted.size(); i++) {
            Process p = sorted.get(i);

            // CPU idle gap if needed
            if (currentTime < p.getArrivalTime()) {
                currentTime = p.getArrivalTime();
            }

            // NEW → READY
            p.setState(ProcessState.READY);
            log.append(String.format("Time %3d : %-4s  NEW          → READY%n", p.getArrivalTime(), p.getPid()));

            // READY → RUNNING
            log.append(String.format("Time %3d : %-4s  READY        → RUNNING%n", currentTime, p.getPid()));
            p.setState(ProcessState.RUNNING);

            int start = currentTime;
            currentTime += p.getBurstTime();
            int end = currentTime;

            // RUNNING → TERMINATED
            p.setState(ProcessState.TERMINATED);
            log.append(String.format("Time %3d : %-4s  RUNNING      → TERMINATED%n", end, p.getPid()));

            p.setCompletionTime(end);
            p.setTurnaroundTime(end - p.getArrivalTime());
            p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());

            gantt.add(new int[]{i, start, end});
        }
        return gantt;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. SHORTEST JOB FIRST (SJF) — non-preemptive
    // ═════════════════════════════════════════════════════════════════════════
    public static List<int[]> runSJF(List<Process> processes,
                                      List<Process> results,
                                      StringBuilder log) {
        List<Process> pool = deepCopy(processes);
        // Sort by arrival first to preserve arrival-order tiebreak
        pool.sort(Comparator.comparingInt(Process::getArrivalTime));
        results.clear();
        results.addAll(pool);

        List<int[]> gantt  = new ArrayList<>();
        List<Process> done = new ArrayList<>();
        int currentTime    = 0;
        int completed      = 0;
        int n              = pool.size();

        while (completed < n) {
            // Find shortest burst among arrived, not-yet-done processes
            Process shortest = null;
            for (Process p : pool) {
                if (!done.contains(p) && p.getArrivalTime() <= currentTime) {
                    if (shortest == null || p.getBurstTime() < shortest.getBurstTime()
                            || (p.getBurstTime() == shortest.getBurstTime()
                                && p.getArrivalTime() < shortest.getArrivalTime())) {
                        shortest = p;
                    }
                }
            }

            if (shortest == null) {
                // CPU idle — jump to next arrival
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : pool) {
                    if (!done.contains(p)) nextArrival = Math.min(nextArrival, p.getArrivalTime());
                }
                currentTime = nextArrival;
                continue;
            }

            int idx = results.indexOf(shortest);

            // Log transitions
            log.append(String.format("Time %3d : %-4s  NEW          → READY%n", shortest.getArrivalTime(), shortest.getPid()));
            log.append(String.format("Time %3d : %-4s  READY        → RUNNING%n", currentTime, shortest.getPid()));
            shortest.setState(ProcessState.RUNNING);

            int start = currentTime;
            currentTime += shortest.getBurstTime();
            int end = currentTime;

            shortest.setState(ProcessState.TERMINATED);
            log.append(String.format("Time %3d : %-4s  RUNNING      → TERMINATED%n", end, shortest.getPid()));

            shortest.setCompletionTime(end);
            shortest.setTurnaroundTime(end - shortest.getArrivalTime());
            shortest.setWaitingTime(shortest.getTurnaroundTime() - shortest.getBurstTime());

            gantt.add(new int[]{idx, start, end});
            done.add(shortest);
            completed++;
        }
        return gantt;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. PRIORITY SCHEDULING — preemptive (lower priority number = higher priority)
    // ═════════════════════════════════════════════════════════════════════════
    public static List<int[]> runPriority(List<Process> processes,
                                           List<Process> results,
                                           StringBuilder log) {
        List<Process> pool = deepCopy(processes);
        pool.sort(Comparator.comparingInt(Process::getArrivalTime));
        results.clear();
        results.addAll(pool);

        List<int[]> gantt = new ArrayList<>();
        int currentTime   = 0;
        int completed     = 0;
        int n             = pool.size();
        int lastRunIdx    = -1;
        int blockStart    = 0;

        // We simulate tick-by-tick for preemption
        while (completed < n) {
            // Mark newly arrived processes as READY
            for (Process p : pool) {
                if (p.getArrivalTime() == currentTime && p.getState() == ProcessState.NEW) {
                    p.setState(ProcessState.READY);
                    log.append(String.format("Time %3d : %-4s  NEW          → READY%n", currentTime, p.getPid()));
                }
            }

            // Pick process with highest priority (lowest number) from READY
            Process running = null;
            for (Process p : pool) {
                if (p.getState() == ProcessState.READY || p.getState() == ProcessState.RUNNING) {
                    if (running == null || p.getPriority() < running.getPriority()
                            || (p.getPriority() == running.getPriority()
                                && p.getArrivalTime() < running.getArrivalTime())) {
                        running = p;
                    }
                }
            }

            if (running == null) {
                currentTime++;
                continue;
            }

            int runningIdx = results.indexOf(running);

            // Detect preemption / start of new block
            if (runningIdx != lastRunIdx) {
                if (lastRunIdx != -1 && blockStart < currentTime) {
                    gantt.add(new int[]{lastRunIdx, blockStart, currentTime});
                }
                if (running.getState() != ProcessState.RUNNING) {
                    log.append(String.format("Time %3d : %-4s  READY        → RUNNING%n", currentTime, running.getPid()));
                }
                // Preempt previously running if it hasn't finished
                for (Process p : pool) {
                    if (p.getState() == ProcessState.RUNNING && p != running) {
                        p.setState(ProcessState.READY);
                        log.append(String.format("Time %3d : %-4s  RUNNING      → READY (preempted)%n", currentTime, p.getPid()));
                    }
                }
                running.setState(ProcessState.RUNNING);
                blockStart = currentTime;
                lastRunIdx = runningIdx;
            }

            // Execute one tick
            running.setRemainingTime(running.getRemainingTime() - 1);
            currentTime++;

            // Mark newly arrived after tick
            for (Process p : pool) {
                if (p.getArrivalTime() == currentTime && p.getState() == ProcessState.NEW) {
                    p.setState(ProcessState.READY);
                    log.append(String.format("Time %3d : %-4s  NEW          → READY%n", currentTime, p.getPid()));
                }
            }

            if (running.getRemainingTime() == 0) {
                gantt.add(new int[]{runningIdx, blockStart, currentTime});
                running.setState(ProcessState.TERMINATED);
                log.append(String.format("Time %3d : %-4s  RUNNING      → TERMINATED%n", currentTime, running.getPid()));
                running.setCompletionTime(currentTime);
                running.setTurnaroundTime(currentTime - running.getArrivalTime());
                running.setWaitingTime(running.getTurnaroundTime() - running.getBurstTime());
                lastRunIdx = -1;
                completed++;
            }
        }
        return gantt;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. ROUND ROBIN — with configurable time quantum
    // ═════════════════════════════════════════════════════════════════════════
    public static List<int[]> runRoundRobin(List<Process> processes,
                                             List<Process> results,
                                             int quantum,
                                             StringBuilder log) {
        List<Process> pool = deepCopy(processes);
        pool.sort(Comparator.comparingInt(Process::getArrivalTime));
        results.clear();
        results.addAll(pool);

        List<int[]> gantt    = new ArrayList<>();
        Queue<Process> ready = new LinkedList<>();
        int currentTime      = 0;
        int completed        = 0;
        int n                = pool.size();
        Set<Process> inQueue = new HashSet<>();
        List<Process> remaining = new ArrayList<>(pool);

        // Enqueue processes that arrive at time 0
        for (Process p : pool) {
            if (p.getArrivalTime() <= currentTime && !inQueue.contains(p)) {
                p.setState(ProcessState.READY);
                log.append(String.format("Time %3d : %-4s  NEW          → READY%n", p.getArrivalTime(), p.getPid()));
                ready.add(p);
                inQueue.add(p);
            }
        }

        while (completed < n) {
            if (ready.isEmpty()) {
                // CPU idle — jump to next arrival
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : remaining) {
                    if (p.getState() != ProcessState.TERMINATED)
                        nextArrival = Math.min(nextArrival, p.getArrivalTime());
                }
                currentTime = nextArrival;
                for (Process p : pool) {
                    if (p.getArrivalTime() <= currentTime && !inQueue.contains(p)
                            && p.getState() != ProcessState.TERMINATED) {
                        p.setState(ProcessState.READY);
                        log.append(String.format("Time %3d : %-4s  NEW          → READY%n", p.getArrivalTime(), p.getPid()));
                        ready.add(p);
                        inQueue.add(p);
                    }
                }
                continue;
            }

            Process current = ready.poll();
            int idx         = results.indexOf(current);
            int execTime    = Math.min(quantum, current.getRemainingTime());

            log.append(String.format("Time %3d : %-4s  READY        → RUNNING%n", currentTime, current.getPid()));
            current.setState(ProcessState.RUNNING);

            int start = currentTime;
            currentTime += execTime;
            current.setRemainingTime(current.getRemainingTime() - execTime);

            // Enqueue any processes that arrived during this quantum
            for (Process p : pool) {
                if (p.getArrivalTime() <= currentTime && !inQueue.contains(p)
                        && p.getState() != ProcessState.TERMINATED) {
                    p.setState(ProcessState.READY);
                    log.append(String.format("Time %3d : %-4s  NEW          → READY%n", p.getArrivalTime(), p.getPid()));
                    ready.add(p);
                    inQueue.add(p);
                }
            }

            if (current.getRemainingTime() == 0) {
                current.setState(ProcessState.TERMINATED);
                log.append(String.format("Time %3d : %-4s  RUNNING      → TERMINATED%n", currentTime, current.getPid()));
                current.setCompletionTime(currentTime);
                current.setTurnaroundTime(currentTime - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
                completed++;
            } else {
                current.setState(ProcessState.READY);
                log.append(String.format("Time %3d : %-4s  RUNNING      → READY (quantum expired)%n", currentTime, current.getPid()));
                ready.add(current); // Re-enqueue at back
            }

            gantt.add(new int[]{idx, start, currentTime});
        }
        return gantt;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Metrics utility methods
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns average waiting time across all provided processes. */
    public static double avgWaitingTime(List<Process> processes) {
        double sum = 0;
        for (Process p : processes) sum += p.getWaitingTime();
        return sum / processes.size();
    }

    /** Returns average turnaround time across all provided processes. */
    public static double avgTurnaroundTime(List<Process> processes) {
        double sum = 0;
        for (Process p : processes) sum += p.getTurnaroundTime();
        return sum / processes.size();
    }

    /**
     * Returns CPU utilization as a percentage.
     * utilization = (total burst time) / (makespan) * 100
     */
    public static double cpuUtilization(List<Process> processes) {
        int totalBurst = 0;
        int makespan   = 0;
        for (Process p : processes) {
            totalBurst += p.getBurstTime();
            makespan = Math.max(makespan, p.getCompletionTime());
        }
        int firstArrival = Integer.MAX_VALUE;
        for (Process p : processes) firstArrival = Math.min(firstArrival, p.getArrivalTime());
        int span = makespan - firstArrival;
        if (span <= 0) return 0;
        return (double) totalBurst / span * 100.0;
    }
}
