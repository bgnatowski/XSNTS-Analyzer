# Użyj oficjalnego obrazu Maven do budowy artefaktu
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

# Skopiuj pliki POM i źródła
COPY pom.xml .
COPY src ./src

# Wykonaj build aplikacji (z pominięciem testów)
RUN mvn clean package -DskipTests

# Użyj JRE uruchomienia aplikacji
FROM arm64v8/eclipse-temurin:17-jre-jammy

# Ustaw katalog roboczy
WORKDIR /app

# Skopiuj zbudowany plik JAR z etapu build
COPY --from=build /app/target/XSNTS-Analyzer-0.0.1-SNAPSHOT.jar app.jar

# Ustaw port aplikacji (możesz wybić inny niż 8080, np. 8081)
ENV SERVER_PORT=8081

# Uruchom aplikację
ENTRYPOINT ["java","-jar","/app/app.jar"]
