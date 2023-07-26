FROM postgres:latest
WORKDIR /nowbot
COPY . .
RUN apt-get update
CMD ["mvn", "exec:java -X -Dexec.mainClass='com.now.nowbot.NowbotApplication' -Dexec.classpathScope=runtime"]
