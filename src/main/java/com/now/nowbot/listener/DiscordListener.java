package com.now.nowbot.listener;

import com.now.nowbot.aop.OpenResource;
import com.now.nowbot.controller.BotWebApi;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Component
public class DiscordListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DiscordListener.class);

    @Autowired
    BotWebApi botWebApi;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Optional<Method> first = Arrays.stream(botWebApi.getClass().getDeclaredMethods()).filter(
                method -> {
                    OpenResource annotation = method.getAnnotation(OpenResource.class);
                    if (annotation == null) {
                        return false;
                    }
                    return annotation.name().equalsIgnoreCase(event.getName().substring(event.getName().indexOf("-") + 1));

                }
        ).findFirst();
        if (first.isPresent()) {
            Method method = first.get();
            Parameter[] parameters = method.getParameters();
            Object[] objects = new Object[parameters.length];
            try {
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    OptionMapping option = event.getOption(parameter.getName().toLowerCase());
                    if (option == null) {
                        continue;
                    }
                    Class<?> type = parameter.getType();
                    if (type.equals(int.class) || type.equals(Integer.class)) {
                        objects[i] = option.getAsInt();
                    } else if (type.equals(String.class)) {
                        objects[i] = option.getAsString();
                    }
                }
                Object invoke = method.invoke(botWebApi, objects);
                if (invoke instanceof ResponseEntity) {
                    //noinspection unchecked
                    ResponseEntity<byte[]> response = (ResponseEntity<byte[]>) invoke;
                    FileUpload fileUpload = FileUpload.fromData(Objects.requireNonNull(response.getBody()), event.getName() + ".png");
                    event.getHook().sendFiles(fileUpload).queue();

                } else if (invoke instanceof String str) {
                    event.getHook().sendMessage(str).queue();
                }
            } catch (Exception e) {
                log.error("处理命令时发生了异常", e);
                Throwable throwable;
                if (e.getCause() != null) {
                    throwable = e.getCause();
                } else {
                    throwable = e;
                }
                event.getHook().sendMessage("处理命令时发生了异常," + throwable.getMessage()).queue();
            }

        } else {
            event.getHook().sendMessage("Can't find any handler to handle this command").queue();
        }
    }
}
