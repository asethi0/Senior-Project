package com.betcha.SocketChat;

import io.javalin.Javalin;

import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static j2html.TagCreator.*;

public class Chat {
    // Display names stored here
    private static Map<Session, String> userUsernameMap = new ConcurrentHashMap<>();

    // Accounts Stored Here
    private static Map<String, String> accountMap = new HashMap<String, String>() {{
        put("aleks", "aleks");
        put("ashish@ash.com", "ashish");
        put("ben", "ben");
        put("clay", "clay");
        put("max@maxray.me", "max");
        put("sean", "sean");
    }};

    // Used to generate username
    private static int nextUserNumber = 1;

    public static void main(String[] args) {
        // Initialize Server
        Javalin app = Javalin.create()
                .port(7070) // Port

                .enableStaticFiles("/public") // Static files location
                .ws("/chat", ws -> {     // Route for websockets
                    ws.onConnect(session -> { // Define behavior for connection
                        String username = "User" + nextUserNumber++; // Generate username
                        userUsernameMap.put(session, username); // Add username to username store
                        broadcastMessage("Server", (username + " joined the chat")); // Tell everyone there's a new person on the server
                    });
                    ws.onClose((session, status, message) -> { // Define behavior for end connection
                        String username = userUsernameMap.get(session); // Get username
                        userUsernameMap.remove(session); // Remove username from active members
                        broadcastMessage("Server", (username + " left the chat")); // Tell everyone who left the server
                    });
                    ws.onMessage((session, message) -> { // Whenever someone sends a message, tell everyone
                        broadcastMessage(userUsernameMap.get(session), message);
                    });
                })
                .start();

        // NOT WORKING: Authentication
        app.post("/login", ctx -> {
            Map<String, String[]> params = ctx.formParamMap();
            if(accountMap.containsKey(params.get("user")[0]) &&
                    accountMap.get(params.get("user")[0]).equals(params.get("pass")[0])) {
                ctx.cookieStore("loggedIn", "true");
                ctx.cookieStore("user", params.get("user"));
                ctx.redirect("index.html");
            } else {
                ctx.redirect("incorrect.html");
            }
        });

        // NOT WORKING : Cookies
        app.get("/", ctx -> {
            ctx.cookieStore("loggedIn", "false");
            ctx.redirect("index.html");
        });
    }

    // Function to tell everyone about any updates to the socket
    private static void broadcastMessage(String sender, String message) {
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(
                        new JSONObject()
                                .put("userMessage", createHtmlMessageFromSender(sender, message))
                                .put("userList", userUsernameMap.values()).toString()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Generate HTML string from socket data
    private static String createHtmlMessageFromSender(String sender, String message) {
        return article(
                b(sender + " says:"),
                span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
                p(message)
        ).render();
    }
}
