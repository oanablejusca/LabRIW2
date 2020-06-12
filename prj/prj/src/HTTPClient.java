import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class HTTPClient {

    private String userAgent;
    private String resourceFolder;

    public HTTPClient(String userAgent, String destinationFolder)
    {
        this.userAgent = userAgent;
        this.resourceFolder = destinationFolder;
    }


    public String getCompleteResource(String localPath, String domainName, String host, Date lastAccessed, int port) throws IOException
    {
        StringBuilder requestBuilder = new StringBuilder();

        // request line
        requestBuilder.append("GET " + localPath + " HTTP/1.1\r\n");
        // Host
        requestBuilder.append("Host: " + domainName + "\r\n");
        requestBuilder.append("User-Agent: " + this.userAgent + "\r\n");
        requestBuilder.append("Connection: close\r\n");

        if (lastAccessed !=null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "EEE, dd MMM yyyy HH:mm:ss z");
            requestBuilder.append("If-Modified-Since: " + dateFormat.format(lastAccessed) + "\r\n");
        }
        // final cerere(separatorul de elemenet de entitate)
        requestBuilder.append("\r\n");
        String requestString = requestBuilder.toString();

        //deschidere conexiune impicita
        try {
            //Socket tcpSocket = new Socket(host, port);
            Socket tcpSocket = new Socket();
            // dechidere conexiune
            tcpSocket.connect(new InetSocketAddress(host, port)) ;// , 2000);
            DataOutputStream outServer = new DataOutputStream(tcpSocket.getOutputStream());
            BufferedReader inServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            //atasam fluxul de date de timp scriere, trimitem stringul de cerere anterior construit cu ajutorul writer-ului creat
            outServer.writeBytes(requestString);

            String responseLine;
            String filePath = null;
            responseLine = inServer.readLine();
            //  get response code
            int responseCode =0;
            try{
                responseCode = Integer.parseInt(responseLine.substring(9, 12));
            } catch(NullPointerException e) {
                return null;
            }

            HashMap<String, String> headers = new HashMap<>();
            String regLine = "";
            StringBuilder hsb = new StringBuilder();
            int pos = 0;
            //citim linie cu line raspunsul pe care il primim din partea server-ului
            while ((regLine = inServer.readLine()) != null) {
                hsb.setLength(0);
                pos = 0;
                String headerName = null;
                String headerValue = null;
                if (regLine.equals("")) //end of response headers
                {
                    break;
                }
                int idx = 0;
                for (char c : regLine.toCharArray()) {
                    idx++;
                    if (idx < regLine.length() && (c != ':' || regLine.toCharArray()[idx] != ' ')) {
                        hsb.append(c);
                    } else {
                        if (pos == 0) {
                            headerName = hsb.toString().trim();
                            hsb.setLength(0);

                            pos = 1;
                        } else {
                            hsb.append(regLine.toCharArray()[regLine.length() - 1]);
                            headerValue = hsb.toString().trim();
                            hsb.setLength(0);
                            headers.put(headerName, headerValue);
                        }
                    }

                }
                //System.out.println(regLine);
            }

            // set the last modified date for heuristics usage for cache validation
            if (headers.containsKey("Last-Modified")) {
                String url = "http://" + domainName + localPath;

                String date = headers.get("Last-Modified");
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                try {
                    Date d = format.parse(date);
                    if (Master.lastModifiedCacheInfo.containsKey(url)) {
                        Master.lastModifiedCacheInfo.replace(url, d);
                    } else {
                        Master.lastModifiedCacheInfo.put(url, d);
                    }
                } catch (ParseException e) {
                    System.out.println("Bad Val for date " + date);
                }

            }
            // verificare coduri de stare
            switch (responseCode) {
                case 200:
                    // parse response content
                    StringBuilder pageBuilder = new StringBuilder();
                    while ((regLine = inServer.readLine()) != null) {
                        pageBuilder.append(regLine + System.lineSeparator());
                    }

                    // construim calea de salvare a resursei
                    filePath = resourceFolder + "/" + domainName + localPath;
                    if (!(filePath.endsWith(".html") || filePath.endsWith("htm")) && !localPath.equals("/robots.txt")) {
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
                    writer.write(pageBuilder.toString());
                    writer.close();
                    String robotsContent = Parser.getRobotsContent(filePath, "http://" + domainName + "/");
                    String exit = "";
                    //all- permisiuni complete pe pagina
                    //none- nicio permisiune pe pagina
                    //index- imi permite sa indexez pagina(extragerea de text)
                    //noindex- nu am voie sa extrag continutul si sa-l trec printr-o forma de procesare
                    //nofollow- nu am voie  sa extrag acele informatii de tip anchor si sa le folosesc mai departe
                    //follow- am voie sa extrag link-uri pentru prelucrarile ulterioare
                    if (robotsContent.contains("none") || robotsContent.contains("nofollow")) {
                        exit = null;
                    }
                    if (robotsContent.contains("none") || robotsContent.contains("noindex")) {
                        // delete the local file
                        File f = new File(filePath);
                        f.delete();
                        filePath = null;
                    }
                    return exit == null ? exit : filePath;
                case 301:
                    String newUrl = headers.get("Location");
                    if (newUrl != null) {
                        try {
                            if (!domainName.equals(Parser.getDomainName(newUrl))) {
                                Master.cleanPool(domainName, Parser.getDomainName(newUrl));
                            } else {
                                Master.urlsPool.add(newUrl); // send it back in order to be revisited
                            }
                        } catch (URISyntaxException e) {
                            System.out.println("Bad Location received");
                        }
                    }
                    //Master.cleanPool("http://" + domainName + "\\" + localPath, headers.get("Location"));
                    break;
                case 302:
                    //System.out.println("old http://" + domainName + localPath);
                    String newLocalUrl = headers.get("Location");
                    //System.out.println("new " + newLocalUrl);
                    if (newLocalUrl != null) {
                        //System.out.println("H: " + headers);
                        try {
                            String newDomainName = Parser.getDomainName(newLocalUrl);
                            String newLocalPath = Parser.getLocalPath(newLocalUrl);
                            if (newDomainName.equals(domainName) && newLocalPath.equals(localPath))
                                return null;
                            String newHost = Daemon.getIp(newDomainName);
                            getCompleteResource(newLocalPath, newDomainName, newHost, null, 80);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 304:
                    break; //not modified...do nothing
                default:
                    break;

            }

            tcpSocket.close();
            return filePath;
        }catch (ConnectException e) {
            System.out.println("Connection refused for: " + domainName + "-> " + localPath);
            return null;
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    public static void main(String args[])
    {
        HTTPClient httpClient = new HTTPClient("CLIENT RIW", "./http");
        String link = "www.quora.com";
        try
        {
            httpClient.getCompleteResource("/unanswered/Does-Selenium-cover-100-testing-for-a-web-application", link,  Daemon.getIp(link), new Date(), 80);
        }
        catch (IOException ioe)
        {
            System.out.println("Eroare socket:");
            ioe.printStackTrace();
        }
    }
}