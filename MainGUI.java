import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * MainGUI.java
 * ──────────────────────────────────────────────────────────────────────────────
 * The main application window for the Multiprogramming OS Simulator.
 *
 * Layout (3 vertical zones inside a JScrollPane):
 *   ┌──────────────────────────────────────────────────────┐
 *   │  Title bar                                           │
 *   ├────────────────────────┬─────────────────────────────┤
 *   │  Input Panel           │  Algorithm + Controls       │
 *   ├────────────────────────┴─────────────────────────────┤
 *   │  Process Table (JTable)                              │
 *   ├──────────────────────────────────────────────────────┤
 *   │  Gantt Chart Panel                                   │
 *   ├──────────────────────┬───────────────────────────────┤
 *   │  State Log           │  Metrics                      │
 *   └──────────────────────┴───────────────────────────────┘
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class MainGUI extends JFrame {

    // ── Colour palette (light, subtle theme) ──────────────────────────────────
    private static final Color C_BG        = new Color(242, 245, 252);  // soft lavender-white bg
    private static final Color C_PANEL     = new Color(255, 255, 255);  // white panels
    private static final Color C_CARD      = new Color(250, 252, 255);  // near-white cards
    private static final Color C_ACCENT    = new Color( 58, 110, 210);  // medium blue
    private static final Color C_SUCCESS   = new Color( 39, 155,  90);  // muted green
    private static final Color C_WARN      = new Color(215, 140,  20);  // muted amber
    private static final Color C_DANGER    = new Color(210,  50,  60);  // muted red
    private static final Color C_TEXT      = new Color( 22,  35,  80);  // deep navy
    private static final Color C_SUBTEXT   = new Color(100, 115, 165);  // muted blue-grey
    private static final Color C_BORDER    = new Color(205, 215, 235);  // light blue-grey
    private static final Color C_ROW_ALT   = new Color(245, 248, 255);  // very subtle tinted row
    private static final Color C_ROW_RUN   = new Color(214, 232, 255);  // soft blue highlight
    private static final Color C_ROW_DONE  = new Color(210, 244, 228);  // soft green highlight
    private static final Color C_TABLE_HDR = new Color(232, 238, 252);  // light blue header

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font F_TITLE  = new Font("Segoe UI", Font.BOLD,  22);
    private static final Font F_LABEL  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font F_INPUT  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BUTTON = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font F_LOG    = new Font("Consolas",  Font.PLAIN, 12);
    private static final Font F_METRIC = new Font("Segoe UI", Font.BOLD,  14);

    // ── Fixed colour palette for processes — slightly desaturated for light bg ─
    private static final Color[] PROCESS_COLORS = {
        new Color( 70, 130, 220), new Color( 50, 170, 110),
        new Color(220, 145,  40), new Color(210,  65,  70),
        new Color(140,  90, 210), new Color( 45, 175, 175),
        new Color(210, 110,  60), new Color(110, 180,  50),
        new Color(200,  80, 160), new Color( 60, 160, 210)
    };

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Process> masterProcessList = new ArrayList<>();
    private final Map<String, Color> colorMap     = new LinkedHashMap<>();

    // ── Input fields ──────────────────────────────────────────────────────────
    private JTextField tfPid, tfArrival, tfBurst, tfPriority, tfQuantum;

    // ── Algorithm selector ────────────────────────────────────────────────────
    private JComboBox<String> cbAlgorithm;
    private JLabel lblQuantum;

    // ── Process table ─────────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable processTable;

    // ── Gantt chart ───────────────────────────────────────────────────────────
    private GanttChartPanel ganttPanel;

    // ── State log ─────────────────────────────────────────────────────────────
    private JTextArea logArea;

    // ── Metrics labels ────────────────────────────────────────────────────────
    private JLabel lblAvgWT, lblAvgTAT, lblCPU;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────
    public MainGUI() {
        super("MultiOS Simulator — CPU Scheduling");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 780));
        setPreferredSize(new Dimension(1250, 860));
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout(0, 0));

        // Title bar
        add(buildTitleBar(), BorderLayout.NORTH);

        // Scrollable main content
        JPanel content = new JPanel();
        content.setBackground(C_BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 14, 14, 14));

        content.add(buildTopRow());
        content.add(Box.createVerticalStrut(12));
        content.add(buildTableSection());
        content.add(Box.createVerticalStrut(12));
        content.add(buildGanttSection());
        content.add(Box.createVerticalStrut(12));
        content.add(buildBottomRow());

        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setBackground(C_BG);
        mainScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Title bar
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(255, 255, 255));
        p.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(14, 20, 14, 20)));

        JLabel title = new JLabel("⚙  MultiOS Simulator");
        title.setFont(F_TITLE);
        title.setForeground(C_ACCENT);

        JLabel sub = new JLabel("CPU Scheduling Algorithm Visualizer — FCFS · SJF · Priority · Round Robin");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(C_SUBTEXT);

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);
        p.add(left, BorderLayout.WEST);

        // Version badge
        JLabel ver = new JLabel("v1.0  JDK 8+");
        ver.setFont(new Font("Segoe UI", Font.BOLD, 11));
        ver.setForeground(C_SUBTEXT);
        p.add(ver, BorderLayout.EAST);

        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top row: Input section + Algorithm/Controls section
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTopRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        row.add(buildInputCard());
        row.add(buildControlCard());
        return row;
    }

    /** Card: process input fields */
    private JPanel buildInputCard() {
        JPanel card = makeCard("➕  Add Process");

        // Lay out 2-column grid for labels + fields
        JPanel grid = new JPanel(new GridLayout(4, 2, 8, 10));
        grid.setOpaque(false);

        tfPid      = makeTextField("e.g. P1");
        tfArrival  = makeTextField("e.g. 0");
        tfBurst    = makeTextField("e.g. 8");
        tfPriority = makeTextField("e.g. 1  (lower = higher)");

        grid.add(makeLabel("Process ID"));   grid.add(tfPid);
        grid.add(makeLabel("Arrival Time")); grid.add(tfArrival);
        grid.add(makeLabel("Burst Time"));   grid.add(tfBurst);
        grid.add(makeLabel("Priority"));     grid.add(tfPriority);

        card.add(grid, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton btnAdd   = makeButton("Add Process",  C_ACCENT);
        JButton btnClear = makeButton("Clear Input",  C_SUBTEXT);
        btnRow.add(btnAdd);
        btnRow.add(btnClear);
        card.add(btnRow, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> addProcess());
        btnClear.addActionListener(e -> clearInputFields());

        return card;
    }

    /** Card: algorithm selector + run/reset buttons */
    private JPanel buildControlCard() {
        JPanel card = makeCard("🖥  Simulation Controls");

        JPanel inner = new JPanel(new GridLayout(3, 2, 8, 12));
        inner.setOpaque(false);

        cbAlgorithm = new JComboBox<>(new String[]{
            "FCFS — First Come First Served",
            "SJF  — Shortest Job First",
            "Priority Scheduling (Preemptive)",
            "Round Robin"
        });
        styleComboBox(cbAlgorithm);
        cbAlgorithm.addActionListener(e -> onAlgorithmChange());

        lblQuantum = makeLabel("Time Quantum");
        lblQuantum.setForeground(C_SUBTEXT); // disabled look initially
        tfQuantum  = makeTextField("e.g. 3");
        tfQuantum.setEnabled(false);
        tfQuantum.setForeground(C_SUBTEXT);

        inner.add(makeLabel("Algorithm"));    inner.add(cbAlgorithm);
        inner.add(lblQuantum);                inner.add(tfQuantum);
        inner.add(new JLabel());              inner.add(new JLabel()); // spacer

        card.add(inner, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        JButton btnRun   = makeButton("▶  Run Simulation", C_SUCCESS);
        JButton btnReset = makeButton("↺  Reset",          C_DANGER);
        btnRow.add(btnRun);
        btnRow.add(btnReset);
        card.add(btnRow, BorderLayout.SOUTH);

        btnRun.addActionListener(e   -> runSimulation());
        btnReset.addActionListener(e -> resetAll());

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process Table section
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTableSection() {
        JPanel wrapper = makeSectionPanel("📋  Process Queue");

        String[] cols = {
            "PID", "Arrival Time", "Burst Time", "Priority",
            "Remaining Time", "State", "Waiting Time", "Turnaround Time"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        processTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                // Row colouring based on state
                String state = (String) tableModel.getValueAt(row, 5);
                if ("RUNNING".equals(state)) {
                    c.setBackground(C_ROW_RUN);
                } else if ("TERMINATED".equals(state)) {
                    c.setBackground(C_ROW_DONE);
                } else if (row % 2 == 0) {
                    c.setBackground(C_PANEL);
                } else {
                    c.setBackground(C_ROW_ALT);
                }
                c.setForeground(C_TEXT);
                return c;
            }
        };

        styleTable(processTable);

        JScrollPane sp = new JScrollPane(processTable);
        sp.setPreferredSize(new Dimension(0, 200));
        sp.setBackground(C_PANEL);
        sp.getViewport().setBackground(C_PANEL);
        sp.setBorder(new LineBorder(C_BORDER, 1));

        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gantt chart section
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildGanttSection() {
        JPanel wrapper = makeSectionPanel("📊  Gantt Chart");

        ganttPanel = new GanttChartPanel();
        ganttPanel.setPreferredSize(new Dimension(0, 108));

        JScrollPane sp = new JScrollPane(ganttPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(new LineBorder(C_BORDER, 1));
        sp.getViewport().setBackground(ganttPanel.getBackground());

        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bottom row: State log + Metrics
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        // ── State log ────────────────────────────────────────────────────────
        JPanel logWrapper = makeSectionPanel("🗒  State Transition Log");
        logArea = new JTextArea();
        logArea.setFont(F_LOG);
        logArea.setBackground(new Color(248, 251, 255));
        logArea.setForeground(new Color( 40,  90,  55));
        logArea.setCaretColor(C_TEXT);
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        logArea.setText("State transitions will appear here after you run the simulation...\n");

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new LineBorder(C_BORDER, 1));
        logWrapper.add(logScroll, BorderLayout.CENTER);

        // ── Metrics ──────────────────────────────────────────────────────────
        JPanel metWrapper = makeCard("📈  Performance Metrics");
        JPanel metGrid    = new JPanel(new GridLayout(6, 1, 0, 10));
        metGrid.setOpaque(false);

        lblAvgWT  = makeMetricLabel("Avg Waiting Time",     "—");
        lblAvgTAT = makeMetricLabel("Avg Turnaround Time",  "—");
        lblCPU    = makeMetricLabel("CPU Utilization",      "—");

        metGrid.add(makeDivider("After Simulation"));
        metGrid.add(lblAvgWT);
        metGrid.add(lblAvgTAT);
        metGrid.add(lblCPU);
        metGrid.add(new JLabel());  // spacers
        metGrid.add(new JLabel());

        metWrapper.add(metGrid, BorderLayout.CENTER);

        row.add(logWrapper);
        row.add(metWrapper);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Event Handlers
    // ─────────────────────────────────────────────────────────────────────────

    /** Validates inputs and adds a Process to the master list + table. */
    private void addProcess() {
        String pid     = tfPid.getText().trim();
        String arrival = tfArrival.getText().trim();
        String burst   = tfBurst.getText().trim();
        String prio    = tfPriority.getText().trim();

        // ── Validation ───────────────────────────────────────────────────────
        if (pid.isEmpty() || arrival.isEmpty() || burst.isEmpty() || prio.isEmpty()) {
            showError("All fields are required. Please fill in PID, Arrival, Burst, and Priority.");
            return;
        }
        int arrivalVal, burstVal, prioVal;
        try {
            arrivalVal = Integer.parseInt(arrival);
            burstVal   = Integer.parseInt(burst);
            prioVal    = Integer.parseInt(prio);
        } catch (NumberFormatException ex) {
            showError("Arrival Time, Burst Time, and Priority must be integers.");
            return;
        }
        if (arrivalVal < 0) { showError("Arrival Time cannot be negative."); return; }
        if (burstVal   < 1) { showError("Burst Time must be at least 1."); return; }
        if (prioVal    < 0) { showError("Priority cannot be negative."); return; }

        // Check for duplicate PID
        for (Process p : masterProcessList) {
            if (p.getPid().equalsIgnoreCase(pid)) {
                showError("A process with PID \"" + pid + "\" already exists.");
                return;
            }
        }

        // ── Create and register ───────────────────────────────────────────────
        Process newProc = new Process(pid, arrivalVal, burstVal, prioVal);
        masterProcessList.add(newProc);

        // Assign colour
        Color col = PROCESS_COLORS[colorMap.size() % PROCESS_COLORS.length];
        colorMap.put(pid, col);

        // Add row to table
        tableModel.addRow(new Object[]{
            pid, arrivalVal, burstVal, prioVal,
            burstVal, ProcessState.NEW.name(), 0, 0
        });

        clearInputFields();
    }

    /** Clears only the text input fields. */
    private void clearInputFields() {
        tfPid.setText("");
        tfArrival.setText("");
        tfBurst.setText("");
        tfPriority.setText("");
        tfPid.requestFocus();
    }

    /** Enables/disables Time Quantum field based on selected algorithm. */
    private void onAlgorithmChange() {
        boolean isRR = cbAlgorithm.getSelectedIndex() == 3;
        tfQuantum.setEnabled(isRR);
        tfQuantum.setForeground(isRR ? C_TEXT : C_SUBTEXT);
        lblQuantum.setForeground(isRR ? C_TEXT : C_SUBTEXT);
    }

    /** Runs the selected scheduling algorithm and updates all UI sections. */
    private void runSimulation() {
        if (masterProcessList.isEmpty()) {
            showError("No processes to schedule. Please add at least one process.");
            return;
        }

        // Read quantum if Round Robin
        int quantum = 1;
        if (cbAlgorithm.getSelectedIndex() == 3) {
            try {
                quantum = Integer.parseInt(tfQuantum.getText().trim());
                if (quantum < 1) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError("Time Quantum must be a positive integer.");
                return;
            }
        }

        // Reset all processes before scheduling
        for (Process p : masterProcessList) p.reset();

        List<Process> results = new ArrayList<>();
        StringBuilder stateLog = new StringBuilder();
        List<int[]> gantt;

        // ── Dispatch to chosen algorithm ──────────────────────────────────────
        switch (cbAlgorithm.getSelectedIndex()) {
            case 0: gantt = Scheduler.runFCFS(masterProcessList, results, stateLog); break;
            case 1: gantt = Scheduler.runSJF(masterProcessList, results, stateLog);  break;
            case 2: gantt = Scheduler.runPriority(masterProcessList, results, stateLog); break;
            case 3: gantt = Scheduler.runRoundRobin(masterProcessList, results, quantum, stateLog); break;
            default: return;
        }

        // ── Update master list with computed metrics ───────────────────────────
        for (Process res : results) {
            for (Process master : masterProcessList) {
                if (master.getPid().equals(res.getPid())) {
                    master.setCompletionTime(res.getCompletionTime());
                    master.setWaitingTime(res.getWaitingTime());
                    master.setTurnaroundTime(res.getTurnaroundTime());
                    master.setRemainingTime(res.getRemainingTime());
                    master.setState(res.getState());
                    break;
                }
            }
        }

        // ── Refresh the table ─────────────────────────────────────────────────
        refreshTable(results);

        // ── Update Gantt chart ────────────────────────────────────────────────
        List<String> pidOrder = new ArrayList<>();
        for (Process p : results) pidOrder.add(p.getPid());
        ganttPanel.setData(gantt, pidOrder, colorMap);

        // ── Update state log ──────────────────────────────────────────────────
        logArea.setText(stateLog.toString());
        logArea.setCaretPosition(0);

        // ── Update metrics ────────────────────────────────────────────────────
        double awt  = Scheduler.avgWaitingTime(results);
        double atat = Scheduler.avgTurnaroundTime(results);
        double cpu  = Scheduler.cpuUtilization(results);
        lblAvgWT.setText("<html><b style='color:#2255aa;'>Avg Waiting Time:</b>"
                + "  <span style='color:#162050;'>" + String.format("%.2f", awt) + " units</span></html>");
        lblAvgTAT.setText("<html><b style='color:#2255aa;'>Avg Turnaround Time:</b>"
                + "  <span style='color:#162050;'>" + String.format("%.2f", atat) + " units</span></html>");
        lblCPU.setText("<html><b style='color:#2255aa;'>CPU Utilization:</b>"
                + "  <span style='color:#1a7a46;'>" + String.format("%.1f", cpu) + " %%</span></html>");
    }

    /** Refreshes each row in the JTable from the results list. */
    private void refreshTable(List<Process> results) {
        tableModel.setRowCount(0);
        for (Process p : results) {
            tableModel.addRow(new Object[]{
                p.getPid(),
                p.getArrivalTime(),
                p.getBurstTime(),
                p.getPriority(),
                p.getRemainingTime(),
                p.getState().name(),
                p.getWaitingTime(),
                p.getTurnaroundTime()
            });
        }
    }

    /** Resets everything to the initial empty state. */
    private void resetAll() {
        masterProcessList.clear();
        colorMap.clear();
        tableModel.setRowCount(0);
        ganttPanel.setData(null, null, null);
        logArea.setText("State transitions will appear here after you run the simulation...\n");
        lblAvgWT.setText("<html><b style='color:#2255aa;'>Avg Waiting Time:</b>  —</html>");
        lblAvgTAT.setText("<html><b style='color:#2255aa;'>Avg Turnaround Time:</b>  —</html>");
        lblCPU.setText("<html><b style='color:#2255aa;'>CPU Utilization:</b>  —</html>");
        clearInputFields();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Builder Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a styled card JPanel with a title and BorderLayout. */
    private JPanel makeCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(F_LABEL);
        lbl.setForeground(C_ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(lbl, BorderLayout.NORTH);

        return card;
    }

    /** Creates a section-level panel (slightly different shade) with a heading. */
    private JPanel makeSectionPanel(String title) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(C_PANEL);
        p.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lbl = new JLabel(title);
        lbl.setFont(F_LABEL);
        lbl.setForeground(C_ACCENT);
        p.add(lbl, BorderLayout.NORTH);

        return p;
    }

    /** Creates a styled JLabel for form labels. */
    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(C_TEXT);
        return l;
    }

    /** Creates a divider label used in the metrics panel. */
    private JLabel makeDivider(String text) {
        JLabel l = new JLabel("── " + text + " ──");
        l.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        l.setForeground(C_SUBTEXT);
        return l;
    }

    /** Creates a metric row label with initial "—" value placeholder. */
    private JLabel makeMetricLabel(String key, String val) {
        JLabel l = new JLabel("<html><b style='color:#2255aa;'>" + key + ":</b>  " + val + "</html>");
        l.setFont(F_METRIC);
        l.setForeground(C_TEXT);
        return l;
    }

    /** Creates a light-styled JTextField with placeholder. */
    private JTextField makeTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(F_INPUT);
        tf.setBackground(Color.WHITE);
        tf.setForeground(C_TEXT);
        tf.setCaretColor(C_TEXT);
        tf.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(5, 8, 5, 8)));

        // Placeholder behaviour
        tf.setForeground(C_SUBTEXT);
        tf.setText(placeholder);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(C_TEXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) {
                    tf.setForeground(C_SUBTEXT);
                    tf.setText(placeholder);
                }
            }
        });
        return tf;
    }

    /** Returns the actual typed text, ignoring placeholder. */
    private String getFieldText(JTextField tf) {
        // NOTE: actual text read directly in addProcess/runSimulation
        return tf.getText();
    }

    /** Creates a styled JButton (light theme: filled accent, white text). */
    private JButton makeButton(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setFont(F_BUTTON);
        btn.setBackground(accent);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));

        // Hover effect — slightly lighter on hover
        Color normal = accent;
        Color hover  = accent.brighter();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(normal); }
        });
        return btn;
    }

    /** Styles a JComboBox to match the light theme. */
    private void styleComboBox(JComboBox<String> cb) {
        cb.setFont(F_INPUT);
        cb.setBackground(Color.WHITE);
        cb.setForeground(C_TEXT);
        cb.setBorder(new LineBorder(C_BORDER, 1));
        cb.setFocusable(false);
        // Style the drop-down list
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? C_ACCENT : Color.WHITE);
                setForeground(isSelected ? Color.WHITE : C_TEXT);
                setBorder(new EmptyBorder(5, 8, 5, 8));
                setFont(F_INPUT);
                return this;
            }
        });
        ((JComponent) cb.getEditor().getEditorComponent()).setBackground(Color.WHITE);
    }

    /** Applies light theme styling to a JTable. */
    private void styleTable(JTable table) {
        table.setBackground(C_PANEL);
        table.setForeground(C_TEXT);
        table.setFont(F_INPUT);
        table.setRowHeight(30);
        table.setGridColor(C_BORDER);
        table.setSelectionBackground(new Color(190, 215, 255));
        table.setSelectionForeground(C_TEXT);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Header style
        JTableHeader header = table.getTableHeader();
        header.setFont(F_LABEL);
        header.setBackground(C_TABLE_HDR);
        header.setForeground(C_ACCENT);
        header.setBorder(new MatteBorder(0, 0, 2, 0, C_ACCENT));
        header.setReorderingAllowed(false);

        // Centre-align all columns
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centre);
        }
    }

    /** Displays an error dialog. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // main
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Use system look-and-feel for native widget rendering on light theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
    }
}
