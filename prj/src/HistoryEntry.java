import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

public class HistoryEntry {

    private HTTPClient httpClient = new HTTPClient("CLIENT RIW", "./http");
    public String url;
    public Date date;
    private Timer refresher;

    public long validityTime(Date lastModifiedDate){
        long cacheTime = 0;
        if(lastModifiedDate != null) {
            long difference = date.getTime() - lastModifiedDate.getTime();
            cacheTime = (long) (0.7 * difference + 0.3 * 1e6 / difference);
        } else {
            cacheTime = (long)1e6;
        }
        return 10 * cacheTime;
    }

    public HistoryEntry(String url, Date d, Date lastModified){
        this.url = url;
        this.date = d;
        refresher = new Timer((int) validityTime(lastModified), new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    String domain = Parser.getDomainName(url);
                    String IP = Daemon.getIp(domain);
                    try {
                        Date lastAccessed = Master.catalog.containsKey(url) ? Master.catalog.get(url).date : null;
                        httpClient.getCompleteResource(Parser.getLocalPath(url), domain, IP, lastAccessed, 80);
                        Master.catalog.get(url).date = new Date();
                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                }catch(URISyntaxException e) {
                    System.out.println("Bad URL  " + url);
                }
            }
        });
        refresher.setRepeats(false);
        refresher.start();
    }
}
