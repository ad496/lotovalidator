import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class Downloader {

    static {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
           System.out.println("Downloader: "+e);
        }
    }


    public static void run(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                BufferedReader reader = null;
                HttpsURLConnection conn = null;
                try {//detail
                    String uu = String.format("https://%s/pos_loto_data","138.197.161.95");
                    URL u = new URL(uu);
                    conn = (HttpsURLConnection) u.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.setReadTimeout(120000 /*milliseconds*/);
                    conn.setConnectTimeout(120000 /* milliseconds */);
                    conn.setRequestMethod("GET");
                    conn.connect();
                    int status = conn.getResponseCode();

                    if (status == 200) {
                        is = conn.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }

                    } else {
                        throw new RuntimeException(uu + " ответ - " + String.valueOf(status));
                    }
                } catch (Exception ex) {
                   System.out.println("http error:"+ex.toString());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    Main.atomicBoolean.set(true);
                }
            }
        }).run();
    }
}
