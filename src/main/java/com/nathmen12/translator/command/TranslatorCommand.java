package com.nathmen12.translator.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nathmen12.translator.TranslatorMod;
import com.nathmen12.translator.chat.ChatMessageHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class TranslatorCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("translator")
            .then(ClientCommandManager.literal("translate")
                .then(ClientCommandManager.argument("messageId", StringArgumentType.greedyString())
                    .executes(TranslatorCommand::translateMessage)
                )
            )
            .then(ClientCommandManager.literal("config")
                .executes(TranslatorCommand::openConfig)
            )
            .then(ClientCommandManager.literal("toggle")
                .executes(TranslatorCommand::toggle)
            )
            .then(ClientCommandManager.literal("help")
                .executes(TranslatorCommand::showHelp)
            )
        );
    }
    
    private static int translateMessage(CommandContext<FabricClientCommandSource> context) {
        String messageIdStr = StringArgumentType.getString(context, "messageId");
        
        try {
            UUID messageId = UUID.fromString(messageIdStr);
            
            ChatMessageHandler handler = TranslatorMod.chatHandler;
            if (handler != null) {
                handler.translateMessage(messageId);
                context.getSource().sendFeedback(Text.literal("Traduction en cours...").formatted(Formatting.AQUA));
            } else {
                context.getSource().sendError(Text.literal("Erreur: Handler de chat non initialisé").formatted(Formatting.RED));
            }
            
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("ID de message invalide: " + messageIdStr).formatted(Formatting.RED));
        }
        
        return 1;
    }
    
    private static int openConfig(CommandContext<FabricClientCommandSource> context) {
        // Ouvrir l'écran de config via Cloth Config
        if (TranslatorMod.config != null) {
            // TODO: Ouvrir l'écran de config
            context.getSource().sendFeedback(Text.literal("Configuration...").formatted(Formatting.AQUA));
        }
        return 1;
    }
    
    private static int toggle(CommandContext<FabricClientCommandSource> context) {
        if (TranslatorMod.config != null) {
            TranslatorMod.config.enabled = !TranslatorMod.config.enabled;
            TranslatorMod.config.save();
            String status = TranslatorMod.config.enabled ? "activé" : "désactivé";
            context.getSource().sendFeedback(Text.literal("Traducteur " + status).formatted(Formatting.GREEN));
        }
        return 1;
    }
    
    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("=== Chat Translator ===").formatted(Formatting.GOLD, Formatting.BOLD));
        context.getSource().sendFeedback(Text.literal("/translator translate <messageId> - Traduire un message").formatted(Formatting.AQUA));
        context.getSource().sendFeedback(Text.literal("/translator toggle - Activer/Désactiver le mod").formatted(Formatting.AQUA));
        context.getSource().sendFeedback(Text.literal("/translator config - Ouvrir la configuration").formatted(Formatting.AQUA));
        context.getSource().sendFeedback(Text.literal("/translator help - Afficher cette aide").formatted(Formatting.AQUA));
        return 1;
    }
}
