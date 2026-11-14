package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.Review;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品浏览（无需登录）
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public Result<PageResult<Product>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer categoryId,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(productService.page(keyword, categoryId, sort, page, size));
    }

    @GetMapping("/recommend")
    public Result<List<Product>> recommend() {
        return Result.ok(productService.recommend());
    }

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(productService.categories());
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @GetMapping("/{id}/reviews")
    public Result<List<Review>> reviews(@PathVariable Long id) {
        return Result.ok(reviewService.productReviews(id));
    }
}
