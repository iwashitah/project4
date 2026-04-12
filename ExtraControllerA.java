package demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@RestController
public class ExtraControllerA {

    private static final Map<String, String> STORE = new HashMap<>();

    @GetMapping("/dbcs")
    public String dbConnectionString(@RequestParam String host,
                                     @RequestParam String db,
                                     @RequestParam String user,
                                     @RequestParam String pass) throws Exception {
        String cs = "jdbc:mysql://" + host + ":3306/" + db + "?user=" + user + "&password=" + pass;
        Connection c = DriverManager.getConnection(cs);
        return String.valueOf(c != null);
    }

    @PostMapping("/save-table")
    public String saveTable(@RequestParam String table) {
        STORE.put("table", table);
        return "saved";
    }

    @GetMapping("/2nd-order-sql")
    public String secondOrderSql(@RequestParam String id) throws Exception {
        String table = STORE.get("table");
        Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/app", "root", "root");
        Statement s = c.createStatement();
        s.executeQuery("SELECT * FROM " + table + " WHERE id = '" + id + "'");
        return "ok";
    }

    @GetMapping("/ldap")
    public Object ldap(@RequestParam String user) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://127.0.0.1:389");
        DirContext ctx = new InitialDirContext(env);
        String filter = "(uid=" + user + ")";
        return ctx.search("ou=people,dc=example,dc=com", filter, null);
    }

    @PostMapping("/save-ldap")
    public String saveLdap(@RequestParam String v) {
        STORE.put("ldapFilter", v);
        return "saved";
    }

    @GetMapping("/stored-ldap")
    public Object storedLdap() throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://127.0.0.1:389");
        DirContext ctx = new InitialDirContext(env);
        return ctx.search("ou=people,dc=example,dc=com", STORE.get("ldapFilter"), null);
    }

    @GetMapping("/jndi")
    public Object jndi(@RequestParam String name) throws Exception {
        InitialContext ctx = new InitialContext();
        return ctx.lookup(name);
    }

    @GetMapping("/cookie")
    public String cookie(HttpServletResponse response,
                         @RequestParam String role) {
        Cookie c = new Cookie("role", role);
        response.addCookie(c);
        return "ok";
    }

    @PostMapping("/save-cookie")
    public String saveCookie(@RequestParam String v) {
        STORE.put("cookieRole", v);
        return "saved";
    }

    @GetMapping("/stored-cookie")
    public String storedCookie(HttpServletResponse response) {
        Cookie c = new Cookie("role", STORE.get("cookieRole"));
        response.addCookie(c);
        return "ok";
    }

    @GetMapping("/password-cookie")
    public String passwordCookie(HttpServletResponse response,
                                 @RequestParam String password) {
        Cookie c = new Cookie("password", password);
        response.addCookie(c);
        return "ok";
    }

    @GetMapping("/header-inject")
    public String headerInject(HttpServletResponse response,
                               @RequestParam String value) {
        response.setHeader("X-User", value);
        return "ok";
    }

    @PostMapping("/save-header-a")
    public String saveHeader(@RequestParam String value) {
        STORE.put("hdr", value);
        return "saved";
    }

    @GetMapping("/stored-header")
    public String storedHeader(HttpServletResponse response) {
        response.setHeader("X-User", STORE.get("hdr"));
        return "ok";
    }

    @PostMapping("/save-next2")
    public String saveNext2(@RequestParam String value) {
        STORE.put("next2", value);
        return "saved";
    }

    @GetMapping("/stored-redirect")
    public void storedRedirect(HttpServletResponse response) throws Exception {
        response.sendRedirect(STORE.get("next2"));
    }

    @GetMapping("/reflect")
    public Object reflection(@RequestParam String className,
                             @RequestParam String method) throws Exception {
        Class<?> c = Class.forName(className);
        Object o = c.getDeclaredConstructor().newInstance();
        Method m = c.getMethod(method);
        return m.invoke(o);
    }

    @PostMapping("/save-reflect")
    public String saveReflect(@RequestParam String className,
                              @RequestParam String method) {
        STORE.put("reflect.class", className);
        STORE.put("reflect.method", method);
        return "saved";
    }

    @GetMapping("/stored-reflect")
    public Object storedReflection() throws Exception {
        Class<?> c = Class.forName(STORE.get("reflect.class"));
        Object o = c.getDeclaredConstructor().newInstance();
        Method m = c.getMethod(STORE.get("reflect.method"));
        return m.invoke(o);
    }
}
