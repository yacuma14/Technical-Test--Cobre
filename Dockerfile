FROM eclipse-temurin:18-jre
WORKDIR /app
COPY target/cobre-notification-events-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
