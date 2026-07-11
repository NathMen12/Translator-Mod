package com.nathmen12.translator.util;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LanguageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageUtils.class);
    
    // Mapping Minecraft language codes -> ISO 639-1 codes (API Translator)
    private static final Map<String, String> MINECRAFT_TO_ISO = new HashMap<>();
    
    static {
        // Langues principales
        MINECRAFT_TO_ISO.put("en_us", "en");
        MINECRAFT_TO_ISO.put("en_gb", "en");
        MINECRAFT_TO_ISO.put("fr_fr", "fr");
        MINECRAFT_TO_ISO.put("de_de", "de");
        MINECRAFT_TO_ISO.put("es_es", "es");
        MINECRAFT_TO_ISO.put("es_mx", "es");
        MINECRAFT_TO_ISO.put("it_it", "it");
        MINECRAFT_TO_ISO.put("pt_br", "pt");
        MINECRAFT_TO_ISO.put("pt_pt", "pt");
        MINECRAFT_TO_ISO.put("ru_ru", "ru");
        MINECRAFT_TO_ISO.put("zh_cn", "zh");
        MINECRAFT_TO_ISO.put("zh_tw", "zh");
        MINECRAFT_TO_ISO.put("ja_jp", "ja");
        MINECRAFT_TO_ISO.put("ko_kr", "ko");
        MINECRAFT_TO_ISO.put("ar_sa", "ar");
        MINECRAFT_TO_ISO.put("pl_pl", "pl");
        MINECRAFT_TO_ISO.put("nl_nl", "nl");
        MINECRAFT_TO_ISO.put("cs_cz", "cs");
        MINECRAFT_TO_ISO.put("hu_hu", "hu");
        MINECRAFT_TO_ISO.put("tr_tr", "tr");
        MINECRAFT_TO_ISO.put("vi_vn", "vi");
        MINECRAFT_TO_ISO.put("th_th", "th");
        MINECRAFT_TO_ISO.put("el_gr", "el");
        MINECRAFT_TO_ISO.put("he_il", "he");
        MINECRAFT_TO_ISO.put("id_id", "id");
        MINECRAFT_TO_ISO.put("ms_my", "ms");
        MINECRAFT_TO_ISO.put("tl_ph", "tl");
        MINECRAFT_TO_ISO.put("sv_se", "sv");
        MINECRAFT_TO_ISO.put("da_dk", "da");
        MINECRAFT_TO_ISO.put("no_no", "no");
        MINECRAFT_TO_ISO.put("fi_fi", "fi");
        MINECRAFT_TO_ISO.put("ro_ro", "ro");
        MINECRAFT_TO_ISO.put("bg_bg", "bg");
        MINECRAFT_TO_ISO.put("hr_hr", "hr");
        MINECRAFT_TO_ISO.put("sk_sk", "sk");
        MINECRAFT_TO_ISO.put("sl_si", "sl");
        MINECRAFT_TO_ISO.put("et_ee", "et");
        MINECRAFT_TO_ISO.put("lv_lv", "lv");
        MINECRAFT_TO_ISO.put("lt_lt", "lt");
        MINECRAFT_TO_ISO.put("uk_ua", "uk");
        MINECRAFT_TO_ISO.put("be_by", "be");
        MINECRAFT_TO_ISO.put("ka_ge", "ka");
        MINECRAFT_TO_ISO.put("hy_am", "hy");
        MINECRAFT_TO_ISO.put("az_az", "az");
        MINECRAFT_TO_ISO.put("kk_kz", "kk");
        MINECRAFT_TO_ISO.put("ky_kg", "ky");
        MINECRAFT_TO_ISO.put("uz_uz", "uz");
        MINECRAFT_TO_ISO.put("mn_mn", "mn");
        MINECRAFT_TO_ISO.put("ne_np", "ne");
        MINECRAFT_TO_ISO.put("si_lk", "si");
        MINECRAFT_TO_ISO.put("my_mm", "my");
        MINECRAFT_TO_ISO.put("km_kh", "km");
        MINECRAFT_TO_ISO.put("lo_la", "lo");
    }

    public static String getMinecraftLanguage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            return client.options.language;
        }
        return Locale.getDefault().toString().toLowerCase().replace('_', '-');
    }

    public static String getTargetLanguageCode() {
        String mcLang = getMinecraftLanguage().toLowerCase();
        return MINECRAFT_TO_ISO.getOrDefault(mcLang, "en");
    }

    public static String getTargetLanguageCode(String minecraftLang) {
        if (minecraftLang == null || minecraftLang.isEmpty()) {
            return "en";
        }
        return MINECRAFT_TO_ISO.getOrDefault(minecraftLang.toLowerCase(), "en");
    }

    public static String getLanguageDisplayName(String langCode) {
        Locale locale = new Locale(langCode);
        return locale.getDisplayLanguage(locale);
    }

    public static String getLanguageDisplayName(String langCode, Locale displayLocale) {
        Locale locale = new Locale(langCode);
        return locale.getDisplayLanguage(displayLocale);
    }

    public static Map<String, String> getSupportedLanguages() {
        return new HashMap<>(MINECRAFT_TO_ISO);
    }

    public static boolean isLanguageSupported(String langCode) {
        return MINECRAFT_TO_ISO.containsValue(langCode.toLowerCase());
    }
}