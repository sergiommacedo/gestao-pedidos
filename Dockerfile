FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src/ src/

RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system appgroup \
    && useradd --system --gid appgroup appuser \
    && mkdir -p /app/uploads/logos \
    && chown -R appuser:appgroup /app

COPY --from=build /app/target/*.jar /app/app.jar

USER appuser

EXPOSE 8080

ENV TZ=America/Sao_Paulo

ENTRYPOINT ["java", "-jar", "/app/app.jar"]