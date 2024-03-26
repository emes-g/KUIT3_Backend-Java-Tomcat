package webserver;

import db.MemoryUserRepository;
import http.util.HttpRequestUtils;
import http.util.IOUtils;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable {
    Socket connection;
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());

    public RequestHandler(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            // request message start-line 검증
            String startLine = br.readLine();
            String[] startLines = startLine.split(" ");
            String method = startLines[0];
            String url = startLines[1];
            if (url.equals("/") || url.equals("/index.html")) {
                // index.html 반환
                byte[] body = Files.readAllBytes(new File("webapp/index.html").toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
                return;
            }
            if (url.equals("/user/form.html")) {
                // user/form.html 반환
                byte[] body = Files.readAllBytes(new File("webapp/user/form.html").toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
                return;
            }
            if (url.startsWith("/user/signup") || method.equals("GET")) {
                String queryString = url.substring("/user/signup?".length());
                Map<String, String> elements = HttpRequestUtils.parseQueryParameter(queryString);
                MemoryUserRepository userRepository = MemoryUserRepository.getInstance();
                userRepository.addUser(new User(elements.get("userId"), elements.get("password"), elements.get("name"), elements.get("email")));
                // for redirect
                response302Header(dos, "/index.html");
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String path) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes(String.format("Location: %s\r\n", path));
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }
}