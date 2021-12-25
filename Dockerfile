FROM postgres:latest
WORKDIR /nowbot
COPY . .
RUN apt-get update
RUN mvn install
RUN mvn compile
RUN PATH="/opt/gtk/bin:$PATH"
RUN export PATH
RUN export '/java/bin'
RUN export '/maven/bin'
CMD ["mvn", "exec:java -X -Dexec.mainClass='com.now.nowbot.NowbotApplication' -Dexec.classpathScope=runtime"]
