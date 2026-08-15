package com.example.backend.qna.service.impl;

import com.example.backend.qna.model.PointLog;
import com.example.backend.qna.mapper.PointMapper;
import com.example.backend.qna.service.PointService;
import org.springframework.stereotype.Service;

@Service
public class PointServiceImpl implements PointService {

    private final PointMapper pointMapper;

    public PointServiceImpl(PointMapper pointMapper) {
        this.pointMapper = pointMapper;
    }

    @Override
    public int getBalance(Long userId) {
        Integer b = pointMapper.getBalance(userId);
        return b != null ? b : 0;
    }

    @Override
    public void addPoints(Long userId, int amount, String reason, String refType, Long refId) {
        if (amount == 0) return;
        pointMapper.addBalance(userId, amount);
        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setAmount(amount);
        log.setReason(reason);
        log.setRefType(refType);
        log.setRefId(refId);
        pointMapper.insertLog(log);
    }
}
