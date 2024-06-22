package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.TaskManager;

public class HistoryHandler extends BaseHttpHandler implements HttpHandler {

    public HistoryHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) {
        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equals("GET")) {
            try {
                String history = gson.toJson(taskManager.getHistory());
                sendText(exchange, history);
            } catch (Exception e) {
                sendInternalServerError(exchange);
            }
        } else {
            sendBadRequest(exchange);
        }
    }

}
