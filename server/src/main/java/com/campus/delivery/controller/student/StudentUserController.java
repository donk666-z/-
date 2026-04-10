package com.campus.delivery.controller.student;

import com.campus.delivery.common.Result;
import com.campus.delivery.dto.AddressSaveResult;
import com.campus.delivery.entity.Address;
import com.campus.delivery.entity.User;
import com.campus.delivery.exception.BusinessException;
import com.campus.delivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/student/user")
public class StudentUserController {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public Result<User> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getSafeProfile(userId);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody User user, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User updatedUser = userService.updateStudentProfile(userId, user);
        return Result.success("更新成功", updatedUser);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请先选择头像图片");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String contentType = file.getContentType();
        boolean isImageContentType = StringUtils.hasText(contentType) && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        if (!isImageContentType && !ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "仅支持上传图片文件");
        }

        File avatarDir = resolveAvatarUploadDir();
        if (!avatarDir.exists() && !avatarDir.mkdirs()) {
            throw new IOException("创建头像目录失败");
        }

        String fileName = UUID.randomUUID() + extension;
        File targetFile = new File(avatarDir, fileName).getAbsoluteFile();
        file.transferTo(targetFile);

        String relativePath = "/uploads/avatar/" + fileName;
        return Result.success("上传成功", buildFileUrl(request, relativePath));
    }

    @GetMapping("/addresses")
    public Result<List<Address>> getAddresses(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Address> addresses = userService.getAddresses(userId);
        return Result.success(addresses);
    }

    @PostMapping("/addresses")
    public Result<Address> addAddress(@RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(401, "未授权，请先登录");
        }
        address.setUserId(userId);
        AddressSaveResult result = userService.addAddress(address);
        return Result.success(buildAddressSaveMessage("地址添加成功", result), result.getAddress());
    }

    @PutMapping("/addresses/{id}")
    public Result<Address> updateAddress(@PathVariable Long id, @RequestBody Address address, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        address.setId(id);
        address.setUserId(userId);
        AddressSaveResult result = userService.updateAddress(address);
        return Result.success(buildAddressSaveMessage("地址更新成功", result), result.getAddress());
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.deleteAddress(id, userId);
        return Result.success("地址删除成功", null);
    }

    @PutMapping("/addresses/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.setDefaultAddress(userId, id);
        return Result.success("已设为默认地址", null);
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private File resolveAvatarUploadDir() {
        File workingDir = new File(System.getProperty("user.dir"));
        return new File(new File(workingDir, "uploads"), "avatar").getAbsoluteFile();
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

    private String buildAddressSaveMessage(String successMessage, AddressSaveResult result) {
        if (result != null && result.isGeocodeAttempted() && !result.isGeocodeResolved()) {
            return successMessage + "，但未能解析出经纬度";
        }
        return successMessage;
    }
}
