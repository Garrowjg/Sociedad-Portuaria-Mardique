# ---- Etapa de compilación ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Etapa de ejecución ----
FROM eclipse-temurin:21-jre
# LibreOffice (para convertir Word/Excel/PPT a PDF) + fuentes que Java necesita
# para dibujar texto con Graphics2D (libfreetype/fontconfig).
RUN apt-get update \
    && apt-get install -y --no-install-recommends libreoffice fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/uploads/intranet
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
ENV INTRANET_UPLOAD_DIR=/app/uploads/intranet
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]