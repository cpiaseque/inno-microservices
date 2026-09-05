FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
RUN jar -xf app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-cp", ".:BOOT-INF/classes:BOOT-INF/lib/*", "com.innowise.paymentservice.PaymentServiceApplication"]