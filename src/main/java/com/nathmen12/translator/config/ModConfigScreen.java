package com.nathmen12.translator.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModConfigScreen {
    
    public static Screen createConfigScreen(Screen parent) {
        // Config screen temporarily disabled - requires cloth-config2 dependency
        return new Screen(Text.literal("Erreur de configuration")) {
            @Override
            public void close() {
                if (this.client != null) {
                    this.client.setScreen(parent);
                }
            }
        };
    }
}
