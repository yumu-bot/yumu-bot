FROM postgres:latest
WORKDIR /nowbot
COPY . .
RUN apt-get update
RUN ./src/main/resources/install.sh
CMD ["mvn", "exec:java -X -Dexec.mainClass='com.now.nowbot.NowbotApplication' -Dexec.classpathScope=runtime"]
