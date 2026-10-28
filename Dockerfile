FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp verify
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --create-home setu && mkdir -p /app/data/uploads && chown -R setu:setu /app
WORKDIR /app
COPY --from=build /build/target/global-setu-0.1.0.jar app.jar
USER setu
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
