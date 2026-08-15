package com.example.backend.competition.service.impl;

import com.example.backend.common.response.PageResponse;
import com.example.backend.competition.model.Competition;
import com.example.backend.competition.mapper.CompetitionMapper;
import com.example.backend.competition.service.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionMapper competitionMapper;

    @Override
    public PageResponse<Competition> list(int page, int size, String status, String search) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 1000);
        int offset = (page - 1) * size;
        List<Competition> data = competitionMapper.list(offset, size, status, search);
        long total = competitionMapper.count(status, search);
        return new PageResponse<>(data, total, page, size);
    }

    @Override
    public Competition getById(Long id, boolean increaseView) {
        if (increaseView) {
            competitionMapper.incrementViewCount(id);
        }
        return competitionMapper.findById(id);
    }
}