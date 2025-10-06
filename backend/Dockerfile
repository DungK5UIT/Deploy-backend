# Giai đoạn 1: Build ứng dụng với Maven và Java 17
FROM maven:3.8.5-openjdk-17 AS build

# Tạo thư mục làm việc
WORKDIR /app

# Copy file pom.xml và tải dependency trước để tận dụng cache
COPY backend/pom.xml ./pom.xml
RUN mvn dependency:go-offline

# Copy toàn bộ source code của backend
COPY backend/src ./src

# Build ứng dụng và đóng gói thành file JAR
RUN mvn package -DskipTests

# Giai đoạn 2: Chạy ứng dụng trên một image Java gọn nhẹ hơn
FROM openjdk:17-jdk-slim

# Tạo thư mục làm việc
WORKDIR /app

# Copy file JAR đã được build từ giai đoạn 1
COPY --from=build /app/target/*.jar app.jar

# Lệnh để chạy ứng dụng khi container khởi động
ENTRYPOINT ["java","-jar","app.jar"]