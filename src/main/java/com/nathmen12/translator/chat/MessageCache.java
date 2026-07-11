package com.nathmen12.translator.chat;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MessageCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCache.class);
    private static final int MAX_SIZE = 100;
    
    private final Cache<UUID, CachedMessage> cache;
    
    public MessageCache() {
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }
    
    public void put(String originalText) {
        UUID id = UUID.randomUUID();
        cache.put(id, new CachedMessage(id, originalText));
        LOGGER.debug("Cached message with ID: {}, text length: {}", id, originalText.length());
    }
    
    public UUID putWithId(String originalText) {
        UUID id = UUID.randomUUID();
        cache.put(id, new CachedMessage(id, originalText));
        LOGGER.debug("Cached message with ID: {}, text length: {}", id, originalText.length());
        return id;
    }
    
    public CachedMessage get(UUID id) {
        return cache.getIfPresent(id);
    }
    
    public String getText(UUID id) {
        CachedMessage msg = cache.getIfPresent(id);
        return msg != null ? msg.originalText : null;
    }
    
    public boolean isTranslating(UUID id) {
        CachedMessage msg = cache.getIfPresent(id);
        return msg != null && msg.isTranslating;
    }
    
    public boolean isTranslated(UUID id) {
        CachedMessage msg = cache.getIfPresent(id);
        return msg != null && msg.isTranslated();
    }
    
    public void setTranslating(UUID id, boolean translating) {
        CachedMessage msg = cache.getIfPresent(id);
        if (msg != null) {
            msg.isTranslating = translating;
            cache.put(id, msg);
        }
    }
    
    public void setTranslated(UUID id, String translatedText, String sourceLang, String targetLang) {
        CachedMessage msg = cache.getIfPresent(id);
        if (msg != null) {
            msg.translatedText = translatedText;
            msg.sourceLang = sourceLang;
            msg.targetLang = targetLang;
            msg.isTranslated = true;
            msg.isTranslating = false;
            cache.put(id, msg);
            LOGGER.debug("Marked message {} as translated", id);
        }
    }
    
    public void setTranslationError(UUID id, String error) {
        CachedMessage msg = cache.getIfPresent(id);
        if (msg != null) {
            msg.translationError = error;
            msg.isTranslated = false;
            msg.isTranslating = false;
            cache.put(id, msg);
            LOGGER.debug("Marked message {} as failed: {}", id, error);
        }
    }
    
    public void invalidate(UUID id) {
        cache.invalidate(id);
    }
    
    public int size() {
        return (int) cache.size();
    }
    
    public void cleanUp() {
        cache.cleanUp();
    }
    
    public static class CachedMessage {
        public final UUID id;
        public final String originalText;
        public String translatedText;
        public String sourceLang;
        public String targetLang;
        public String translationError;
        public boolean isTranslated = false;
        public boolean isTranslating = false;
        public final long timestamp;
        
        public CachedMessage(UUID id, String originalText) {
            this.id = id;
            this.originalText = originalText;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isTranslated() {
            return isTranslated && translatedText != null && !translatedText.isEmpty();
        }
        
        public boolean hasError() {
            return translationError != null && !translationError.isEmpty();
        }
    }
}