# ---------- Етап 1: збірка .jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Спочатку тільки файли збірки: якщо pom.xml не змінився,
# Docker візьме шар із залежностями з кешу і не качатиме їх знову
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Потім код (змінюється часто)
COPY src/ src/
RUN ./mvnw -B package -DskipTests

# ---------- Етап 2: розкладання .jar на шари ----------
FROM eclipse-temurin:21-jre AS extract
WORKDIR /extract
COPY --from=build /workspace/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- Етап 3: фінальний образ ----------
FROM eclipse-temurin:21-jre
WORKDIR /application

# Не запускаємо застосунок від root
RUN useradd --system --uid 10001 spring

COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]