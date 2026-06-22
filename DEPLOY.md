# Deploy the Construction App

Deploy means putting the app online so it has a real website link.

## Step 1: Make Sure It Runs Locally

From inside the `construction-java-app` folder, run:

```bash
javac src/*.java
java -cp src WebApp
```

Then open:

```text
http://localhost:8080
```

## Step 2: Create a GitHub Account

Go to:

```text
https://github.com
```

Create an account if you do not already have one.

## Step 3: Create a New GitHub Repository

On GitHub:

1. Click the plus button.
2. Click `New repository`.
3. Name it `construction-java-app`.
4. Choose `Public` or `Private`.
5. Click `Create repository`.

## Step 4: Upload Your Code to GitHub

In VS Code, open the terminal inside the `construction-java-app` folder.

Run these commands:

```bash
git init
git add .
git commit -m "First construction web app"
```

Then GitHub will show commands that look like this:

```bash
git remote add origin https://github.com/YOUR_USERNAME/construction-java-app.git
git branch -M main
git push -u origin main
```

Use the commands GitHub gives you, because your username will be different.

## Step 5: Create a Render Account

Go to:

```text
https://render.com
```

Sign up and connect your GitHub account.

## Step 6: Create a Web Service on Render

In Render:

1. Click `New`.
2. Click `Web Service`.
3. Pick your `construction-java-app` GitHub repository.
4. For runtime, choose `Docker`.
5. Render will use the `Dockerfile` in this project.

6. Click `Deploy Web Service`.

## Step 7: Open Your Live Website

When Render finishes, it gives you a link like:

```text
https://construction-java-app.onrender.com
```

That is your live website.

## Important Note About Saving

The current app saves projects to `projects.txt`.

That works well on your computer.

On real hosting, saved files may disappear when the server restarts. Later,
you should replace `projects.txt` with a real database.
