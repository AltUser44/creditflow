FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/* && \
    curl -fsSL https://github.com/sbt/sbt/releases/download/v1.9.9/sbt-1.9.9.tgz | tar xz -C /usr/local
ENV PATH="/usr/local/sbt/bin:${PATH}"
COPY project project
COPY build.sbt .
COPY shared shared
COPY transaction-service transaction-service
RUN sbt transactionService/stage

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/transaction-service/target/universal/stage/ .
ENV JAVA_OPTS="-Xmx512m"
EXPOSE 8082
ENTRYPOINT ["bin/transaction-service"]
