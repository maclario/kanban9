package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import model.Task;
import service.TaskManager;
import util.NumChecker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected String contentFromRequestBody;
    protected TaskManager taskManager;
    protected Gson gson;
    protected static final int OK = 200;
    protected static final int CREATED = 201;
    protected static final int BAD_REQUEST = 400;
    protected static final int NOT_FOUND = 404;
    protected static final int METHOD_NOT_ALLOWED = 405;
    protected static final int NOT_ACCEPTABLE = 406;
    protected static final int INTERNAL_SERVER_ERROR = 500;
    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    protected static final String DEFAULT_CONTENT_TYPE = "application/json;charset=utf-8";

    public BaseHttpHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

    protected String[] getPathParts(HttpExchange exchange) {
        return exchange.getRequestURI().getPath().split("/");
    }

    protected String defineEndpoint(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] pathParts = getPathParts(exchange);
        String endpoint = "UNKNOWN";
        switch (method) {
            case "GET" -> endpoint = defineGetVariation(pathParts);
            case "POST" -> endpoint = definePostVariation(exchange);
            case "DELETE" -> endpoint = defineDeleteVariation(pathParts, exchange);
            default -> sendMethodNotAllowed(exchange);
        }
        return endpoint;
    }

    protected String defineGetVariation(String[] pathParts) {
        if (pathParts.length == 3 && NumChecker.isNum(pathParts[2])) {
            return "GET_BY_ID";
        } else if (pathParts.length == 4 && NumChecker.isNum(pathParts[2]) && pathParts[3].equals("subtasks")) {
            return "GET_SUBS_BY_ID";
        } else if (pathParts.length == 2) {
            return "GET";
        } else {
            return "UNKNOWN";
        }
    }

    protected String definePostVariation(HttpExchange exchange) throws IOException {
        contentFromRequestBody = parseContentFromRequestBody(exchange);
        Task receivedTask = gson.fromJson(contentFromRequestBody, Task.class);
        if (receivedTask.getId() != null) {
            return "POST_BY_ID";
        } else {
            return "POST";
        }
    }

    protected String defineDeleteVariation(String[] pathParts, HttpExchange exchange) {
        if (pathParts.length == 3 && NumChecker.isNum(pathParts[2])) {
            return "DELETE_BY_ID";
        } else {
            sendBadRequest(exchange);
            return "UNKNOWN";
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, byte[] response) {
        try (OutputStream responseBody = exchange.getResponseBody()) {
            if (response != null) {
                exchange.sendResponseHeaders(statusCode, response.length);
                responseBody.write(response);
            } else {
                exchange.sendResponseHeaders(statusCode, 0);
            }
        } catch (IOException e) {
            System.out.println("FAIL: Ошибка при создании и отправке ответа.");
        }
    }

    protected void sendText(HttpExchange httpExchange, String text) {
        byte[] response = text.getBytes(DEFAULT_CHARSET);
        httpExchange.getResponseHeaders().add("Content-Type", DEFAULT_CONTENT_TYPE);
        sendResponse(httpExchange, OK, response);
    }

    protected void sendOk(HttpExchange httpExchange) {
        sendResponse(httpExchange, OK, null);
    }

    protected void sendCreated(HttpExchange httpExchange) {
        sendResponse(httpExchange, CREATED, null);
    }

    protected void sendNotFound(HttpExchange httpExchange) {
        sendResponse(httpExchange, NOT_FOUND, null);
    }

    protected void sendHasInteractions(HttpExchange httpExchange) {
        sendResponse(httpExchange, NOT_ACCEPTABLE, null);
    }

    protected void sendMethodNotAllowed(HttpExchange httpExchange) {
        sendResponse(httpExchange, METHOD_NOT_ALLOWED, null);
    }

    protected void sendBadRequest(HttpExchange httpExchange) {
        sendResponse(httpExchange, BAD_REQUEST, null);
    }

    protected void sendInternalServerError(HttpExchange httpExchange) {
        sendResponse(httpExchange, INTERNAL_SERVER_ERROR, null);
    }

    protected String parseContentFromRequestBody(HttpExchange httpExchange) throws IOException {
        try (InputStream requestBody = httpExchange.getRequestBody()) {
            return new String(requestBody.readAllBytes(), DEFAULT_CHARSET);
        } catch (IOException e) {
            sendInternalServerError(httpExchange);
            throw e;
        }
    }

}
