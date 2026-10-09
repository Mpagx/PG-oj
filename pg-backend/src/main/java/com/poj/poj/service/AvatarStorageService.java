package com.poj.poj.service;

import com.poj.poj.common.ErrorCode;
import com.poj.poj.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {
    private static final long MAX_SIZE = 2 * 1024 * 1024L;
    private static final int MAX_EDGE = 512;
    private final Path uploadRoot;

    public AvatarStorageService(@Value("${app.upload.directory:uploads}") String directory) {
        this.uploadRoot = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file, long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择头像图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像不能超过 2MB");
        }
        String extension = extension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!("jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension))
                || !("image/jpeg".equals(contentType) || "image/png".equals(contentType))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像仅支持 JPG、JPEG、PNG");
        }
        Path userDirectory = uploadRoot.resolve("avatar").resolve(String.valueOf(userId)).normalize();
        ensureInsideRoot(userDirectory);
        String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        Path target = userDirectory.resolve(filename).normalize();
        ensureInsideRoot(target);
        try {
            Files.createDirectories(userDirectory);
            BufferedImage original;
            try (InputStream input = file.getInputStream()) {
                original = ImageIO.read(input);
            }
            if (original == null || original.getWidth() < 1 || original.getHeight() < 1
                    || (long) original.getWidth() * original.getHeight() > 16_000_000L) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片内容无效或尺寸过大");
            }
            BufferedImage optimized = optimize(original);
            writeJpeg(optimized, target);
            optimized.flush();
            original.flush();
            return "/api/uploads/avatar/" + userId + "/" + filename;
        } catch (BusinessException error) {
            throw error;
        } catch (IOException error) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像保存失败");
        }
    }

    public void deleteIfLocal(String avatarUrl) {
        String prefix = "/api/uploads/";
        if (avatarUrl == null || !avatarUrl.startsWith(prefix)) return;
        Path target = uploadRoot.resolve(avatarUrl.substring(prefix.length())).normalize();
        ensureInsideRoot(target);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // 新头像已经保存成功时，不因为旧文件清理失败而回滚用户操作。
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public String getResourceLocation() {
        String location = uploadRoot.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private BufferedImage optimize(BufferedImage source) {
        double scale = Math.min(1D, Math.min((double) MAX_EDGE / source.getWidth(),
                (double) MAX_EDGE / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    private void writeJpeg(BufferedImage image, Path target) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(Files.newOutputStream(target))) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(0.82F);
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(uploadRoot)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件路径");
        }
    }
}
