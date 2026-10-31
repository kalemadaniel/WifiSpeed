import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.BoxLayout;
import javax.swing.JTabbedPane;

/**
 *
 * @author Cyborg
 */
public class WifiSpeedApp extends JFrame {
    private static final long MAX_BYTES = 25000000L;
    private final JButton scan = new JButton("Scanner le Wi-Fi");
    private final JButton speed = new JButton("Tester le d\u00e9bit (25 Mo max.)");
    private final JTextArea output = new JTextArea();
    private final JTextArea networks = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel speedValue = new JLabel("\u2014 Mbit/s");
    private final JLabel volumeValue = new JLabel("\u2014 Mo");
    private final JLabel serverValue = new JLabel("En attente");
    private final JLabel status = new JLabel("Pr\u00eat");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private static final Color INK = new Color(28, 43, 66);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color MUTED = new Color(96, 111, 132);

    public WifiSpeedApp() {
        super("Wi-Fi et d\u00e9bit Internet - Java Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 720);
        setMinimumSize(new Dimension(820, 600));
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(new Color(242, 246, 251));
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        setContentPane(root);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Wi-Fi Studio");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(INK);
        JLabel badge = new JLabel(" JAVA 8 / SWING ");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(BLUE);
        heading.add(title, BorderLayout.WEST);
        heading.add(badge, BorderLayout.EAST);
        top.add(heading);
        JLabel subtitle = new JLabel("Vos r\u00e9seaux Wi-Fi et votre d\u00e9bit Internet, en un coup d'\u0153il.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        JPanel subtitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        subtitleRow.setOpaque(false);
        subtitleRow.add(subtitle);
        top.add(subtitleRow);
        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        metrics.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        metrics.add(metricCard("D\u00c9BIT DESCENDANT", speedValue, "Estimation de la connexion active"));
        metrics.add(metricCard("DONN\u00c9ES RE\u00c7UES", volumeValue, "25 Mo maximum par test"));
        metrics.add(metricCard("SERVEUR DE TEST", serverValue, "Bascule automatique si indisponible"));
        top.add(metrics);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        buttons.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        styleButton(scan, false);
        speed.setText("Tester le d\u00e9bit");
        styleButton(speed, true);
        buttons.add(scan);
        buttons.add(javax.swing.Box.createHorizontalStrut(12));
        buttons.add(speed);
        top.add(buttons);
        add(top, BorderLayout.NORTH);
        configureText(output);
        configureText(networks);
        networks.setText("R\u00e9seaux \u00e0 proximit\u00e9\n\n"
                + "Cliquez sur Scanner le Wi-Fi pour afficher les r\u00e9seaux disponibles.\n"
                + "Vous retrouverez ici le nom, le signal, le canal et la s\u00e9curit\u00e9.\n");
        output.setText("Pr\u00eat pour votre premier test\n\n"
                + "Connectez Windows au Wi-Fi \u00e0 tester, puis cliquez sur Tester le d\u00e9bit.\n\n"
                + "Le test suit la connexion active du PC : Ethernet ou un VPN peut influencer\n"
                + "le r\u00e9sultat. Il mesure le t\u00e9l\u00e9chargement Internet, sans upload ni ping.\n");
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabs.addTab("  R\u00e9seaux Wi-Fi  ", new JScrollPane(networks));
        tabs.addTab("  Test de d\u00e9bit  ", new JScrollPane(output));
        add(tabs, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setForeground(MUTED);
        progress.setPreferredSize(new Dimension(180, 10));
        progress.setForeground(BLUE);
        progress.setStringPainted(true);
        footer.add(status, BorderLayout.NORTH);
        footer.add(progress, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        scan.addActionListener(e -> scanWifi());
        speed.addActionListener(e -> testSpeed());
    }

    private static JPanel metricCard(String title, JLabel value, String hint) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(221, 229, 240)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(BLUE);
        JLabel note = new JLabel(hint);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(MUTED);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(note, BorderLayout.SOUTH);
        return card;
    }

    private static void styleButton(JButton button, boolean primary) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(primary ? BLUE : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? BLUE : new Color(207, 217, 231)),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));
    }

    private static void configureText(JTextArea text) {
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setForeground(INK);
        text.setBackground(Color.WHITE);
        text.setMargin(new java.awt.Insets(20, 20, 20, 20));
    }

    private void setBusy(boolean busy, String message) {
        scan.setEnabled(!busy);
        speed.setEnabled(!busy);
        progress.setIndeterminate(busy);
        progress.setValue(0);
        status.setText(message);
    }

    private void showFailure(Exception exception) {
        Throwable cause = exception;
        if (exception instanceof ExecutionException && exception.getCause() != null) {
            cause = exception.getCause();
        }
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        JTextArea target = tabs.getSelectedIndex() == 0 ? networks : output;
        target.append("\nErreur : " + cause.toString() + "\n");
        status.setText("Operation \u00e9chou\u00e9e : voir le message ci-dessus.");
    }

    private void scanWifi() {
        setBusy(true, "Recherche des r\u00e9seaux...");
        tabs.setSelectedIndex(0);
        tabs.setEnabledAt(1, false);
        networks.setText("Recherche des r\u00e9seaux en cours...\n");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                        .startsWith("windows")) {
                    throw new IOException("Ce scan utilis\u00e9 netsh et necessite Windows.");
                }
                return "=== CONNEXION WI-FI ACTUELLE ===\n"
                        + runNetsh("interfaces")
                        + "\n=== R\u00c9SEAUX VISIBLES ===\n"
                        + runNetsh("networks", "mode=bssid");
            }

            @Override
            protected void done() {
                setBusy(false, "Scan termin\u00e9");
                tabs.setEnabledAt(1, true);
                try {
                    networks.setText(get() + "\nSi Windows signale un acc\u00e8s refus\u00e9, v\u00e9rifiez "
                            + "les autorisations de localisation.\n"
                            + "Si aucun reseau appara\u00eet, v\u00e9rifiez la carte Wi-Fi et le mode avion.\n");
                    networks.setCaretPosition(0);
                } catch (Exception ex) {
                    showFailure(ex);
                }
            }
        }.execute();
    }

    private static String runNetsh(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 3];
        command[0] = "netsh.exe";
        command[1] = "wlan";
        command[2] = "show";
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        // Redirection vers un fichier : pas de blocage si la sortie est volumineuse.
        Path temp = Files.createTempFile("wifi-scan-", ".txt");
        Process process = null;
        try {
            // La page UTF-8 est limitee au processus enfant, sans modifier Windows.
            String invocation = "chcp 65001 >nul && " + String.join(" ", command);
            process = new ProcessBuilder("cmd.exe", "/d", "/c", invocation).redirectErrorStream(true)
                    .redirectOutput(temp.toFile()).start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                throw new IOException("Windows ne r\u00e9pond pas au scan (15 secondes).");
            }
            String result = decodeNetsh(Files.readAllBytes(temp));
            if (process.exitValue() != 0) {
                throw new IOException("netsh :\n" + result);
            }
            return result;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(temp);
        }
    }

    static String decodeNetsh(byte[] data) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException ex) {
            // Repli pour les anciennes versions de netsh qui gardent l'encodage OEM.
            text = new String(data, Charset.forName(System.getProperty("wifi.charset", "Cp850")));
        }
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private void testSpeed() {
        setBusy(true, "T\u00e9l\u00e9chargement de test en cours...");
        tabs.setSelectedIndex(1);
        tabs.setEnabledAt(0, false);
        speedValue.setText("En cours...");
        volumeValue.setText("\u2014 Mo");
        serverValue.setText("Connexion...");
        output.setText("Test du d\u00e9bit descendant de la connexion Internet active.\n"
                + "Serveurs : Cloudflare, puis Hetzner en cas de refus.\n"
                + "Donn\u00e9es : 25 Mo maximum.\n"
                + "Mesure indicative, avec une seule connexion HTTPS.\n\n");
        new SwingWorker<DownloadResult, String>() {
            @Override
            protected DownloadResult doInBackground() throws Exception {
                String[] servers = {
                    "https://speed.cloudflare.com/__down?bytes=" + MAX_BYTES
                            + "&nocache=" + System.currentTimeMillis(),
                    "https://fsn1-speed.hetzner.com/100MB.bin",
                    "https://hel1-speed.hetzner.com/100MB.bin"
                };
                HttpURLConnection connection = openDownloadConnection(servers,
                        message -> publish(message));
                long bytes = 0;
                long start = System.nanoTime();
                long lastUpdate = start;
                try {
                    long expected = connection.getContentLengthLong();
                    try (InputStream input = connection.getInputStream()) {
                        byte[] buffer = new byte[65536];
                        while (bytes < MAX_BYTES) {
                            int read = input.read(buffer, 0,
                                    (int) Math.min(buffer.length, MAX_BYTES - bytes));
                            if (read == -1) {
                                if (expected > 0 && bytes < Math.min(expected, MAX_BYTES)) {
                                    throw new IOException("T\u00e9l\u00e9chargement interrompu.");
                                }
                                break;
                            }
                            bytes += read;
                            long now = System.nanoTime();
                            if (now - lastUpdate >= 500000000L) {
                                publish(String.format(Locale.FRANCE,
                                        "%.1f Mo re\u00e7us...", bytes / 1000000.0));
                                lastUpdate = now;
                            }
                            // Echantillon limite dans le temps pour les connexions lentes.
                            if (now - start >= 15000000000L) {
                                break;
                            }
                        }
                    }
                    double seconds = (System.nanoTime() - start) / 1000000000.0;
                    // Evite de transformer une petite reponse d'erreur en resultat de d\u00e9bit.
                    if (bytes < 100000L) {
                        throw new IOException("Echantillon trop petit pour estimer le d\u00e9bit.");
                    }
                    double mbps = bytes * 8.0 / seconds / 1000000.0;
                    String details = String.format(Locale.FRANCE,
                            "Serveur utilis\u00e9 : %s\n"
                            + "D\u00e9bit descendant estim\u00e9 : %.2f Mbit/s\n"
                            + "\u00c9quivalent : %.2f Mo/s\n"
                            + "Volume re\u00e7u : %.2f Mo en %.2f secondes\n\n"
                            + "La dur\u00e9e mesure le transfert apr\u00e8s r\u00e9ception des en-tetes HTTP.\n"
                            + "Le serveur, le VPN et les autres usages du reseau influencent le resultat.\n"
                            + "Ce resultat ne mesure pas directement la vitesse radio du Wi-Fi.\n",
                            connection.getURL().getHost(), mbps, mbps / 8.0,
                            bytes / 1000000.0, seconds);
                    return new DownloadResult(details, mbps, bytes, connection.getURL().getHost());
                } finally {
                    connection.disconnect();
                }
            }

            @Override
            protected void process(List<String> messages) {
                for (String message : messages) {
                    output.append(message + "\n");
                }
                status.setText(messages.get(messages.size() - 1));
            }

            @Override
            protected void done() {
                setBusy(false, "Test termin\u00e9");
                tabs.setEnabledAt(0, true);
                try {
                    DownloadResult result = get();
                    output.append("\n" + result.details);
                    speedValue.setText(String.format(Locale.FRANCE, "%.2f Mbit/s", result.mbps));
                    volumeValue.setText(String.format(Locale.FRANCE, "%.2f Mo", result.bytes / 1000000.0));
                    serverValue.setFont(new Font("Segoe UI", Font.BOLD, 15));
                    serverValue.setText(result.server);
                    progress.setValue(100);
                } catch (Exception ex) {
                    speedValue.setText("Indisponible");
                    serverValue.setText("\u00c9chec du test");
                    showFailure(ex);
                }
            }
        }.execute();
    }

    private static final class DownloadResult {
        final String details;
        final double mbps;
        final long bytes;
        final String server;
        DownloadResult(String details, double mbps, long bytes, String server) {
            this.details = details;
            this.mbps = mbps;
            this.bytes = bytes;
            this.server = server;
        }
    }

    /** Essaie un autre serveur seulement avant de commencer le transfert. */
    static HttpURLConnection openDownloadConnection(String[] servers,
            Consumer<String> report) throws IOException {
        IOException failure = new IOException("Aucun serveur de test accessible.");
        for (String address : servers) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(address);
                report.accept("Connexion a " + url.getHost() + "...");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(false);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", "WifiSpeedApp/1.1 (Java 8)");
                connection.setRequestProperty("Accept", "application/octet-stream, */*");
                connection.setRequestProperty("Accept-Encoding", "identity");
                connection.setRequestProperty("Cache-Control", "no-cache");
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + code
                            + (code == 403 ? " : acc\u00e8s refus\u00e9 par le serveur" : ""));
                }
                String type = connection.getContentType();
                if (type != null && (type.toLowerCase(Locale.ROOT).contains("text/html")
                        || type.toLowerCase(Locale.ROOT).contains("application/xhtml"))) {
                    throw new IOException("Le serveur renvoie une page Web au lieu du fichier de test.");
                }
                report.accept("Serveur accepte : " + url.getHost());
                return connection;
            } catch (IOException ex) {
                if (connection != null) {
                    connection.disconnect();
                }
                failure.addSuppressed(ex);
                report.accept(address + " : " + ex.getMessage());
            }
        }
        throw new IOException("Aucun serveur accessible. Verifiez les messages ci-dessus "
                + "et l'acc\u00e8s Internet. Si une erreur HTTPS appara\u00eet, mettez le JDK 8 a jour.", failure);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // Le style Swing par defaut reste disponible.
            }
            new WifiSpeedApp().setVisible(true);
        });
    }
}
