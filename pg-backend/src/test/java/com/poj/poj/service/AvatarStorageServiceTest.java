package com.poj.poj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AvatarStorageServiceTest {
    @TempDir
    Path directory;

    @Test
    void savesACompressedBoundedJpegAndExposesStableUrl() throws Exception {
        BufferedImage source = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 1200, 800);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(source, "png", bytes);

        AvatarStorageService service = new AvatarStorageService(directory.toString());
        String url = service.save(new MockMultipartFile(
                "file", "avatar.png", "image/png", bytes.toByteArray()), 7L);

        assertTrue(url.matches("/api/uploads/avatar/7/[a-f0-9]{32}\\.jpg"));
        Path saved = directory.resolve(url.substring("/api/uploads/".length()));
        assertTrue(Files.exists(saved));
        BufferedImage optimized = ImageIO.read(saved.toFile());
        assertEquals(512, optimized.getWidth());
        assertEquals(341, optimized.getHeight());
        assertTrue(Files.size(saved) < bytes.size());
        assertTrue(service.getResourceLocation().endsWith("/"));
    }
}
