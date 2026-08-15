package com.example.backend.portal.service.impl;

import com.example.backend.portal.model.Event;
import com.example.backend.portal.mapper.EventMapper;
import com.example.backend.portal.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;

    @Override
    public List<Event> listAll(int page, int size, String type, String search) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        size = Math.min(size, 100);
        int offset = (page - 1) * size;
        return eventMapper.list(offset, size, type, search);
    }

    @Override
    public long count(String type, String search) {
        return eventMapper.count(type, search);
    }

    @Override
    public List<Event> latest(int limit) {
        if (limit <= 0) limit = 2;
        return eventMapper.latest(limit);
    }

    @Override
    public Event getById(Long id, boolean increaseView) {
        if (increaseView) {
            eventMapper.incrementViewCount(id);
        }
        return eventMapper.findById(id);
    }
}