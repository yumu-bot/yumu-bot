package com.now.nowbot.config;

import com.neovisionaries.ws.client.WebSocketFactory;
import com.now.nowbot.aop.OpenResource;
import com.now.nowbot.controller.BotWebApi;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Configuration
@Component
@ConfigurationProperties(prefix = "discord", ignoreInvalidFields = true)

public class DiscordConfig {

    private String token;
    private String commandSuffix;

    @Bean
    public JDA jda(List<ListenerAdapter> listenerAdapters, OkHttpClient okHttpClient) {
        WebSocketFactory factory = new WebSocketFactory();
        factory.getProxySettings().setHost("127.0.0.1").setPort(7899);
        JDA build = JDABuilder.createDefault(token)
                .setHttpClient(okHttpClient)
                .setWebsocketFactory(factory)
                .addEventListeners(listenerAdapters.toArray())
                .build();

        for (Method declaredMethod : BotWebApi.class.getDeclaredMethods()) {
            OpenResource methodAnnotation = declaredMethod.getAnnotation(OpenResource.class);
            if (methodAnnotation == null) {
                continue;
            }
            String name = declaredMethod.getName();
            SlashCommandData commandData = Commands.slash((commandSuffix + name).toLowerCase(), methodAnnotation.desp());
            for (Parameter parameter : declaredMethod.getParameters()) {
                OpenResource parameterAnnotation = parameter.getAnnotation(OpenResource.class);
                String parameterName = parameterAnnotation.name();
                OptionData optionData = new OptionData(OptionType.STRING, parameterName.toLowerCase(), parameterAnnotation.desp());
                if (parameterName.equals("mode")) {
                    optionData.addChoice("OSU", "OSU");
                    optionData.addChoice("TAIKO", "TAIKO");
                    optionData.addChoice("CATCH", "CATCH");
                    optionData.addChoice("MANIA", "MANIA");
                }
                optionData.setRequired(parameterAnnotation.required());
                commandData.addOptions(optionData);
            }
            build.upsertCommand(commandData).complete();
        }


        return build;
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
