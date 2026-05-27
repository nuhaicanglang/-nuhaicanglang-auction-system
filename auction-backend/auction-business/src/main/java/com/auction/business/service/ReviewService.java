package com.auction.business.service;

import com.auction.business.dto.ReviewCreateDTO;
import com.auction.business.dto.ReviewQueryDTO;
import com.auction.business.vo.ReviewVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface ReviewService {

    ReviewVO createReview(Long orderId, Long userId, ReviewCreateDTO dto);

    IPage<ReviewVO> listReviews(ReviewQueryDTO query);
}
