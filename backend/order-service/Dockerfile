FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring
COPY --from=build /workspace/target/order-service-1.0.0.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]