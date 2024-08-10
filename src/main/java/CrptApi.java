import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

@Data
public class CrptApi {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Semaphore semaphore;
    private final long interval;
    private final TimeUnit timeUnit;
    private final Gson gson = new GsonBuilder().create();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.interval = timeUnit.toMillis(1);
        this.semaphore = new Semaphore(requestLimit);

        scheduler.scheduleAtFixedRate(semaphore::drainPermits, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            String jsonInputString = gson.toJson(document);
            sendPostRequest(jsonInputString, signature);
        } finally {
            semaphore.release();
        }
    }

    private void sendPostRequest(String jsonInputString, String signature) throws IOException {
        URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        connection.getOutputStream().write(jsonInputString.getBytes());
        connection.getOutputStream().flush();
        connection.getOutputStream().close();

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        connection.disconnect();
    }

    @Data
    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;

    }

    @Data
    public static class Description {
        private String participantInn;

    }

    @Data
    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 1);

        CrptApi.Document document = new CrptApi.Document();

        try {
            crptApi.createDocument(document, "signature");
            System.out.println("Документ создан!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}