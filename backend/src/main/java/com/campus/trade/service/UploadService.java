package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.config.AppProps;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private static final List<String> ALLOW = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    private final AppProps appProps;

    public UploadService(AppProps appProps) {
        this.appProps = appProps;
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOW.contains(ext)) {
            throw new BizException("只支持 jpg / png / gif / webp 格式的图片");
        }
        File dir = new File(appProps.getUploadDir()).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("上传目录创建失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (Exception e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        return "/uploads/" + fileName;
    }
}
