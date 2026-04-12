package demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class VulnController {

    private static final String DB_PASSWORD = "A9v!q2#LmP8$Xn7^Rt4@Ks1";
    private static final String GITHUB_TOKEN = "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";
    private static final String URL_WITH_SECRET = "https://demoUser:[email protected]:5432/appdb";

    private static final byte[] WEAK_AES_KEY = "1234567890abcdef".getBytes(StandardCharsets.UTF_8);
    private static final Random INSECURE_RANDOM = new Random();
    private static final Map<String, String> STORE = new HashMap<>();

    @GetMapping("/sql")
    public String sql(@RequestParam String name) throws Exception {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/appdb";
        Connection conn = DriverManager.getConnection(jdbcUrl, "root", DB_PASSWORD);
        Statement stmt = conn.createStatement();
        stmt.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
        return "ok";
    }

    @GetMapping("/cmd")
    public String cmd(@RequestParam String arg) throws Exception {
        Runtime.getRuntime().exec("sh -c \"ls " + arg + "\"");
        return "ok";
    }

    @GetMapping("/xss")
    public String xss(@RequestParam String q) {
        return "<html><body><h1>" + q + "</h1></body></html>";
    }

    @GetMapping("/redirect")
    public void redirect(HttpServletResponse response, @RequestParam String next) throws Exception {
        response.sendRedirect(next);
    }

    @GetMapping("/header")
    public void header(HttpServletResponse response, @RequestParam String value) {
        response.addHeader("X-Debug", value);
    }

    @GetMapping("/read-file")
    public String readFile(@RequestParam String file) throws Exception {
        Path p = Path.of("/tmp/app/data/" + file);
        return Files.readString(p);
    }

    @PostMapping("/store")
    public String store(@RequestParam String key, @RequestParam String value) {
        STORE.put(key, value);
        return "saved";
    }

    @GetMapping("/stored-xss")
    public String storedXss(@RequestParam String key) {
        String value = STORE.get(key);
        return "<html><body><div>" + value + "</div></body></html>";
    }

    @GetMapping("/stored-file")
    public String storedFile(@RequestParam String key) throws Exception {
        String stored = STORE.get(key);
        Path p = Path.of("/var/www/uploads/" + stored);
        return Files.readString(p);
    }

    @GetMapping("/ssrf")
    public String ssrf(@RequestParam String u) throws Exception {
        try (var in = new URL(u).openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/xxe")
    public String xxe(@RequestParam String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        return doc.getDocumentElement().getTextContent();
    }

    @PostMapping("/xpath")
    public String xpath(@RequestParam String xml, @RequestParam String user) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expr = "/users/user[name/text()='" + user + "']/secret/text()";
        Object result = xpath.evaluate(expr, doc, XPathConstants.STRING);
        return "<html><body>" + result + "</body></html>";
    }

    @PostMapping("/deserialize-bytes")
    public String deserializeBytes(@RequestParam String data) throws Exception {
        byte[] bytes = java.util.Base64.getDecoder().decode(data);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object obj = ois.readObject();
        return String.valueOf(obj);
    }

    @GetMapping("/deserialize-file")
    public String deserializeFile(@RequestParam String file) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        Object obj = ois.readObject();
        return String.valueOf(obj);
    }

    @GetMapping("/crypto")
    public byte[] crypto(@RequestParam String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec key = new SecretKeySpec(WEAK_AES_KEY, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/rand")
    public int rand() {
        return INSECURE_RANDOM.nextInt(1000000);
    }

    @GetMapping("/secure-rand")
    public int secureRand() {
        return new SecureRandom().nextInt(1000000);
    }

    @GetMapping("/trace")
    public String trace() {
        try {
            throw new IllegalStateException("boom");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
    }

    @PostMapping("/save-next")
    public String saveNext(@RequestParam String next) {
        STORE.put("next", next);
        return "saved";
    }

    @GetMapping("/go-next")
    public void goNext(HttpServletResponse response) throws Exception {
        response.sendRedirect(STORE.get("next"));
    }

    @PostMapping("/save-header")
    public String saveHeader(@RequestParam String v) {
        STORE.put("hdr", v);
        return "saved";
    }

    @GetMapping("/use-header")
    public void useHeader(HttpServletResponse response) {
        response.addHeader("X-Stored", STORE.get("hdr"));
    }

    @GetMapping("/profile")
    public String profile(@RequestParam String displayName, @RequestParam String bio) {
        return "<html><body>"
                + "<h2>" + displayName + "</h2>"
                + "<p>" + bio + "</p>"
                + "</body></html>";
    }

    @GetMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) throws Exception {
        String jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/demo";
        Connection conn = DriverManager.getConnection(jdbcUrl, "postgres", DB_PASSWORD);
        Statement stmt = conn.createStatement();
        stmt.executeQuery(
                "SELECT * FROM accounts WHERE username = '" + username +
                "' AND password = '" + password + "'"
        );
        return "ok";
    }

    @GetMapping("/download")
    public byte[] download(@RequestParam String name) throws Exception {
        Path p = Path.of("/srv/downloads/" + name);
        return Files.readAllBytes(p);
    }

    @GetMapping("/ping")
    public String ping(@RequestParam String host) throws Exception {
        Process p = Runtime.getRuntime().exec("sh -c \"ping -c 1 " + host + "\"");
        return "started:" + p.pid();
    }
}
