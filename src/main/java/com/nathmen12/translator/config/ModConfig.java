package com.nathmen12.translator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Config(name = "translator")
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public GeneralSettings general = new GeneralSettings();
    
    @ConfigEntry.Gui.CollapsibleObject
    public ApiSettings api = new ApiSettings();
    
    @ConfigEntry.Gui.CollapsibleObject
    public DisplaySettings display = new DisplaySettings();
    
    // Champs de compatibilité pour l'ancien code
    public boolean enabled = true;
    public String targetLanguage = "auto";
    public String apiEndpoint = "https://nathmen12-translator-api.hf.space";
    
    public static ModConfig load() {
        try {
            AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
            return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        } catch (Exception e) {
            return new ModConfig();
        }
    }
    
    public void save() {
        try {
            AutoConfig.getConfigHolder(ModConfig.class).save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Sync legacy fields
        this.enabled = general.enabled;
        this.targetLanguage = general.targetLanguage.code;
        this.apiEndpoint = api.baseUrl;
    }
    
    public static class GeneralSettings {
        @Comment("Enable/disable the chat translator mod")
        public boolean enabled = true;
        
        @Comment("Target language for translation (auto = use Minecraft language)")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public TargetLanguage targetLanguage = TargetLanguage.AUTO;
        
        @Comment("Only show translate button on hover")
        public boolean showButtonOnHover = false;
        
        @Comment("Automatically translate messages without clicking")
        public boolean autoTranslate = false;
    }
    
    public static class ApiSettings {
        @Comment("Translator API base URL")
        public String baseUrl = "https://nathmen12-translator-api.hf.space";
        
    @Comment("Request timeout in seconds")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 60)
    public int timeout = 30;
    
    @Comment("Maximum text length to translate")
    @ConfigEntry.BoundedDiscrete(min = 100, max = 5000)
    public int maxTextLength = 5000;
    }
    
    public static class DisplaySettings {
        @Comment("Show original text in tooltip when hovering translated message")
        public boolean showOriginalInTooltip = true;
        
        @Comment("Show translation source language in tooltip")
        public boolean showSourceLanguage = true;
        
        @Comment("Show translation duration in tooltip")
        public boolean showDuration = false;
        
        @Comment("Color of the translate button")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ButtonColor translateButtonColor = ButtonColor.AQUA;
        
        @Comment("Color of the translated icon")
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ButtonColor translatedIconColor = ButtonColor.GREEN;
    }
    
    public enum TargetLanguage {
        AUTO("auto", "Auto (Minecraft Language)"),
        EN("en", "English"),
        FR("fr", "French"),
        DE("de", "German"),
        ES("es", "Spanish"),
        IT("it", "Italian"),
        PT("pt", "Portuguese"),
        RU("ru", "Russian"),
        ZH("zh", "Chinese"),
        JA("ja", "Japanese"),
        KO("ko", "Korean"),
        AR("ar", "Arabic"),
        PL("pl", "Polish"),
        NL("nl", "Dutch"),
        TR("tr", "Turkish");
        
        public final String code;
        public final String displayName;
        
        TargetLanguage(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName + " (" + code + ")";
        }
    }
    
public enum ButtonColor {
        AQUA("aqua", "Aqua"),
        BLUE("blue", "Blue"),
        GREEN("green", "Green"),
        YELLOW("yellow", "Yellow"),
        GOLD("gold", "Gold"),
        LIGHT_PURPLE("light_purple", "Light Purple"),
        WHITE("white", "White");
        
        public final String code;
        public final String displayName;
        
        ButtonColor(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
}
