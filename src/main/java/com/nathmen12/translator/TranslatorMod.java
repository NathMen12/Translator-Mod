package com.nathmen12.translator;

import com.nathmen12.translator.api.TranslatorApiClient;
import com.nathmen12.translator.chat.ChatMessageHandler;
import com.nathmen12.translator.command.TranslatorCommand;
import com.nathmen12.translator.config.ModConfig;
import com.nathmen12.translator.util.LanguageUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslatorMod implements ClientModInitializer {
    public static final String MOD_ID = "translator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TranslatorApiClient apiClient;
    public static ChatMessageHandler chatHandler;
    public static ModConfig config;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Chat Translator Mod...");

        // Charger la config
        config = ModConfig.load();

        // Initialiser le client API
        apiClient = new TranslatorApiClient(config.api.baseUrl);

        // Initialiser le handler de chat
        chatHandler = new ChatMessageHandler();

        // Enregistrer l'event de réception de message chat
        // L'interface attend: (Text, SignedMessage, GameProfile, Parameters, Instant)
        ClientReceiveMessageEvents.CHAT.register(chatHandler::onReceiveChatMessage);
        ClientReceiveMessageEvents.GAME.register(chatHandler::onReceiveGameMessage);

        // Enregistrer la commande /translator
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> TranslatorCommand.register(dispatcher));

        // Tick client pour le rate limiting / cleanup cache
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                chatHandler.tick();
            }
        });

        LOGGER.info("Chat Translator Mod initialized!");
    }

    public static String getConfigTargetLanguage() {
        if (config != null) {
            ModConfig.TargetLanguage target = config.general.targetLanguage;
            if (target != ModConfig.TargetLanguage.AUTO) {
                return target.code;
            }
            return LanguageUtils.getTargetLanguageCode();
        }
        return LanguageUtils.getTargetLanguageCode();
    }

    public static boolean isEnabled() {
        return config != null && config.general.enabled;
    }
}