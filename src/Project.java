import java.util.ArrayList;
import java.util.List;

public class Project {
    private String name;
    private String client;
    private String address;
    private String status;
    private ArrayList<ProjectTask> tasks;
    private ArrayList<String> photos;

    public Project(String name, String client, String address, String status) {
        this(name, client, address, status, new ArrayList<>(), new ArrayList<>());
    }

    public Project(String name, String client, String address, String status, List<ProjectTask> tasks, List<String> photos) {
        this.name = name;
        this.client = client;
        this.address = address;
        this.status = status;
        this.tasks = new ArrayList<>(tasks);
        this.photos = new ArrayList<>(photos);
    }

    public String getName() {
        return name;
    }

    public String getClient() {
        return client;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ProjectTask> getTasks() {
        return tasks;
    }

    public void addTask(String text) {
        tasks.add(new ProjectTask(text, false));
    }

    public void updateTask(int taskIndex, String text) {
        tasks.get(taskIndex).setText(text);
    }

    public void toggleTask(int taskIndex) {
        ProjectTask task = tasks.get(taskIndex);
        task.setDone(!task.isDone());
    }

    public void deleteTask(int taskIndex) {
        tasks.remove(taskIndex);
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void addPhoto(String photoPath) {
        photos.add(photoPath);
    }

    public void printDetails() {
        System.out.println("Project: " + name);
        System.out.println("Client: " + client);
        System.out.println("Address: " + address);
        System.out.println("Status: " + status);
    }
}
