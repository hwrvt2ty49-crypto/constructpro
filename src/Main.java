import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static ArrayList<Project> projects = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        addSampleProjects();

        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                listProjects();
            } else if (choice.equals("2")) {
                addProject();
            } else if (choice.equals("3")) {
                updateProjectStatus();
            } else if (choice.equals("4")) {
                running = false;
                System.out.println("Goodbye!");
            } else {
                System.out.println("Please choose 1, 2, 3, or 4.");
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=== Construction Project Tracker ===");
        System.out.println("1. List projects");
        System.out.println("2. Add a project");
        System.out.println("3. Update project status");
        System.out.println("4. Exit");
        System.out.print("Choose an option: ");
    }

    private static void listProjects() {
        if (projects.isEmpty()) {
            System.out.println("No projects yet.");
            return;
        }

        System.out.println();
        System.out.println("--- Projects ---");

        for (int i = 0; i < projects.size(); i++) {
            System.out.println();
            System.out.println((i + 1) + ".");
            projects.get(i).printDetails();
        }
    }

    private static void addProject() {
        System.out.print("Project name: ");
        String name = scanner.nextLine();

        System.out.print("Client name: ");
        String client = scanner.nextLine();

        System.out.print("Job address: ");
        String address = scanner.nextLine();

        System.out.print("Project status: ");
        String status = scanner.nextLine();

        Project project = new Project(name, client, address, status);
        projects.add(project);

        System.out.println("Project added.");
    }

    private static void updateProjectStatus() {
        if (projects.isEmpty()) {
            System.out.println("No projects to update.");
            return;
        }

        listProjectNames();

        System.out.print("Which project number do you want to update? ");
        String projectNumberText = scanner.nextLine();

        int projectIndex;

        try {
            projectIndex = Integer.parseInt(projectNumberText) - 1;
        } catch (NumberFormatException error) {
            System.out.println("Please enter a number.");
            return;
        }

        if (projectIndex < 0 || projectIndex >= projects.size()) {
            System.out.println("That project number does not exist.");
            return;
        }

        System.out.print("New status: ");
        String newStatus = scanner.nextLine();

        projects.get(projectIndex).setStatus(newStatus);
        System.out.println("Status updated.");
    }

    private static void listProjectNames() {
        System.out.println();
        System.out.println("--- Pick a Project ---");

        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            System.out.println((i + 1) + ". " + project.getName());
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
