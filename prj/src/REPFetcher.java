import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

public class REPFetcher {

    private static Semaphore repSem = new Semaphore(1);

    public static void fetchRobotsTxtRules(String strHost, String userAgentSignature, String domainName) {
        String strRobot = "http://" + domainName + "/robots.txt";
        //System.out.println("Fetching " + strRobot);
        URL urlRobot;
        try {
            urlRobot = new URL(strRobot);
        } catch (MalformedURLException e) {
            return;
        }


        String strCommands;
        try {
            // InputStream urlRobotStream = urlRobot.openStream();
            URLConnection con = urlRobot.openConnection();
            con.setConnectTimeout(1000);
            con.setReadTimeout(1000);
            InputStream urlRobotStream = con.getInputStream();

            int totalRead = 0;
            // read in entire file
            byte[] c = new byte[10000];
            int numRead = urlRobotStream.read(c);
            totalRead += numRead;
            if(numRead != -1)
                strCommands = new String(c, 0, numRead);
            else
                strCommands = "";
            while (numRead != -1) {
                numRead = urlRobotStream.read(c);
                totalRead += numRead;
                if (numRead != -1) {
                    String newCommands = new String(c, 0, numRead);
                    strCommands += newCommands;
                }
            }

            // bad to the bone
            if(totalRead > 1000){
                Master.repDisallowed.put(domainName, new HashSet<>());
                return;
            }

            StringBuilder sb = new StringBuilder();
            int i = 0;
            String lastAgentName= "";
            boolean writtenDissalow = false;

            List<String> genericDisalows = new ArrayList<>();
            boolean foundAgent = false;
            char [] b = strCommands.toCharArray();
            while(i < totalRead){
                String ruleName = "";
                String ruleValue = "";
                while( i < strCommands.length() && b[i] != '\n' && b[i] != '\r') {
                    if(b[i] == '#'){
                        while(i<strCommands.length() && b[i]!='\n'){
                            i++;
                        }
                        i++; //consume new line
                        continue;
                    }
                    if (b[i] != ':')
                        sb.append(b[i]);
                    else
                    {
                        ruleName = sb.toString();
                        sb.setLength(0);
                    }
                    i++;
                }
                ruleValue = sb.toString();
                sb.setLength(0);
                if(ruleName.equals("User-agent")) {
                    lastAgentName = ruleValue;
                }

                if(lastAgentName.trim().equals("*") || lastAgentName.trim().equals(userAgentSignature)) {
                    if(ruleName.trim().equals("Disallow")) {
                        if(ruleValue.endsWith("*"))
                        {
                            ruleValue = ruleValue.substring(0, ruleValue.indexOf('*'));
                        }
                        if(ruleValue.trim().equals("")) {
                            if(lastAgentName.trim().equals(userAgentSignature)){
                                Master.repDisallowed.put(domainName, new HashSet<>());
                                return;
                            } else {
                                continue;
                            }
                        }
                        if(ruleValue.trim().equals("/") && !Master.blackListedDomains.contains(domainName)) {
                            Master.blackListedDomains.add(domainName);
                            writtenDissalow = true;
                            return;
                        }
                        if(Master.repDisallowed.containsKey(domainName)) {
                            if(lastAgentName.trim().equals(userAgentSignature)) {
                                Master.repDisallowed.get(domainName).add(ruleValue.trim());
                                writtenDissalow = true;
                                foundAgent = true;
                            }
                            else
                            {
                                genericDisalows.add(ruleValue.trim());
                                writtenDissalow = true;
                            }
                        } else {
                            if(lastAgentName.trim().equals(userAgentSignature)) {
                                HashSet<String> hs = new HashSet<>();
                                hs.add(ruleValue.trim());
                                Master.repDisallowed.put(domainName, hs);
                                foundAgent = true;
                                writtenDissalow = true;
                            } else {
                                genericDisalows.add(ruleValue.trim());
                                writtenDissalow = true;
                            }
                        }
                    }
                    if(ruleName.trim().equals("Crawl-Delay")){
                        int delay = Integer.parseInt(ruleValue.trim());
                        if(Master.repDelays.containsKey(domainName)){
                            Master.repDelays.replace(domainName, delay);
                        }else {
                            Master.repDelays.put(domainName, delay);
                        }

                    }
                }
                i++;
            }

            if(writtenDissalow == false) {
                Master.repDisallowed.put(domainName, new HashSet<>()); // set a marker for parsed robots.txt
            }

            if (foundAgent == false) {
                for (String s: genericDisalows) {
                    if(Master.repDisallowed.containsKey(domainName)){
                        Master.repDisallowed.get(domainName).add(s);
                    } else {
                        HashSet<String> hs = new HashSet<>();
                        hs.add(s);
                        Master.repDisallowed.put(domainName, hs);
                    }
                }
            }
            urlRobotStream.close();
        } catch (IOException e) {
            //return true;
            Master.repDisallowed.put(domainName, new HashSet<>());
        }
        catch (IllegalArgumentException e) {
            Master.repDisallowed.put(domainName, new HashSet<>());
            ;//System.out.println("EXCEPTION " + urlRobot);
        }
        // System.out.println("Done:" + strRobot);
    }

    public static boolean isSafe(String url, String signature){
        try {
            if(!url.startsWith("http")) {
                return false;
            }

            String domain = Parser.getDomainName(url);
            if(Master.blackListedDomains.contains(domain) || Master.repDisallowed.containsKey(domain)) {
                if (Master.blackListedDomains.contains(domain)) {
                    return false;
                }
                String path = Parser.getLocalPath(url);
                if (Master.repDisallowed.containsKey(domain)) {
                    HashSet<String> disallowed = Master.repDisallowed.get(domain);
                    for (String s : disallowed) {
                        if (path.startsWith(s)) {
                            return false;
                        }
                    }
                }
            } else {
                try {
                    repSem.acquire();
                    String IP = Daemon.getIp(domain);
                    if(!Master.blackListedDomains.contains(domain) && !Master.repDisallowed.containsKey(domain)){
                        fetchRobotsTxtRules(IP, signature, domain);
                        return isSafe(url, signature);
                    }
                    //repSem.release();
                }catch (InterruptedException e){
                    ;
                } finally {
                    repSem.release();
                }
            }
        }catch(URISyntaxException e) {
            return false;
        }
        return true;
    }


    public static void main(String[] args){
        REPFetcher.fetchRobotsTxtRules("95.216.24.32", "MyBot", "httpd.apache.org");
    }
}
