package com.now.nowbot.config;

import com.neovisionaries.ws.client.WebSocketFactory;
import com.now.nowbot.aop.OpenResource;
import com.now.nowbot.controller.BotWebApi;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.now.nowbot.config.AsyncSetting.V_THREAD_FACORY;

@Configuration
@Component
@ConfigurationProperties(prefix = "discord", ignoreInvalidFields = true)

public class DiscordConfig {
    private static final Logger log = LoggerFactory.getLogger(DiscordConfig.class);

    private String token;
    private String commandSuffix;

    @Bean
    public JDA jda(List<ListenerAdapter> listenerAdapters, OkHttpClient okHttpClient, NowbotConfig config, ThreadPoolTaskExecutor botAsyncExecutor) {
        WebSocketFactory factory = new WebSocketFactory();
        var proy = factory.getProxySettings();
        if (config.proxyPort != 0)
            proy.setHost("localhost").setPort(config.proxyPort);
        JDA jda;
        try {
            var scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(0, V_THREAD_FACORY);
            jda = JDABuilder
                    .createDefault(token)
                    .setHttpClient(okHttpClient)
                    .setWebsocketFactory(factory)
                    .addEventListeners(listenerAdapters.toArray())
                    .setCallbackPool(botAsyncExecutor.getThreadPoolExecutor())
                    .setGatewayPool(scheduledThreadPoolExecutor)
                    .setRateLimitPool(scheduledThreadPoolExecutor)
                    .setEventPool(botAsyncExecutor.getThreadPoolExecutor())
                    .setAudioPool(scheduledThreadPoolExecutor)
                    .build();
            jda.awaitReady();
        } catch (Exception e) {
            log.error("create jda error", e);
            return null;
        }

        for (Command command : jda.retrieveCommands().complete()) {
            command.delete().complete();
        }
        for (Method declaredMethod : BotWebApi.class.getDeclaredMethods()) {
            OpenResource methodAnnotation = declaredMethod.getAnnotation(OpenResource.class);
            if (methodAnnotation == null) {
                continue;
            }
            String name = methodAnnotation.name();
            SlashCommandData commandData = Commands.slash((commandSuffix + name).toLowerCase(), methodAnnotation.desp());
            for (Parameter parameter : declaredMethod.getParameters()) {
                OpenResource parameterAnnotation = parameter.getAnnotation(OpenResource.class);
                if (parameterAnnotation == null) {
                    continue;
                }
                OptionType optionType;
                Class<?> type = parameter.getType();
                if (type.equals(int.class) || type.equals(Integer.class)) {
                    optionType = OptionType.INTEGER;
                } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
                    optionType = OptionType.BOOLEAN;
                } else {
                    optionType = OptionType.STRING;
                }
                String parameterName = parameterAnnotation.name();
                OptionData optionData = new OptionData(optionType, parameterName.toLowerCase(), parameterAnnotation.desp());
                if (parameterName.equals("mode")) {
                    optionData.addChoice("OSU", "OSU");
                    optionData.addChoice("TAIKO", "TAIKO");
                    optionData.addChoice("CATCH", "CATCH");
                    optionData.addChoice("MANIA", "MANIA");
                }
                optionData.setRequired(parameterAnnotation.required());
                commandData.addOptions(optionData);
            }
            jda.upsertCommand(commandData).complete();
        }

        return jda;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCommandSuffix() {
        return commandSuffix;
    }

    public void setCommandSuffix(String commandSuffix) {
        this.commandSuffix = commandSuffix;
    }
}
