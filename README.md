# Construction Java App

This is a beginner Java app for tracking construction projects.

Right now it can:

- Show a menu
- List construction projects
- Add a new project
- Update a project's status
- Run as a simple browser app at `http://localhost:8080`
- Save browser app projects to `projects.txt`
- Add, edit, complete, and delete tasks under each project
- Upload photos under each project

## Where the Code Goes

The code is in:

```text
src/Main.java
src/Project.java
src/WebApp.java
```

`Main.java` starts the app and shows the menu.

`Project.java` describes what information a construction project has.

`WebApp.java` starts a simple website version of the app.

`projects.txt` is created automatically when the browser app runs. It stores
the projects so they are still there after you stop and restart the app.

## How to Run It

First, install Java. You need the JDK, not just the regular Java runtime.

Then open a terminal in this folder:

```text
construction-java-app
```

Compile the code:

```bash
javac src/*.java
```

Run the app:

```bash
java -cp src Main
```

## How to Run the Browser App

Compile the code:

```bash
javac src/*.java
```

Start the website:

```bash
java -cp src WebApp
```

Then open this in your browser:

```text
http://localhost:8080
```

To test saving:

1. Add a project in the browser.
2. Stop the app.
3. Start it again with `java -cp src WebApp`.
4. Refresh the browser.

Your project should still be listed.

## How to Put It Online

See:

```text
DEPLOY.md
```

## What to Learn First

Start with `src/Main.java`.

Look for this line:

```java
public static void main(String[] args)
```

That is where the app starts running.

Then look at:

```java
private static void printMenu()
```

That function prints the menu.

Then look at:

```java
private static void addProject()
```

That function asks the user questions and creates a new project.

## Good Next Improvements

After this works, good next steps are:

- Add daily logs
- Add material tracking
- Add change orders
- Replace `projects.txt` with a real database
