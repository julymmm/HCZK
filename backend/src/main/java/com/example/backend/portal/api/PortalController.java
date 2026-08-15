package com.example.backend.portal.api;

import com.example.backend.portal.model.Event;
import com.example.backend.portal.service.AnnouncementService;
import com.example.backend.portal.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final AnnouncementService announcementService;
    private final EventService eventService;

    /** 事件详情。 */
    @GetMapping("/events/{id}")
    public ResponseEntity<?> eventDetail(@PathVariable Long id) {
        Event event = eventService.getById(id, true);
        if (event == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "Unauthorized"));
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "OK");
        resp.put("data", event);
        return ResponseEntity.ok(resp);
    }
}
