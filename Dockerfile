FROM oraclelinux:9-slim

WORKDIR /app

COPY target/native-image/scala3-learn /app/scala3-learn

CMD ["/app/scala3-learn"]