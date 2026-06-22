public class Project {
    private String name;
    private String client;
    private String address;
    private String status;

    public Project(String name, String client, String address, String status) {
        this.name = name;
        this.client = client;
        this.address = address;
        this.status = status;
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

    public void printDetails() {
        System.out.println("Project: " + name);
        System.out.println("Client: " + client);
        System.out.println("Address: " + address);
        System.out.println("Status: " + status);
    }
}
