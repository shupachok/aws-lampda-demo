import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

//trigger event from EventBridge
public class DemoLambda implements RequestHandler<Map<String, String>, String> {

    private final String LINE_ACCESS_TOKEN = System.getenv("LINE_ACCESS_TOKEN");
    private final String LINE_USER_ID = System.getenv("LINE_USER_ID");
    private final String ALPHA_VANTAGE_API_KEY = System.getenv("ALPHA_VANTAGE_API_KEY");

    private final double TARGET_PE_RATIO = 30.0;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String handleRequest(Map<String, String> input, Context context) {
        var logger = context.getLogger();
        logger.log("Lambda function triggered for stock analysis...");

        String targetStock = input.getOrDefault("symbol", "META");

        try {
            JsonObject stockData = fetchStockOverview(targetStock,logger);

            if (stockData != null && stockData.has("PERatio")) {
                double currentPE = stockData.get("PERatio").getAsDouble();
                String name = stockData.get("Name").getAsString();

                logger.log("Company: " + name + " | Current P/E: " + currentPE);

                if (currentPE < TARGET_PE_RATIO) {
                    String message = String.format(
                            """
                            แจ้งเตือนหุ้นสหรัฐ MEG7 น่าสนใจ!
                            ชื่อหุ้น: %s (%s)
                            P/E ปัจจุบัน: %.2f
                            """,
                            name, targetStock, currentPE
                    );

                    sendLineNotify(message,logger);
                    logger.log("Alert sent to LINE successfully!");
                } else {
                    logger.log("Condition not met. No alert sent.");
                }
            } else {
                logger.log("API Rate limit reached or data not found.");
            }

        } catch (Exception e) {
            logger.log("Error occurred: " + e.getMessage());
        }

        return "Success";
    }

    private JsonObject fetchStockOverview(String symbol,LambdaLogger logger) throws Exception {
        String url = String.format("https://www.alphavantage.co/query?function=OVERVIEW&symbol=%s&apikey=%s",
                symbol, ALPHA_VANTAGE_API_KEY);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.log("Get stock detail successfully!");
        } else {
            logger.log("Failed to get stock detail. Status: " + response.statusCode());
            logger.log("Response: " + response.body());
        }

        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void sendLineNotify(String messageText, LambdaLogger logger) throws Exception {

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("type", "text");
        messageObj.addProperty("text", messageText);

        JsonArray messagesArray = new JsonArray();
        messagesArray.add(messageObj);

        JsonObject payloadObj = new JsonObject();
        payloadObj.addProperty("to", LINE_USER_ID);
        payloadObj.add("messages", messagesArray);

        String jsonPayload = payloadObj.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.line.me/v2/bot/message/push"))
                .header("Authorization", "Bearer " + LINE_ACCESS_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            logger.log("Push message sent successfully!");
        } else {
            logger.log("Failed to send message. Status: " + response.statusCode());
            logger.log("Response: " + response.body());
        }
    }
}