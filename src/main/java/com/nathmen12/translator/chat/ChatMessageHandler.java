package com.nathmen12.translator.chat;

import com.nathmen12.translator.TranslatorMod;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.authlib.GameProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ChatMessageHandler implements ClientReceiveMessageEvents.Chat, ClientReceiveMessageEvents.Game {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageHandler.class);
    private static final String TRANSLATE_COMMAND_PREFIX = "/translator translate ";
    private static final String TRANSLATE_ICON = "🌐";
    private static final String TRANSLATED_ICON = "✅";
    private static final String TRANSLATING_ICON = "⏳";
    
    private final MessageCache messageCache = new MessageCache();
    
    @Override
    public void onReceiveChatMessage(Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters parameters, java.time.Instant instant) {
        // L'interface attend GameProfile, on extrait l'UUID
        java.util.UUID uuid = sender != null ? sender.getId() : null;
        handleMessage(message, false, uuid);
    }
    
    @Override
    public void onReceiveGameMessage(Text message, boolean overlay) {
        handleMessage(message, overlay, null);
    }
    
    private void handleMessage(Text message, boolean overlay, java.util.UUID sender) {
        if (!TranslatorMod.isEnabled()) {
            return;
        }
        
        // Ne pas ajouter de bouton aux messages déjà traduits ou aux messages système
        if (isTranslatedMessage(message) || isSystemMessage(message)) {
            return;
        }
        
        // Extraire le texte brut du message
        String plainText = message.getString().trim();
        if (plainText.isEmpty() || plainText.length() < 2) {
            return;
        }
        
        // Ignorer nos propres messages envoyés (pas reçus)
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && sender != null && sender.equals(player.getUuid())) {
            return;
        }
        
        // Créer un ID unique pour ce message
        UUID messageId = messageCache.putWithId(plainText);
        
        // Injecter le bouton de traduction dans le message
        // Note: On ne peut pas modifier le message directement dans l'event Fabric
        // On doit stocker l'association messageId -> message original pour le re-rendu
    }
    
    public Text injectTranslateButton(Text originalMessage, java.util.UUID messageId) {
        String command = TRANSLATE_COMMAND_PREFIX + messageId.toString();
        
        MutableText translateButton = Text.literal(" " + TRANSLATE_ICON + " ")
            .setStyle(Style.EMPTY
                .withColor(Formatting.AQUA)
                .withBold(true)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("translator.tooltip.translate")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withInsertion(command));
        
        // Copier le message original et ajouter le bouton à la fin
        MutableText copy = originalMessage.copy();
        copy.append(translateButton);
        
        return copy;
    }
    
    public void translateMessage(UUID messageId) {
        String originalText = messageCache.getText(messageId);
        if (originalText == null || originalText.isEmpty()) {
            LOGGER.warn("Message not found in cache: {}", messageId);
            return;
        }
        
        // Vérifier si déjà en cours de traduction ou déjà traduit
        if (messageCache.isTranslating(messageId) || messageCache.isTranslated(messageId)) {
            return;
        }
        
        messageCache.setTranslating(messageId, true);
        
        String targetLang = TranslatorMod.getConfigTargetLanguage();
        
        TranslatorMod.apiClient.translate(originalText, "auto", targetLang)
            .thenAccept(result -> {
                if (result != null && result.translatedText != null) {
                    messageCache.setTranslated(messageId, result.translatedText, result.source, targetLang);
                    // Re-render le message traduit
                    reRenderTranslatedMessage(messageId, result.translatedText, originalText);
                } else {
                    String error = result != null ? result.error : "Unknown error";
                    messageCache.setTranslationError(messageId, error);
                    showTranslationError(messageId, error);
                }
                messageCache.setTranslating(messageId, false);
            })
            .exceptionally(throwable -> {
                LOGGER.error("Translation failed for message {}", messageId, throwable);
                messageCache.setTranslationError(messageId, throwable.getMessage());
                showTranslationError(messageId, throwable.getMessage());
                messageCache.setTranslating(messageId, false);
                return null;
            });
    }
    
    private void reRenderTranslatedMessage(UUID messageId, String translatedText, String originalText) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        
        // Créer le message traduit avec tooltip du texte original
        MutableText translatedMessage = Text.literal(translatedText)
            .setStyle(Style.EMPTY
                .withColor(Formatting.GREEN)
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal(originalText).formatted(Formatting.GRAY).formatted(Formatting.ITALIC)
                )));
        
        // Ajouter l'icône de traduit
        MutableText icon = Text.literal(" " + TRANSLATED_ICON + " ")
            .setStyle(Style.EMPTY
                .withColor(Formatting.GREEN)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("translator.tooltip.translated"))));
        
        translatedMessage.append(icon);
        
        // Envoyer au chat
        client.inGameHud.getChatHud().addMessage(translatedMessage);
    }
    
    private void showTranslationError(UUID messageId, String error) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        
        MutableText errorMessage = Text.literal("❌ Erreur traduction: " + error)
            .setStyle(Style.EMPTY.withColor(Formatting.RED));
        
        client.inGameHud.getChatHud().addMessage(errorMessage);
    }
    
    private boolean isTranslatedMessage(Text message) {
        String text = message.getString();
        return text.contains(TRANSLATED_ICON) || text.contains("✅");
    }
    
    private boolean isSystemMessage(Text message) {
        String text = message.getString();
        return text.startsWith("[") && text.contains("]") && 
               (text.contains("Server") || text.contains("Info") || text.contains("Warning") ||
                text.contains("connected") || text.contains("disconnected") || text.contains("joined") ||
                text.contains("left") || text.contains("server") || text.contains("world"));
    }
    
    public void tick() {
        messageCache.cleanUp();
    }
    
    public MessageCache getMessageCache() {
        return messageCache;
    }
}