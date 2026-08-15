package com.example.backend.competition.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.competition.model.Competition;

public interface CompetitionService {
    PageResponse<Competition> list(int page, int size, String status, String search);
    Competition getById(Long id, boolean increaseView);
}