import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebApp {
    private static final ArrayList<Project> projects = new ArrayList<>();
    private static final Path PROJECTS_FILE = Path.of("projects.txt");
    private static final Path UPLOADS_DIR = Path.of("uploads");
    private static final String TASK_SEPARATOR = "~~TASK~~";
    private static final String PHOTO_SEPARATOR = "~~PHOTO~~";
    private static final String TASK_DONE_SEPARATOR = "::";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(UPLOADS_DIR);
        loadProjects();

        int port = getPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", WebApp::handleHomePage);
        server.createContext("/add-project", WebApp::handleAddProject);
        server.createContext("/add-task", WebApp::handleAddTask);
        server.createContext("/update-task", WebApp::handleUpdateTask);
        server.createContext("/toggle-task", WebApp::handleToggleTask);
        server.createContext("/delete-task", WebApp::handleDeleteTask);
        server.createContext("/upload-photo", WebApp::handleUploadPhoto);
        server.createContext("/uploads", WebApp::handleUploadedFile);
        server.createContext("/delete-project", WebApp::handleDeleteProject);
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

        sendResponse(exchange, buildHomePage(), 200);
    }

    private static void handleAddProject(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        Map<String, String> formData = readUrlEncodedForm(exchange);
        String name = formData.getOrDefault("name", "");
        String client = formData.getOrDefault("client", "");
        String address = formData.getOrDefault("address", "");
        String status = formData.getOrDefault("status", "");

        if (!name.isBlank()) {
            projects.add(new Project(name, client, address, status));
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static void handleAddTask(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        Map<String, String> formData = readUrlEncodedForm(exchange);
        Project project = findProject(formData.getOrDefault("index", ""));
        String taskText = formData.getOrDefault("task", "").trim();

        if (project != null && !taskText.isBlank()) {
            project.addTask(taskText);
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static void handleUpdateTask(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        Map<String, String> formData = readUrlEncodedForm(exchange);
        Project project = findProject(formData.getOrDefault("index", ""));
        int taskIndex = parseIndex(formData.getOrDefault("taskIndex", ""));
        String taskText = formData.getOrDefault("task", "").trim();

        if (project != null && taskIndex >= 0 && taskIndex < project.getTasks().size() && !taskText.isBlank()) {
            project.updateTask(taskIndex, taskText);
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static void handleToggleTask(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        Map<String, String> formData = readUrlEncodedForm(exchange);
        Project project = findProject(formData.getOrDefault("index", ""));
        int taskIndex = parseIndex(formData.getOrDefault("taskIndex", ""));

        if (project != null && taskIndex >= 0 && taskIndex < project.getTasks().size()) {
            project.toggleTask(taskIndex);
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static void handleDeleteTask(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        Map<String, String> formData = readUrlEncodedForm(exchange);
        Project project = findProject(formData.getOrDefault("index", ""));
        int taskIndex = parseIndex(formData.getOrDefault("taskIndex", ""));

        if (project != null && taskIndex >= 0 && taskIndex < project.getTasks().size()) {
            project.deleteTask(taskIndex);
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static void handleUploadPhoto(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String boundary = getMultipartBoundary(contentType);

        if (boundary == null) {
            sendResponse(exchange, "Missing upload boundary", 400);
            return;
        }

        MultipartForm multipartForm = parseMultipartForm(exchange.getRequestBody().readAllBytes(), boundary);
        Project project = findProject(multipartForm.fields.getOrDefault("index", ""));
        UploadedFile photo = multipartForm.files.get("photo");

        if (project != null && photo != null && photo.bytes.length > 0) {
            String fileName = "project-" + projects.indexOf(project) + "-" + System.currentTimeMillis()
                    + getImageExtension(photo.fileName);
            Path photoPath = UPLOADS_DIR.resolve(fileName);
            Files.write(photoPath, photo.bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            project.addPhoto(fileName);
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static void handleUploadedFile(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        String fileName = requestPath.substring("/uploads/".length());
        Path filePath = UPLOADS_DIR.resolve(fileName).normalize();

        if (!filePath.startsWith(UPLOADS_DIR) || !Files.exists(filePath)) {
            sendResponse(exchange, "Not found", 404);
            return;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().add("Content-Type", getImageContentType(fileName));
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void handleDeleteProject(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, "Method not allowed", 405);
            return;
        }

        Map<String, String> formData = readUrlEncodedForm(exchange);
        int projectIndex = parseIndex(formData.getOrDefault("index", ""));

        if (projectIndex >= 0 && projectIndex < projects.size()) {
            projects.remove(projectIndex);
            saveProjects();
        }

        redirectHome(exchange);
    }

    private static String buildHomePage() {
        StringBuilder projectCards = new StringBuilder();

        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);

            projectCards.append("""
                    <article class="project-card">
                        <div class="project-header">
                            <div>
                                <h3>%s</h3>
                                <p><strong>Client:</strong> %s</p>
                                <p><strong>Address:</strong> %s</p>
                                <p><strong>Status:</strong> %s</p>
                            </div>
                            <form class="delete-form" method="POST" action="/delete-project">
                                <input type="hidden" name="index" value="%d">
                                <button class="delete-button" type="submit">Delete</button>
                            </form>
                        </div>

                        <section class="project-section">
                            <h4>Tasks</h4>
                            %s
                            <form class="add-row-form" method="POST" action="/add-task">
                                <input type="hidden" name="index" value="%d">
                                <label for="task-%d">New Task</label>
                                <input id="task-%d" name="task" placeholder="Example: Schedule rough inspection">
                                <button type="submit">Add Task</button>
                            </form>
                        </section>

                        <section class="project-section">
                            <h4>Photos</h4>
                            %s
                            <form class="add-row-form" method="POST" action="/upload-photo" enctype="multipart/form-data">
                                <input type="hidden" name="index" value="%d">
                                <label for="photo-%d">Upload Photo</label>
                                <input id="photo-%d" name="photo" type="file" accept="image/*">
                                <button type="submit">Upload Photo</button>
                            </form>
                        </section>
                    </article>
                    """.formatted(
                    escapeHtml(project.getName()),
                    escapeHtml(project.getClient()),
                    escapeHtml(project.getAddress()),
                    escapeHtml(project.getStatus()),
                    i,
                    buildTasksHtml(project, i),
                    i,
                    i,
                    i,
                    buildPhotosHtml(project),
                    i,
                    i,
                    i
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
                            max-width: 1040px;
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

                        .add-project-form, .project-card {
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

                        .project-header {
                            display: flex;
                            justify-content: space-between;
                            gap: 16px;
                        }

                        .project-card h3 {
                            margin: 0 0 10px;
                        }

                        .project-card p {
                            margin: 6px 0;
                        }

                        .project-section {
                            margin-top: 16px;
                            padding-top: 14px;
                            border-top: 1px solid #e6edf3;
                        }

                        .project-section h4 {
                            margin: 0 0 10px;
                            font-size: 16px;
                        }

                        .empty-message {
                            margin: 0 0 12px;
                            color: #5f6f7a;
                        }

                        .task-list {
                            display: grid;
                            gap: 10px;
                            margin-bottom: 12px;
                        }

                        .task-row {
                            display: grid;
                            grid-template-columns: auto 1fr auto;
                            gap: 8px;
                            align-items: end;
                            padding: 10px;
                            border: 1px solid #e6edf3;
                            border-radius: 8px;
                            background: #fbfcfd;
                        }

                        .task-row form {
                            margin: 0;
                        }

                        .task-edit-form {
                            display: grid;
                            grid-template-columns: 1fr auto;
                            gap: 8px;
                            align-items: end;
                        }

                        .task-row input {
                            margin-top: 0;
                        }

                        .task-done input {
                            text-decoration: line-through;
                            color: #637381;
                        }

                        .small-button {
                            width: auto;
                            margin-top: 0;
                            padding: 9px 11px;
                            white-space: nowrap;
                        }

                        .done-button {
                            background: #217a43;
                        }

                        .not-done-button {
                            background: #6b7280;
                        }

                        .delete-button {
                            width: auto;
                            margin-top: 0;
                            padding: 8px 12px;
                            background: #b42318;
                        }

                        .delete-form {
                            margin: 0;
                        }

                        .add-row-form {
                            margin-top: 12px;
                        }

                        .photo-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
                            gap: 10px;
                            margin-bottom: 12px;
                        }

                        .photo-grid img {
                            width: 100%;
                            aspect-ratio: 4 / 3;
                            object-fit: cover;
                            border-radius: 8px;
                            border: 1px solid #d9e1e8;
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
                                gap: 18px;
                            }

                            .add-project-form, .project-card {
                                padding: 14px;
                            }

                            input, button {
                                font-size: 16px;
                            }

                            .project-header {
                                display: grid;
                            }

                            .task-row {
                                grid-template-columns: 1fr;
                            }

                            .task-edit-form {
                                grid-template-columns: 1fr;
                            }

                            .small-button, .delete-button {
                                width: 100%;
                            }
                        }
                    </style>
                </head>
                <body>
                    <main class="page">
                        <header>
                            <h1>Construction Project Tracker</h1>
                            <p>Add jobs, track clients, addresses, status, tasks, and photos.</p>
                        </header>

                        <section class="layout">
                            <form class="add-project-form" method="POST" action="/add-project">
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

    private static String buildTasksHtml(Project project, int projectIndex) {
        if (project.getTasks().isEmpty()) {
            return "<p class=\"empty-message\">No tasks yet.</p>";
        }

        StringBuilder html = new StringBuilder("<div class=\"task-list\">");

        for (int taskIndex = 0; taskIndex < project.getTasks().size(); taskIndex++) {
            ProjectTask task = project.getTasks().get(taskIndex);
            String rowClass = task.isDone() ? "task-row task-done" : "task-row";
            String doneButtonClass = task.isDone() ? "small-button not-done-button" : "small-button done-button";
            String doneButtonText = task.isDone() ? "Undo" : "Done";

            html.append("""
                    <div class="%s">
                        <form method="POST" action="/toggle-task">
                            <input type="hidden" name="index" value="%d">
                            <input type="hidden" name="taskIndex" value="%d">
                            <button class="%s" type="submit">%s</button>
                        </form>
                        <form class="task-edit-form" method="POST" action="/update-task">
                            <input type="hidden" name="index" value="%d">
                            <input type="hidden" name="taskIndex" value="%d">
                            <input name="task" value="%s">
                            <button class="small-button" type="submit">Save</button>
                        </form>
                        <form method="POST" action="/delete-task">
                            <input type="hidden" name="index" value="%d">
                            <input type="hidden" name="taskIndex" value="%d">
                            <button class="delete-button" type="submit">Delete</button>
                        </form>
                    </div>
                    """.formatted(
                    rowClass,
                    projectIndex,
                    taskIndex,
                    doneButtonClass,
                    doneButtonText,
                    projectIndex,
                    taskIndex,
                    escapeHtml(task.getText()),
                    projectIndex,
                    taskIndex
            ));
        }

        html.append("</div>");
        return html.toString();
    }

    private static String buildPhotosHtml(Project project) {
        if (project.getPhotos().isEmpty()) {
            return "<p class=\"empty-message\">No photos yet.</p>";
        }

        StringBuilder html = new StringBuilder("<div class=\"photo-grid\">");

        for (String photo : project.getPhotos()) {
            html.append("<img src=\"/uploads/")
                    .append(escapeHtml(photo))
                    .append("\" alt=\"Project photo\">");
        }

        html.append("</div>");
        return html.toString();
    }

    private static Map<String, String> readUrlEncodedForm(HttpExchange exchange) throws IOException {
        String formBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseFormData(formBody);
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

    private static Project findProject(String indexText) {
        int projectIndex = parseIndex(indexText);

        if (projectIndex >= 0 && projectIndex < projects.size()) {
            return projects.get(projectIndex);
        }

        return null;
    }

    private static int parseIndex(String indexText) {
        try {
            return Integer.parseInt(indexText);
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    private static void redirectHome(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
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

            if (parts.size() >= 4) {
                List<ProjectTask> tasks = new ArrayList<>();
                List<String> photos = new ArrayList<>();

                if (parts.size() >= 5) {
                    tasks = decodeProjectTasks(parts.get(4));
                }

                if (parts.size() >= 6) {
                    photos = decodeProjectPhotos(parts.get(5));
                }

                projects.add(new Project(
                        decodeProjectText(parts.get(0)),
                        decodeProjectText(parts.get(1)),
                        decodeProjectText(parts.get(2)),
                        decodeProjectText(parts.get(3)),
                        tasks,
                        photos
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
                    + encodeProjectText(project.getStatus()) + "|"
                    + encodeProjectTasks(project.getTasks()) + "|"
                    + encodeProjectPhotos(project.getPhotos());
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
                .replace("\n", " ")
                .replace(TASK_SEPARATOR, " ")
                .replace(PHOTO_SEPARATOR, " ");
    }

    private static String encodeProjectTasks(List<ProjectTask> tasks) {
        ArrayList<String> encodedTasks = new ArrayList<>();

        for (ProjectTask task : tasks) {
            encodedTasks.add(encodeProjectText(task.getText()) + TASK_DONE_SEPARATOR + task.isDone());
        }

        return String.join(TASK_SEPARATOR, encodedTasks);
    }

    private static List<ProjectTask> decodeProjectTasks(String text) {
        ArrayList<ProjectTask> tasks = new ArrayList<>();

        if (text.isBlank()) {
            return tasks;
        }

        String[] savedTasks = text.split(TASK_SEPARATOR, -1);

        for (String savedTask : savedTasks) {
            if (savedTask.isBlank()) {
                continue;
            }

            String[] taskParts = savedTask.split(TASK_DONE_SEPARATOR, 2);
            String taskText = decodeProjectText(taskParts[0]);
            boolean done = taskParts.length > 1 && Boolean.parseBoolean(taskParts[1]);

            if (!taskText.isBlank()) {
                tasks.add(new ProjectTask(taskText, done));
            }
        }

        return tasks;
    }

    private static String encodeProjectPhotos(List<String> photos) {
        ArrayList<String> encodedPhotos = new ArrayList<>();

        for (String photo : photos) {
            encodedPhotos.add(encodeProjectText(photo));
        }

        return String.join(PHOTO_SEPARATOR, encodedPhotos);
    }

    private static List<String> decodeProjectPhotos(String text) {
        ArrayList<String> photos = new ArrayList<>();

        if (text.isBlank()) {
            return photos;
        }

        String[] savedPhotos = text.split(PHOTO_SEPARATOR, -1);

        for (String savedPhoto : savedPhotos) {
            String photo = decodeProjectText(savedPhoto);

            if (!photo.isBlank()) {
                photos.add(photo);
            }
        }

        return photos;
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

    private static String getMultipartBoundary(String contentType) {
        if (contentType == null) {
            return null;
        }

        String[] parts = contentType.split(";");

        for (String part : parts) {
            String trimmedPart = part.trim();

            if (trimmedPart.startsWith("boundary=")) {
                return trimmedPart.substring("boundary=".length());
            }
        }

        return null;
    }

    private static MultipartForm parseMultipartForm(byte[] body, String boundary) {
        MultipartForm form = new MultipartForm();
        String bodyText = new String(body, StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;
        int position = 0;

        while (true) {
            int delimiterStart = bodyText.indexOf(delimiter, position);

            if (delimiterStart < 0) {
                break;
            }

            int partStart = delimiterStart + delimiter.length();

            if (bodyText.startsWith("--", partStart)) {
                break;
            }

            if (bodyText.startsWith("\r\n", partStart)) {
                partStart += 2;
            }

            int headersEnd = bodyText.indexOf("\r\n\r\n", partStart);

            if (headersEnd < 0) {
                break;
            }

            String headers = bodyText.substring(partStart, headersEnd);
            int contentStart = headersEnd + 4;
            int nextDelimiter = bodyText.indexOf("\r\n" + delimiter, contentStart);

            if (nextDelimiter < 0) {
                break;
            }

            byte[] content = Arrays.copyOfRange(body, contentStart, nextDelimiter);
            String fieldName = getHeaderValue(headers, "name");
            String fileName = getHeaderValue(headers, "filename");

            if (fieldName != null && fileName != null) {
                form.files.put(fieldName, new UploadedFile(fileName, content));
            } else if (fieldName != null) {
                form.fields.put(fieldName, new String(content, StandardCharsets.UTF_8));
            }

            position = nextDelimiter + 2;
        }

        return form;
    }

    private static String getHeaderValue(String headers, String key) {
        String searchText = key + "=\"";
        int valueStart = headers.indexOf(searchText);

        if (valueStart < 0) {
            return null;
        }

        valueStart += searchText.length();
        int valueEnd = headers.indexOf("\"", valueStart);

        if (valueEnd < 0) {
            return null;
        }

        return headers.substring(valueStart, valueEnd);
    }

    private static String getImageExtension(String fileName) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return ".jpg";
        }

        if (lowerName.endsWith(".png")) {
            return ".png";
        }

        if (lowerName.endsWith(".gif")) {
            return ".gif";
        }

        if (lowerName.endsWith(".webp")) {
            return ".webp";
        }

        return ".jpg";
    }

    private static String getImageContentType(String fileName) {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".png")) {
            return "image/png";
        }

        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }

        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }

        return "image/jpeg";
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

    private static class MultipartForm {
        private Map<String, String> fields = new HashMap<>();
        private Map<String, UploadedFile> files = new HashMap<>();
    }

    private static class UploadedFile {
        private String fileName;
        private byte[] bytes;

        private UploadedFile(String fileName, byte[] bytes) {
            this.fileName = fileName;
            this.bytes = bytes;
        }
    }
}
