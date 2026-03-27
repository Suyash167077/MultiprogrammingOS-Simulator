import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GanttChartPanel.java
 * ──────────────────────────────────────────────────────────────────────────────
 * A custom JPanel that renders a coloured Gantt chart.
 *
 * Data contract:
 *   • ganttBlocks  – List<int[]> where each int[] = { pidIndex, startTime, endTime }
 *   • pidLabels    – ordered list of PID strings (index matches pidIndex above)
 *   • colorMap     – maps PID string → Color for block fill
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class GanttChartPanel extends JPanel {

    // ── Visual constants ───────────────────────────────────────────────────────
    private static final int BLOCK_HEIGHT  = 48;
    private static final int TOP_MARGIN    = 20;
    private static final int BOTTOM_MARGIN = 36;
    private static final int LEFT_MARGIN   = 10;
    private static final int MIN_BLOCK_W   = 40;  // minimum px per unit time
    private static final Font LABEL_FONT   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font TIME_FONT    = new Font("Segoe UI", Font.PLAIN, 11);

    // ── Data ───────────────────────────────────────────────────────────────────
    private List<int[]>   ganttBlocks; // { pidIndex, start, end }
    private List<String>  pidLabels;
    private Map<String, Color> colorMap;
    private int totalTime = 0;

    // ── Background colour that matches the dark theme ──────────────────────────
    private static final Color BG = new Color(248, 251, 255);  // soft warm white (light theme)

    public GanttChartPanel() {
        setBackground(BG);
        setPreferredSize(new Dimension(800, TOP_MARGIN + BLOCK_HEIGHT + BOTTOM_MARGIN));
    }

    /**
     * Provide new Gantt data and repaint the panel.
     *
     * @param ganttBlocks List of {pidIndex, start, end} entries
     * @param pidLabels   PID strings indexed by pidIndex
     * @param colorMap    PID → Color
     */
    public void setData(List<int[]> ganttBlocks, List<String> pidLabels,
                        Map<String, Color> colorMap) {
        this.ganttBlocks = ganttBlocks;
        this.pidLabels   = pidLabels;
        this.colorMap    = colorMap;
        this.totalTime   = 0;
        if (ganttBlocks != null) {
            for (int[] b : ganttBlocks) totalTime = Math.max(totalTime, b[2]);
        }
        // Adjust preferred width dynamically
        int pxPerUnit = calcPxPerUnit();
        setPreferredSize(new Dimension(
                LEFT_MARGIN + totalTime * pxPerUnit + LEFT_MARGIN,
                TOP_MARGIN + BLOCK_HEIGHT + BOTTOM_MARGIN));
        revalidate();
        repaint();
    }

    /** Pixels per time unit, at least MIN_BLOCK_W. */
    private int calcPxPerUnit() {
        if (totalTime == 0) return MIN_BLOCK_W;
        int available = Math.max(getWidth() - LEFT_MARGIN * 2, 200);
        return Math.max(MIN_BLOCK_W, available / totalTime);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (ganttBlocks == null || ganttBlocks.isEmpty()) {
            drawPlaceholder(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pxPerUnit = calcPxPerUnit();
        int y         = TOP_MARGIN;

        // ── Draw each Gantt block ─────────────────────────────────────────────
        for (int[] block : ganttBlocks) {
            int pidIdx    = block[0];
            int startTime = block[1];
            int endTime   = block[2];

            String pid = (pidLabels != null && pidIdx < pidLabels.size())
                    ? pidLabels.get(pidIdx) : "?";
            Color base = (colorMap != null && colorMap.containsKey(pid))
                    ? colorMap.get(pid) : new Color(100, 100, 200);

            int x = LEFT_MARGIN + startTime * pxPerUnit;
            int w = (endTime - startTime) * pxPerUnit;

            // Block fill with gradient
            GradientPaint gp = new GradientPaint(
                    x, y, base.brighter(),
                    x, y + BLOCK_HEIGHT, base.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, w - 2, BLOCK_HEIGHT, 12, 12);

            // Block border
            g2.setColor(base.darker().darker());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, w - 2, BLOCK_HEIGHT, 12, 12);

            // PID label centred in block
            g2.setFont(LABEL_FONT);
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (w - 2 - fm.stringWidth(pid)) / 2;
            int ty = y + (BLOCK_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(pid, tx, ty);
        }

        // ── Draw time markers below blocks ────────────────────────────────────
        g2.setFont(TIME_FONT);
        g2.setColor(new Color(180, 190, 220));
        Set<Integer> drawn = new java.util.HashSet<>();
        for (int[] block : ganttBlocks) {
            for (int t : new int[]{block[1], block[2]}) {
                if (drawn.add(t)) {
                    int mx = LEFT_MARGIN + t * pxPerUnit;
                    // Tick mark
                    g2.setColor(new Color(140, 150, 190));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine(mx, y + BLOCK_HEIGHT, mx, y + BLOCK_HEIGHT + 8);
                    // Time label
                    g2.setColor(new Color(180, 190, 220));
                    String label = String.valueOf(t);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(label, mx - fm.stringWidth(label) / 2,
                            y + BLOCK_HEIGHT + 22);
                }
            }
        }
    }

    private void drawPlaceholder(Graphics g) {
        g.setColor(new Color(160, 170, 200));
        g.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        FontMetrics fm = g.getFontMetrics();
        String msg = "Gantt chart will appear here after simulation";
        g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2,
                TOP_MARGIN + BLOCK_HEIGHT / 2 + fm.getAscent() / 2);
    }
}
