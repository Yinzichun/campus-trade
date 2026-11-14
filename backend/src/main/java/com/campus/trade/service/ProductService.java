package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.ProductDTO;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    public ProductService(ProductMapper productMapper, UserMapper userMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 商品搜索：支持关键字、分类、排序（价格/销量/好评率）
     */
    public PageResult<Product> page(String keyword, Integer categoryId, String sort, int pageNo, int size) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, "ON_SALE");
        if (StringUtils.hasText(keyword)) {
            qw.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            qw.eq(Product::getCategoryId, categoryId);
        }
        if ("price_asc".equals(sort)) {
            qw.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            qw.orderByDesc(Product::getPrice);
        } else if ("sales_desc".equals(sort)) {
            qw.orderByDesc(Product::getSales);
        } else if ("rating_desc".equals(sort)) {
            qw.orderByDesc(Product::getRatingAvg);
        } else {
            qw.orderByDesc(Product::getCreatedAt);
        }
        Page<Product> p = productMapper.selectPage(new Page<>(pageNo, size), qw);
        enrich(p.getRecords());
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    public Product detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("商品不存在");
        }
        enrich(List.of(product));
        return product;
    }

    public List<Product> recommend() {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "ON_SALE")
                .orderByDesc(Product::getSales)
                .last("limit 6");
        List<Product> list = productMapper.selectList(qw);
        enrich(list);
        return list;
    }

    public List<Category> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public List<Product> merchantProducts(Long merchantId) {
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .orderByDesc(Product::getCreatedAt));
        enrich(list);
        return list;
    }

    /**
     * 商家发布商品，进入待审核状态
     */
    public Product create(ProductDTO dto) {
        User merchant = UserContext.get();
        if (merchant == null || !"MERCHANT".equals(merchant.getRole())) {
            throw new BizException(403, "只有商家可以发布商品");
        }
        if (merchant.getBannedUntil() != null && merchant.getBannedUntil().isAfter(LocalDateTime.now())) {
            throw new BizException("店铺处于限时封禁中，暂不能发布商品");
        }
        validate(dto);
        Product p = new Product();
        copy(dto, p);
        p.setMerchantId(merchant.getId());
        p.setStatus("PENDING");
        p.setSales(0);
        p.setRatingAvg(new BigDecimal("5.00"));
        p.setRatingCount(0);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(p);
        return p;
    }

    public Product update(ProductDTO dto) {
        Product p = productMapper.selectById(dto.getId());
        if (p == null) {
            throw new BizException("商品不存在");
        }
        if (!p.getMerchantId().equals(UserContext.userId())) {
            throw new BizException(403, "只能修改自己店铺的商品");
        }
        validate(dto);
        copy(dto, p);
        p.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(p);
        return p;
    }

    public void changeStatus(Long id, String status) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException("商品不存在");
        }
        if (!p.getMerchantId().equals(UserContext.userId())) {
            throw new BizException(403, "只能操作自己店铺的商品");
        }
        p.setStatus(status);
        p.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(p);
    }

    /**
     * 管理员审核商品
     */
    public void audit(Long id, boolean pass) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException("商品不存在");
        }
        p.setStatus(pass ? "ON_SALE" : "OFF_SHELF");
        p.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(p);
    }

    /**
     * 店铺页：商家信息 + 商品 + 统计
     */
    public Map<String, Object> store(Long merchantId) {
        User merchant = userMapper.selectById(merchantId);
        if (merchant == null || !"MERCHANT".equals(merchant.getRole())) {
            throw new BizException("店铺不存在");
        }
        merchant.setPassword(null);
        List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .in(Product::getStatus, Arrays.asList("ON_SALE", "SOLD"))
                .orderByDesc(Product::getCreatedAt));
        enrich(list);
        int totalSales = list.stream().mapToInt(p -> p.getSales() == null ? 0 : p.getSales()).sum();
        Map<String, Object> data = new HashMap<>();
        data.put("merchant", merchant);
        data.put("products", list);
        data.put("totalSales", totalSales);
        data.put("productCount", list.size());
        return data;
    }

    public void enrich(List<Product> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> merchantIds = list.stream().map(Product::getMerchantId).distinct().collect(Collectors.toList());
        Map<Long, User> merchantMap = userMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Integer, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        for (Product p : list) {
            User m = merchantMap.get(p.getMerchantId());
            if (m != null) {
                p.setShopName(m.getShopName());
                p.setMerchantName(m.getName());
            }
            p.setCategoryName(categoryMap.get(p.getCategoryId()));
            p.setImageList(StringUtils.hasText(p.getImages())
                    ? new ArrayList<>(Arrays.asList(p.getImages().split(",")))
                    : new ArrayList<>());
        }
    }

    private void validate(ProductDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BizException("请填写商品名称");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("价格必须大于 0");
        }
        if (dto.getStock() == null || dto.getStock() < 1) {
            throw new BizException("库存至少为 1");
        }
        if (dto.getImages() == null || dto.getImages().isEmpty()) {
            throw new BizException("至少上传一张商品图片");
        }
    }

    private void copy(ProductDTO dto, Product p) {
        p.setCategoryId(dto.getCategoryId());
        p.setName(dto.getName());
        p.setPrice(dto.getPrice());
        p.setOriginalPrice(dto.getOriginalPrice());
        p.setSize(dto.getSize());
        p.setDescription(dto.getDescription());
        p.setImages(String.join(",", dto.getImages()));
        p.setNegotiable(dto.getNegotiable() == null ? 0 : dto.getNegotiable());
        p.setStock(dto.getStock());
        p.setConditionLevel(dto.getConditionLevel());
    }
}
