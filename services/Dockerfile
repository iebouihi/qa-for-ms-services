# Runtime image shared by all services. The jar is built once by the pipeline
# (mvn package) and promoted unchanged through every environment.
FROM eclipse-temurin:21-jre-alpine
ARG JAR_FILE
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY ${JAR_FILE} app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
