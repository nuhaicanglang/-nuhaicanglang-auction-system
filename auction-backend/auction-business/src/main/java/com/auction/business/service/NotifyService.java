package com.auction.business.service;

import com.auction.business.dto.NotifyCreateDTO;

public interface NotifyService {

    void sendInApp(NotifyCreateDTO dto);

    void sendEmail(NotifyCreateDTO dto);
}
