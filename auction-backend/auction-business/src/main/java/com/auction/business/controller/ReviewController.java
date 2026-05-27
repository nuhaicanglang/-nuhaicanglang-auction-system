package com.auction.business.controller;

import com.auction.business.dto.ReviewCreateDTO;
import com.auction.business.dto.ReviewQueryDTO;
import com.auction.business.service.ReviewService;
import com.auction.business.vo.ReviewVO;
import com.auction.common.core.Result;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/orders/{id}/review")
    public Result<ReviewVO> create(@PathVariable Long id, @Valid @RequestBody ReviewCreateDTO dto) {
        return Result.success(reviewService.createReview(id, SecurityUtils.getUserId(), dto));
    }

    @GetMapping("/reviews")
    public Result<IPage<ReviewVO>> list(ReviewQueryDTO query) {
        return Result.success(reviewService.listReviews(query));
    }
}
