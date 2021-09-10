FROM alvistack/openjdk-16:20210904.0.0
WORKDIR /nowbot
COPY . .
RUN apt-get update
RUN mvn install
RUN mvn compile
CMD ["mvn", "exec:java -X -Dexec.mainClass='com.now.nowbot.NowbotApplication' -Dexec.classpathScope=runtime"]
