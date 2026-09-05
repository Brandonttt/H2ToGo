# Imagen del backend H2ToGo: se construye y se ejecuta enteramente dentro de Docker,
# así que basta con tener Docker instalado (ni JDK ni Maven en la máquina).
#
# Etapa 1 (build): compila y empaqueta el jar ejecutable con Maven + JDK 21.
#   Se saltan las pruebas: las de integración necesitan Docker (Testcontainers), que no
#   está disponible dentro del contenedor de build. Correr `mvn verify` es tarea del CI/host.
# Etapa 2 (runtime): solo el JRE 21 + el jar. El grafo OSM viaja dentro del jar (es un recurso).

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copiamos primero el POM (y la config de Maven) para cachear la descarga de dependencias.
COPY pom.xml .
COPY .mvn/ .mvn/
RUN mvn -B -q dependency:go-offline

# Ahora el código; empaqueta el jar (sin pruebas).
COPY src/ src/
RUN mvn -B -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# El jar repackaged de Spring Boot (excluye el *.jar.original).
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
