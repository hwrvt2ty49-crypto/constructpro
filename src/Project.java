import java.util.ArrayList;
import java.util.List;

public class Project {
    private String name;
    private String client;
    private String address;
    private String status;
    private ArrayList<ProjectTask> tasks;
    private ArrayList<String> photos;
    private ArrayList<ProjectMaterial> materials;
    private double estimatedBudget;
    private double actualCost;

    public Project(String name, String client, String address, String status) {
        this(name, client, address, status, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, 0);
    }

    public Project(String name, String client, String address, String status, List<ProjectTask> tasks, List<String> photos) {
        this(name, client, address, status, tasks, photos, new ArrayList<>(), 0, 0);
    }

    public Project(
            String name,
            String client,
            String address,
            String status,
            List<ProjectTask> tasks,
            List<String> photos,
            List<ProjectMaterial> materials,
            double estimatedBudget,
            double actualCost
    ) {
        this.name = name;
        this.client = client;
        this.address = address;
        this.status = status;
        this.tasks = new ArrayList<>(tasks);
        this.photos = new ArrayList<>(photos);
        this.materials = new ArrayList<>(materials);
        this.estimatedBudget = estimatedBudget;
        this.actualCost = actualCost;
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

    public List<ProjectMaterial> getMaterials() {
        return materials;
    }

    public void addMaterial(String name, String quantity, String status) {
        materials.add(new ProjectMaterial(name, quantity, status));
    }

    public void updateMaterial(int materialIndex, String name, String quantity, String status) {
        ProjectMaterial material = materials.get(materialIndex);
        material.setName(name);
        material.setQuantity(quantity);
        material.setStatus(status);
    }

    public void deleteMaterial(int materialIndex) {
        materials.remove(materialIndex);
    }

    public double getEstimatedBudget() {
        return estimatedBudget;
    }

    public double getActualCost() {
        return actualCost;
    }

    public double getBudgetRemaining() {
        return estimatedBudget - actualCost;
    }

    public void updateBudget(double estimatedBudget, double actualCost) {
        this.estimatedBudget = estimatedBudget;
        this.actualCost = actualCost;
    }

    public void printDetails() {
        System.out.println("Project: " + name);
        System.out.println("Client: " + client);
        System.out.println("Address: " + address);
        System.out.println("Status: " + status);
    }
}
