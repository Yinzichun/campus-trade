package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.entity.WalletTxn;
import com.campus.trade.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public Result<Map<String, Object>> info() {
        return Result.ok(walletService.info(UserContext.userId()));
    }

    @GetMapping("/txns")
    public Result<List<WalletTxn>> txns() {
        return Result.ok(walletService.txns(UserContext.userId()));
    }
}
