package com.auction.business.service.impl;

import com.auction.business.dto.NotifyCreateDTO;
import com.auction.business.service.NotifyChannelStrategy;
import com.auction.business.service.NotifyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotifyServiceImpl implements NotifyService {

    private final Map<String, NotifyChannelStrategy> strategyMap;

    public NotifyServiceImpl(List<NotifyChannelStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotifyChannelStrategy::channel, Function.identity()));
    }

    @Override
    public void sendInApp(NotifyCreateDTO dto) {
        send("IN_APP", dto);
    }

    @Override
    public void sendEmail(NotifyCreateDTO dto) {
        send("EMAIL", dto);
    }

    private void send(String channel, NotifyCreateDTO dto) {
        NotifyChannelStrategy strategy = strategyMap.get(channel);
        if (strategy != null) {
            strategy.send(dto);
        }
    }
}
