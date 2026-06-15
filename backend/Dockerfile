FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/health-diagnosis-0.0.1-SNAPSHOT.jar health-diagnosis-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","health-diagnosis-0.0.1-SNAPSHOT.jar"]