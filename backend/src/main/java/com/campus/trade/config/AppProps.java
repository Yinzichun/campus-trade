package com.campus.trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 业务参数集中配置，对应 application.yml 中的 app.* 节点
 */
@Component
public class AppProps {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.fee-rates}")
    private String feeRates;

    @Value("${app.auto-confirm-days:7}")
    private int autoConfirmDays;

    @Value("${app.return-hours:24}")
    private int returnHours;

    public String getUploadDir() {
        return uploadDir;
    }

    public int getAutoConfirmDays() {
        return autoConfirmDays;
    }

    public int getReturnHours() {
        return returnHours;
    }

    /**
     * 按商家等级取平台手续费率：1级0.1% ... 5级1%
     */
    public BigDecimal feeRate(Integer level) {
        String[] arr = feeRates.split(",");
        int idx = (level == null ? 1 : level) - 1;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= arr.length) {
            idx = arr.length - 1;
        }
        return new BigDecimal(arr[idx].trim());
    }
}
