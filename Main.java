/**
 * Main.java
 * ──────────────────────────────────────────────────────────────────────────────
 * Application entry point.
 * Launches the MainGUI window on the Swing Event Dispatch Thread (EDT)
 * to ensure thread safety as required by Swing.
 * ──────────────────────────────────────────────────────────────────────────────
 */
public class Main {

    public static void main(String[] args) {
        // All Swing UI creation must happen on the EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainGUI window = new MainGUI();
            window.setVisible(true);
        });
    }
}
