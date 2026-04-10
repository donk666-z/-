package com.campus.delivery.controller;

import com.campus.delivery.common.Result;
import com.campus.delivery.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadImage(@RequestPart("file") MultipartFile file, HttpServletRequest request) throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请先选择图片");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String contentType = file.getContentType();
        boolean isImageContentType = StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        if (!isImageContentType && !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "仅支持上传图片文件");
        }

        File uploadDir = resolveDishUploadDir();
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("创建上传目录失败");
        }

        String fileName = UUID.randomUUID() + extension;
        File targetFile = new File(uploadDir, fileName).getAbsoluteFile();
        file.transferTo(targetFile);

        String relativePath = "/uploads/dish/" + fileName;
        String fileUrl = buildFileUrl(request, relativePath);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("url", fileUrl);
        payload.put("path", relativePath);
        return Result.success("上传成功", payload);
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private File resolveDishUploadDir() {
        File workingDir = new File(System.getProperty("user.dir"));
        return new File(new File(workingDir, "uploads"), "dish").getAbsoluteFile();
    }

    private String buildFileUrl(HttpServletRequest request, String relativePath) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme()).append("://").append(request.getServerName());
        if (!isDefaultPort(request.getScheme(), request.getServerPort())) {
            builder.append(":").append(request.getServerPort());
        }
        builder.append(request.getContextPath()).append(relativePath);
        return builder.toString();
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
    }
}
