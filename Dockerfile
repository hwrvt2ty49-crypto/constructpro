FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY src ./src
COPY projects.txt ./projects.txt

RUN javac src/*.java

CMD ["java", "-cp", "src", "WebApp"]
