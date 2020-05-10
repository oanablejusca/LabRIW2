import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public class Parser {
    public static void writeToFile(String content, PrintWriter printWriter)
    {
        printWriter.print(content);
    }

    public static String getDomainName(String url) throws URISyntaxException {
        if(url != null) {
            try {
                URI uri = new URI(url);
                String domain = uri.getHost();
                return domain.startsWith("www.") ? domain.substring(4) : domain;
            } catch (NullPointerException e) {
                System.out.println("NULL POINTER FOUND");
                System.out.println("URL " + url);
                return null;
            }
        } else {
            System.out.println("Null url found");
            return null;
        }
    }

    public static String getLocalPath(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String path = uri.getPath();
        return path;
    }

    public static void writeFileTextContent(PrintWriter p, String filePath, String absUrl) {
        File input = new File(filePath);
        try {
            Document doc = Jsoup.parse(input, null, absUrl);
            try {
                String text = doc.body().text();
                writeToFile("\n" + text, p);
            }catch(NullPointerException e) {
                writeToFile( "\nNO TEXT", p);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static  String trimUrl(String url) {
        if(url.indexOf('#') > 0) {
            return url.substring(0, url.indexOf('#'));
        } else
            return url;
    }
    public static List<String> getConnectedLinks(String filePath, String absUrl) {
        List<String> results = new LinkedList<>();
        File input = new File(filePath);
        try {
            Document doc = Jsoup.parse(input, null, absUrl);
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                //p.print(link.attr("abs:href"));
                results.add(trimUrl(link.attr("abs:href")));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("____________FILE__________" + filePath);
            e.printStackTrace();
        }
        return results;
    }

    public static String getRobotsContent(String filePath,  String absUrl) {
        String content="";
        File input = new File(filePath);
        try {
            Document doc = Jsoup.parse(input, null, absUrl);
            List<Element> robots = doc.select("meta[name=robots]");
            for (Element e:
                    robots) {
                content += e.attr("content").toLowerCase() + ",";

            }
            return content;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return content;
    }
}
