import java.io.*;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.stream.Collectors;


public class HTTPClient {

    private final static String USER_AGENT = "CLIENT RIW";
    private final static String WORKING_PATH = "./http";
    public static final String HTTP_VERSION = "HTTP/1.1";

    @SneakyThrows
    public static void createRequest(String path, String domain, int port) throws IOException {

        String request = genereateGetRequest(path, domain, USER_AGENT);
        BufferedReader bufferedReader = sendRequest(domain, port, request);
        String response = bufferedReader.lines().collect(Collectors.joining("\n"));
        boolean isSuccessful = handleFirstResponseLine(response);
        if (isSuccessful) {
            String headers = getResponseHeaders(response);
            writeHTMLBody(path, domain, response, headers);
        }
    }

    @SneakyThrows
    private static void writeHTMLBody(String path, String domain, String response, String headers) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(response));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (!headers.contains(line))
                sb.append(line).append("\n");
        }

        String filePath = WORKING_PATH + "/" + domain + path;
        if (!(filePath.endsWith(".html") || filePath.endsWith("htm")) && !path.equals("/robots.txt")) {
            if (!filePath.endsWith("/")) {
                filePath += "/";
            }
            filePath += "index.html";
        }

        File file = new File(filePath);
        File pDir = file.getParentFile();
        if (!pDir.exists()) {
            pDir.mkdirs(); // mkdir  -p
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(sb.toString());
        writer.close();
    }

    private static String getResponseHeaders(String response) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(response));
        String line = null;
        StringBuilder sb = new StringBuilder();

        while ((line = bufferedReader.readLine()) != null) {
            if (line.equals(""))
                break;
            sb.append(line).append("\n");
        }
        bufferedReader.close();
        return sb.toString();
    }

    private static String genereateGetRequest(String path, String domain, String userAgent) {
        StringBuilder sb = new StringBuilder();
        sb.append("GET ").append(path).append(" ").append(HTTP_VERSION).append("\r\n");
        sb.append("Host: ").append(domain).append("\r\n");
        sb.append("User-Agent: ").append(userAgent).append("\r\n");
        sb.append("Connection: close\r\n");
        sb.append("\r\n");
        return sb.toString();
    }

    @SneakyThrows
    private static BufferedReader sendRequest(String domain, int port, String request) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(domain, port));
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        dataOutputStream.writeBytes(request);
        return bufferedReader;
    }

    @SneakyThrows
    private static boolean handleFirstResponseLine(String response) {
        int statusCode = Integer.parseInt(response.substring(HTTP_VERSION.length(), HTTP_VERSION.length() + 4).trim());
        return statusCode >= 200 && statusCode < 400;
    }

}
