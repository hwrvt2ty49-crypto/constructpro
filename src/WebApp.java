import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebApp {
    private static final ArrayList<Project> projects = new ArrayList<>();
    private static final Path PROJECTS_FILE = Path.of("projects.txt");

    public static void main(String[] args) throws IOException {
        loadProjects();

        int port = getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebApp::handleHomePage);
        server.createContext("/add-project", WebApp::handleAddProject);
        server.setExecutor(null);
        server.start();

        System.out.println("Construction app is running.");
        System.out.println("Open this in your browser: http://localhost:" + port);
    }

    private static int getPort() {
        String portText = System.getenv().getOrDefault("PORT", "8080");
        return Integer.parseInt(portText);
    }

    private static void handleHomePage(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        String html = buildHomePage();
        sendResponse(exchange, html, 200);
    }

    private static void handleAddProject(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        String formBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> formData = parseFormData(formBody);

        String name = formData.getOrDefault("name", "");
        String client = formData.getOrDefault("client", "");
        String address = formData.getOrDefault("address", "");
        String status = formData.getOrDefault("status", "");

        if (!name.isBlank()) {
            projects.add(new Project(name, client, address, status));
            saveProjects();
        }

        exchange.getResponseHeaders().add("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static String buildHomePage() {
        StringBuilder projectCards = new StringBuilder();

        for (Project project : projects) {
            projectCards.append("""
                    <article class="project-card">
                        <h3>%s</h3>
                        <p><strong>Client:</strong> %s</p>
                        <p><strong>Address:</strong> %s</p>
                        <p><strong>Status:</strong> %s</p>
                    </article>
                    """.formatted(
                    escapeHtml(project.getName()),
                    escapeHtml(project.getClient()),
                    escapeHtml(project.getAddress()),
                    escapeHtml(project.getStatus())
            ));
        }

        if (projects.isEmpty()) {
            projectCards.append("<p>No projects yet.</p>");
        }

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Construction Project Tracker</title>
                    <style>
                        body {
                            margin: 0;
                            font-family: Arial, sans-serif;
                            background: #f4f6f8;
                            color: #17202a;
                        }

                        .page {
                            max-width: 960px;
                            margin: 0 auto;
                            padding: 32px 20px;
                        }

                        header {
                            margin-bottom: 24px;
                        }

                        h1 {
                            margin: 0 0 8px;
                            font-size: 32px;
                        }

                        .layout {
                            display: grid;
                            grid-template-columns: 320px 1fr;
                            gap: 24px;
                            align-items: start;
                        }

                        form, .project-card {
                            background: white;
                            border: 1px solid #d9e1e8;
                            border-radius: 8px;
                            padding: 18px;
                        }

                        label {
                            display: block;
                            margin-top: 12px;
                            font-weight: bold;
                        }

                        input {
                            width: 100%;
                            box-sizing: border-box;
                            margin-top: 6px;
                            padding: 10px;
                            border: 1px solid #b8c4ce;
                            border-radius: 6px;
                            font-size: 15px;
                        }

                        button {
                            width: 100%;
                            margin-top: 16px;
                            padding: 11px;
                            border: 0;
                            border-radius: 6px;
                            background: #2364aa;
                            color: white;
                            font-size: 15px;
                            font-weight: bold;
                            cursor: pointer;
                        }

                        .project-list {
                            display: grid;
                            gap: 14px;
                        }

                        .project-card h3 {
                            margin: 0 0 10px;
                        }

                        .project-card p {
                            margin: 6px 0;
                        }

                        @media (max-width: 760px) {
                            .page {
                                padding: 18px 14px;
                            }

                            h1 {
                                font-size: 26px;
                            }
                            .layout {
                                grid-template-columns: 1fr;
                                gap:18px;
                            }
                            form, .project-card {
                                padding: 14px;
                            }
                            input, button {
                                font-size: 16px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <main class="page">
                        <header>
                            <h1>Construction Project Tracker</h1>
                            <p>Add jobs, track clients, addresses, and project status.</p>
                        </header>

                        <section class="layout">
                            <form method="POST" action="/add-project">
                                <h2>Add Project</h2>

                                <label for="name">Project Name</label>
                                <input id="name" name="name" required>

                                <label for="client">Client Name</label>
                                <input id="client" name="client">

                                <label for="address">Job Address</label>
                                <input id="address" name="address">

                                <label for="status">Status</label>
                                <input id="status" name="status" placeholder="In Progress">

                                <button type="submit">Add Project</button>
                            </form>

                            <div>
                                <h2>Projects</h2>
                                <div class="project-list">
                                    {{PROJECT_CARDS}}
                                </div>
                            </div>
                        </section>
                    </main>
                </body>
                </html>
                """.replace("{{PROJECT_CARDS}}", projectCards.toString());
    }

    private static Map<String, String> parseFormData(String formBody) {
        Map<String, String> formData = new HashMap<>();

        if (formBody.isBlank()) {
            return formData;
        }

        String[] pairs = formBody.split("&");

        for (String pair : pairs) {
            String[] keyAndValue = pair.split("=", 2);
            String key = decodeUrlText(keyAndValue[0]);
            String value = keyAndValue.length > 1 ? decodeUrlText(keyAndValue[1]) : "";
            formData.put(key, value);
        }

        return formData;
    }

    private static String decodeUrlText(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void loadProjects() throws IOException {
        if (!Files.exists(PROJECTS_FILE)) {
            addSampleProjects();
            saveProjects();
            return;
        }

        List<String> lines = Files.readAllLines(PROJECTS_FILE, StandardCharsets.UTF_8);

        for (String line : lines) {
            List<String> parts = splitProjectLine(line);

            if (parts.size() == 4) {
                projects.add(new Project(
                        decodeProjectText(parts.get(0)),
                        decodeProjectText(parts.get(1)),
                        decodeProjectText(parts.get(2)),
                        decodeProjectText(parts.get(3))
                ));
            }
        }
    }

    private static void saveProjects() throws IOException {
        ArrayList<String> lines = new ArrayList<>();

        for (Project project : projects) {
            String line = encodeProjectText(project.getName()) + "|"
                    + encodeProjectText(project.getClient()) + "|"
                    + encodeProjectText(project.getAddress()) + "|"
                    + encodeProjectText(project.getStatus());
            lines.add(line);
        }

        Files.write(
                PROJECTS_FILE,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static List<String> splitProjectLine(String line) {
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        boolean previousWasSlash = false;

        for (int i = 0; i < line.length(); i++) {
            char letter = line.charAt(i);

            if (letter == '|' && !previousWasSlash) {
                parts.add(currentPart.toString());
                currentPart.setLength(0);
            } else {
                currentPart.append(letter);
            }

            previousWasSlash = letter == '\\' && !previousWasSlash;

            if (letter != '\\') {
                previousWasSlash = false;
            }
        }

        parts.add(currentPart.toString());
        return parts;
    }

    private static String encodeProjectText(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", " ");
    }

    private static String decodeProjectText(String text) {
        StringBuilder result = new StringBuilder();
        boolean previousWasSlash = false;

        for (int i = 0; i < text.length(); i++) {
            char letter = text.charAt(i);

            if (previousWasSlash) {
                result.append(letter);
                previousWasSlash = false;
            } else if (letter == '\\') {
                previousWasSlash = true;
            } else {
                result.append(letter);
            }
        }

        if (previousWasSlash) {
            result.append('\\');
        }

        return result.toString();
    }

    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void addSampleProjects() {
        projects.add(new Project(
                "Kitchen Remodel",
                "Maria Lopez",
                "24 Maple Street",
                "In Progress"
        ));

        projects.add(new Project(
                "Bathroom Addition",
                "James Carter",
                "108 Pine Avenue",
                "Waiting on Inspection"
        ));
    }
}
