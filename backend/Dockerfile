# ---------- 1) Build: compila o projeto com Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copiar primeiro só o pom.xml permite ao Docker reutilizar a cache das dependências
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ---------- 2) Runtime: imagem leve apenas com o JRE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/vacations-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
