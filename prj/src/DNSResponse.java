
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DNSResponse {
    public String domain;
    public String IP;
    public int TTL;
    private Timer timer;

    public DNSResponse(String domain, String IP, int TTL){
        this.domain = domain;
        this.IP = IP;
        this.TTL = TTL;
        timer = new Timer(TTL  * 1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.out.println("DNS REFRESH");
                String repliedWith = Daemon.dnsQuery(domain);
                StringBuilder sb = new StringBuilder();
                String IP = "";
                if (repliedWith != null && !repliedWith.equals("Error")) {
                    for (char c : repliedWith.toCharArray()) {
                        if (c != '-') {
                            sb.append(c);
                        } else {
                            IP = sb.toString();
                            Master.dnsCache.replace(domain, IP);
                            sb.setLength(0);
                        }
                    }
                    int TTL = Integer.parseInt(sb.toString());
                    Master.dnsResponses.replace(domain, new DNSResponse(domain, IP, TTL));
                } else {
                    try {
                        IP = InetAddress.getByName(domain).getHostAddress();
                        int TTL = 600;
                        Master.dnsResponses.replace(domain, new DNSResponse(domain, IP, TTL));
                    } catch (UnknownHostException e) {
                        ;
                    }
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
}
