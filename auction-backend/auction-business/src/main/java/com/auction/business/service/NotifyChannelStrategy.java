package com.auction.business.service;

import com.auction.business.dto.NotifyCreateDTO;

public interface NotifyChannelStrategy {

    String channel();

    void send(NotifyCreateDTO dto);
}
