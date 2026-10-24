# Stage 1: build the fat jar with the exact JDK the project targets.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies first: this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Stage 2: runtime image.
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /app/data && chown -R app:app /app
USER app

COPY --from=build /build/target/url-shortener-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
