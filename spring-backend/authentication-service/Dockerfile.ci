FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/*SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
