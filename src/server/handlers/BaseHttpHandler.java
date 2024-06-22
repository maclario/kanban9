package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import model.EpicTask;
import model.Subtask;
import model.Task;
import service.TaskManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {
    protected TaskManager taskManager;
    protected Gson gson;
    protected Integer id;
    protected Task receivedTask;
    protected Subtask receivedSub;
    protected EpicTask receivedEpic;
    protected static final int OK = 200;
    protected static final int CREATED = 201;
    protected static final int BAD_REQUEST = 400;
    protected static final int NOT_FOUND = 404;
    protected static final int METHOD_NOT_ALLOWED = 405;
    protected static final int NOT_ACCEPTABLE = 406;
    protected static final int INTERNAL_SERVER_ERROR = 500;
    protected enum JTBD {GET, GET_BY_ID, GET_SUBS_BY_ID, POST, POST_BY_ID, DELETE_BY_ID, UNKNOWN};
    protected JTBD jtbd;

    public BaseHttpHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

    protected JTBD defineJTBD(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] pathParts = exchange.getRequestURI().getPath().split("/");
        JTBD jtbd = JTBD.UNKNOWN;
        switch (method) {
            case "GET" -> jtbd = defineGetVariation(pathParts);
            case "POST" -> jtbd = definePostVariation(pathParts, exchange);
            case "DELETE" -> jtbd = defineDeleteVariation(pathParts, exchange);
            default -> sendMethodNotAllowed(exchange);
        }
        return jtbd;
    }

    protected JTBD defineGetVariation(String[] pathParts) {
        if (pathParts.length == 3) {
            try {
                id = Integer.parseInt(pathParts[2]);
                return JTBD.GET_BY_ID;
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Полученный идентификатор не является числом.");
            }
        } else if (pathParts.length == 4 && pathParts[1].equals("epics") && pathParts[3].equals("subtasks")) {
            try {
                id = Integer.parseInt(pathParts[2]);
                return JTBD.GET_SUBS_BY_ID;
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Полученный идентификатор не является числом.");
            }
        } else {
            return JTBD.GET;
        }
    }

    protected JTBD definePostVariation(String[] pathParts, HttpExchange exchange) throws IOException {
        switch (pathParts[1]) {
            case "tasks":
                receivedTask = gson.fromJson(getTaskFromRequestBody(exchange), Task.class);
                id = receivedTask.getId();
                break;
            case "subtasks":
                receivedSub = gson.fromJson(getTaskFromRequestBody(exchange), Subtask.class);
                id = receivedSub.getId();
                break;
            case "epics":
                receivedEpic = gson.fromJson(getTaskFromRequestBody(exchange), EpicTask.class);
                id = receivedEpic.getId();
                break;
            default:
                sendBadRequest(exchange);
        }

        if (id != null) {
            return JTBD.POST_BY_ID;
        } else {
            return JTBD.POST;
        }
    }

    protected JTBD defineDeleteVariation(String[] pathParts, HttpExchange exchange) {
        if (pathParts.length == 3) {
            try {
                id = Integer.parseInt(pathParts[2]);
                return JTBD.DELETE_BY_ID;
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Полученный идентификатор не является числом.");
            }
        } else {
            sendBadRequest(exchange);
        }
        return JTBD.UNKNOWN;
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
        byte[] response = text.getBytes(StandardCharsets.UTF_8);
        httpExchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
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

    protected String getTaskFromRequestBody(HttpExchange httpExchange) throws IOException {
        try (InputStream requestBody = httpExchange.getRequestBody()) {
            return new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            sendInternalServerError(httpExchange);
            throw e;
        }
    }

}
