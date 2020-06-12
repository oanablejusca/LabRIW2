import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

public class URLFrontier {
    private static  final int wkNum = 2;
    public static HashMap<String, Queue<String>> urlPool = new HashMap<>();
    public static HashMap<String, Lock> lockers = new HashMap<>();
    private static String[] lastKeys = new String[wkNum];
    public static int count = 0;

    private static  String fetch(String key, int daemonID) {
        lockers.get(key).lock();
        lastKeys[daemonID] = key;
        String url = urlPool.get(key).remove();
        count--;
        if(urlPool.get(key).size() == 0)
            urlPool.remove(key);
        lockers.get(key).unlock();
        return url;
    }

    public static String feed(int daemonID)
    {
        if(urlPool.size() == 0) {
            count = 0;
            return null;
        }

        if (urlPool.size() == 1 || lastKeys[daemonID] == null)
        {
            String key = (String) urlPool.keySet().toArray()[0];
            if(urlPool.get(key).size() > 0) {
                return fetch(key, daemonID);
            } else {
                urlPool.remove(key);
                count = 0;
            }
        } else {
            String key = (String) urlPool.keySet().toArray()[(int) (Math.random() * 1000) % urlPool.keySet().size()];
            int trials = 0;
            while(key.equals(lastKeys[daemonID]) || urlPool.get(key).size() < 1){
                key = (String) urlPool.keySet().toArray()[(int) (Math.random() * 1000) % urlPool.keySet().size()];
                if(++trials > 10) {
                    return null;
                }
            }
            // System.out.println(daemonID  + "got from "  + key);
            return fetch(key, daemonID);
        }
        return null;
    }
}
