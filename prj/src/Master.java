
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Master extends Thread {
    private final int workersNumber = 2;
    public List<Daemon> workers;
    public static HashMap<String, HistoryEntry> catalog = new HashMap<String, HistoryEntry>();
    public static Queue<String> urlsPool = new LinkedList<String>();
    public static HashMap<String, String> dnsCache = new HashMap<>();
    public static HashMap<String, DNSResponse> dnsResponses = new HashMap<>();
    public static HashMap<String, Date> lastModifiedCacheInfo = new HashMap<>();
    public static HashSet<String> blackListedDomains= new HashSet<>();

    public static HashMap<String, HashSet<String>> repDisallowed = new HashMap<>();
    public static HashMap<String, Integer> repDelays = new HashMap<>();
    public static Lock feedLock = new ReentrantLock();

    public static int  resourceCount = 0;
    public static Date startTime = null;


    public Master() {
        // history = new HashSet<>();int
        urlsPool = new LinkedList<>();
        workers = new LinkedList<>();
        for (int i = 0; i < workersNumber; i++){
            workers.add(new Daemon(i));
        }
        for(int i = 0; i < workersNumber; i++) {
            workers.get(i).start();
        }
    }

    public void run() {
        startTime = new Date();
        for(;;) {
            if(URLFrontier.count > 0) {
                for (int i = 0; i < 10; i++) {
                    int worker_idx = (int) (Math.random() * 1000) % workersNumber;
                    String url = URLFrontier.feed(worker_idx);
                    if (url != null) {
                        workers.get(worker_idx).urlLock.lock();
                        workers.get(worker_idx).urls.add(url);
                        workers.get(worker_idx).urlLock.unlock();
                    }
                }
            }

            if(urlsPool.size() > 0) {
                feedLock.lock();
                int written = 0;
                try {
                    for (; ; ) {
                        String url = urlsPool.remove();

                        if (url != null) {
                            try {
                                String domain = Parser.getDomainName(url);
                                if (!URLFrontier.lockers.containsKey(domain))
                                    URLFrontier.lockers.put(domain, new ReentrantLock());
                                URLFrontier.lockers.get(domain).lock();
                                if (URLFrontier.urlPool.containsKey(domain)) {
                                    URLFrontier.urlPool.get(domain).add(url);
                                } else {
                                    Queue<String> domainUrl = new LinkedList<>();
                                    domainUrl.add(url);
                                    URLFrontier.urlPool.put(domain, domainUrl);
                                }
                                URLFrontier.count++;
                                written++;
                                URLFrontier.lockers.get(domain).unlock();
                            } catch (URISyntaxException e) {
                                System.out.println("Wrong url" + url);
                            }
                        }
                        if (written >= 20) {
                            break;
                        }
                    }

                }catch (NoSuchElementException e) {
                    ;
                } finally {
                    feedLock.unlock();
                }

            } else {
                try {
                    this.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public static void addToPool(List<String> urlList) {
        feedLock.lock();
        for(String url: urlList) {
            if(url != null && !catalog.containsKey(url) && REPFetcher.isSafe(url, "RIWEB_CRAWLER") && !url.equals("")) {
                urlsPool.add(url);
            }

            if (!Master.catalog.containsKey(url)) {
                Master.catalog.put(url, new HistoryEntry(url, new Date(), Master.lastModifiedCacheInfo.get(url)));
            } else {
                Master.catalog.replace(url, new HistoryEntry(url, new Date(), Master.lastModifiedCacheInfo.get(url)));
            }

        }
        feedLock.unlock();

    }

    public static void cleanPool(String wrongDomain, String newDomain) {
        if(wrongDomain != null && newDomain != null && wrongDomain != "" && newDomain != "") {
            feedLock.lock();
            for (String url:
                    urlsPool) {
                try {
                    if (Objects.equals(Parser.getDomainName(url), wrongDomain)) {
                        urlsPool.remove(url);
                        String newUrl = "http://" + newDomain + Parser.getLocalPath(url);
                        urlsPool.add(newUrl);
                    }
                }catch(URISyntaxException e){
                    ;
                }
            }
            // urlsPool.removeIf((String url) -> Parser.getDomainName(url).equals(wrongDomain));
            // urlsPool.add(newUrl);
            feedLock.unlock();
        }
    }

    public static void main(String[] args) {
        Master m = new Master();
        List<String> seedList = new LinkedList<>();
        seedList.add("http://riweb.tibeica.com/crawl/");
//        seedList.add("http://dmoz-odp.org/Business/");
//        seedList.add("http://dmoz-odp.org/Regional/Europe/United_Kingdom/Society_and_Culture/People/");
//        seedList.add("http://ac.tuiasi.ro/");
//        seedList.add("http://florinleon.byethost24.com/");
//        seedList.add("http://blog.chapagain.com.np/recommender-system-using-java-apache-mahout/");


        addToPool(seedList);
        m.start();
    }
}
