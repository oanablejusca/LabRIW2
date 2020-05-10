import java.io.IOException;

public class Aplication {
    public static void main(String[] args) {

        //LAB05
        String domain = "riweb.tibeica.com";

        try {
            HTTPClient.createRequest("/", domain, 80);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
