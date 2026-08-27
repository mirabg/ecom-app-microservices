# ---------- build ----------
# Building inside the image means `docker compose build` can never pick up a
# stale jar, and no local JDK/Maven is required.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# One shared Maven repo for all modules (id=maven) so dependencies download
# once. sharing=locked serialises access: with the default sharing=shared the
# seven parallel `docker compose build` jobs corrupt each other's repo and
# the build fails with exit 127.
RUN --mount=type=cache,target=/root/.m2,id=maven,sharing=locked \
    chmod +x mvnw && ./mvnw -B -q clean package -DskipTests

# ---------- split the fat jar ----------
# Boot 3.3+/4.x: the `layertools` jarmode was removed, `tools` replaces it.
# Produces a thin app.jar plus lib/, so the (large, rarely changing) dependency
# layer stays cached while only the (small) application layer is re-pushed.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /build
COPY --from=build /src/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extracted

# ---------- runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# PodSecurity "restricted" rejects containers running as root.
RUN groupadd --system spring && useradd --system --gid spring --home /app spring

# Order matters: lib/ changes only when dependencies change.
COPY --from=extract --chown=spring:spring /build/extracted/lib/ ./lib/
COPY --from=extract --chown=spring:spring /build/extracted/app.jar ./app.jar

USER spring
EXPOSE 8080

# Without MaxRAMPercentage the JVM sizes the heap from the host's total RAM and
# gets OOM-killed under a container/pod memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
