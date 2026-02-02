package com.chessconnect.config;

import com.chessconnect.service.ThumbnailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Initializes thumbnails for existing recordings on application startup.
 * Runs asynchronously to not block application startup.
 */
@Component
public class ThumbnailInitializer {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailInitializer.class);

    private final ThumbnailService thumbnailService;

    public ThumbnailInitializer(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void onApplicationReady() {
        log.info("Checking for missing video thumbnails...");
        try {
            if (thumbnailService.isFfmpegAvailable()) {
                thumbnailService.generateMissingThumbnails();
            } else {
                log.warn("FFmpeg not available - skipping thumbnail generation");
            }
        } catch (Exception e) {
            log.error("Error during thumbnail initialization", e);
        }
    }
}
