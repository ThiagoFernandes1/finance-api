# Build em estagio separado para que a imagem final nao carregue Maven nem o codigo-fonte.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# Copia so o pom primeiro: a camada de dependencias so invalida quando o pom muda.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sem privilegios para reduzir a superficie de ataque do container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
