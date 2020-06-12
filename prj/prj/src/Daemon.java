import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Daemon extends Thread {
    private int id;
    public static  Object obj = new Object();
    private HTTPClient httpClient = new HTTPClient("RIWEB_CRAWLER", "./http");

    public static Queue<String> urls = new LinkedList<>();

    public static Lock urlLock = new ReentrantLock();

    public Daemon(int daemonCount) {
        id = daemonCount;
    }

    public static String getIp(String domainName) {
        String ip = "";
        if(Master.dnsCache.containsKey(domainName) && Master.dnsCache.get(domainName)!= null)
            ip = Master.dnsCache.get(domainName);
        else {
            String responseLine = "";
            synchronized (obj) {
                responseLine = dnsQuery(domainName);
            }
            if (responseLine != null && !responseLine.equals("Error")) {
                StringBuilder sb = new StringBuilder();
                for (char c : responseLine.toCharArray()) {
                    if (c != '-') {
                        sb.append(c);
                    } else {
                        ip = sb.toString();
                        sb.setLength(0);
                    }
                }
                int ttl = Integer.parseInt(sb.toString());
                if (Master.dnsResponses.containsKey(domainName))
                    Master.dnsResponses.replace(domainName, new DNSResponse(domainName, ip, ttl));
                else
                    Master.dnsResponses.put(domainName, new DNSResponse(domainName, ip, ttl));
            }
            if (ip.equals("")) {
                //search another DNS
                try {
                    ip = InetAddress.getByName(domainName).getHostAddress();

                    int ttl = 600;
                    if (Master.dnsResponses.containsKey(domainName))
                        Master.dnsResponses.replace(domainName, new DNSResponse(domainName, ip, ttl));
                    else
                        Master.dnsResponses.put(domainName, new DNSResponse(domainName, ip, ttl));

                } catch (UnknownHostException e) {
                    System.out.println("Unknown Domain " + domainName);
                }
            }
            Master.dnsCache.put(domainName, ip);
        }
        return ip;
    }

    public void run() {
        for (;;) {
            String url = null;
            if(urls.size() > 0) {
                urlLock.lock();
                if (urls.size() > 0)
                    url = urls.remove();
                urlLock.unlock();
                if (url == null)
                    continue;
                try {
                    String domainName = Parser.getDomainName(url);
                    String ip = getIp(domainName);
                    if(ip == null) {
                        continue;
                    }
                    try
                    {
                        Date lastAccessed = null;
                        String filePath = httpClient.getCompleteResource(Parser.getLocalPath(url), domainName, ip, lastAccessed, 80);

                        if (filePath != null) {
                            List<String> connectedLinks = Parser.getConnectedLinks(filePath, url);
                            Master.resourceCount++;
                            if(Master.resourceCount % 40 == 0) {
                                Date end = new Date();
                                long milis = end.getTime() - Master.startTime.getTime();
                                System.out.println("Thread " + id +" Time: " + (int) milis/1000);
                            }
                            System.out.println(Master.resourceCount + " " + urls.size());
                            //adaugare url-uri in coada
                            Master.addToPool(connectedLinks);
                        }
                    }
                    catch (IOException ioe)
                    {
                        System.out.println("Eroare socket:");
                        ioe.printStackTrace();
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    this.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String dnsQuery(String domain) {
        String hostname = "localhost";
        int port = 1234;
        Socket clientSocket = null;
        DataOutputStream os = null;
        BufferedReader is = null;
        synchronized (obj) {
            try {
                clientSocket = new Socket(hostname, port);
                clientSocket.setTcpNoDelay(true);
                os = new DataOutputStream(clientSocket.getOutputStream());
                is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                os.writeBytes(Integer.toString(domain.length()) + domain + "\r\n");
                String resLine = is.readLine();
                clientSocket.close();
                return resLine;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (ConnectException e) {
                // System.out.println("Connection refused by DNS service");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
