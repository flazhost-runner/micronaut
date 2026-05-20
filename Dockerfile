FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/app-1.0.0.jar /app/app.jar
ENV PORT=80
EXPOSE 80
CMD ["java", "-jar", "/app/app.jar"]
