# --- STAGE 1: Build ---
# Menggunakan image Maven dengan JDK 21 untuk meng-compile proyek kita
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Menjalankan build Maven untuk menghasilkan file JAR
RUN mvn clean package -DskipTests

# --- STAGE 2: Run ---
# Menggunakan image Java Runtime Environment (JRE) yang lebih kecil untuk menjalankan aplikasi
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Menyalin file JAR yang sudah jadi dari stage 'builder'
COPY --from=builder /app/target/*.jar app.jar
# Memberitahu Docker bahwa aplikasi kita berjalan di port 8080
EXPOSE 8080
# Perintah untuk menjalankan aplikasi saat container dimulai
ENTRYPOINT ["java", "-jar", "app.jar"]