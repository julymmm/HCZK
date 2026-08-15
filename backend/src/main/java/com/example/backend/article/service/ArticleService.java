package com.example.backend.article.service;

import com.example.backend.common.response.PageResponse;
import com.example.backend.article.model.Article;

public interface ArticleService {
    PageResponse<Article> list(int page, int size, String search, String source);
    Article getById(Long id, boolean increaseViewCount);
}



