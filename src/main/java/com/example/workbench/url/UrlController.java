package com.example.workbench.url;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

@RestController
public class UrlController {
    private final UrlService service;
    public UrlController(UrlService service) { this.service = service; }
    private String base(HttpServletRequest request) { String scheme = request.getHeader("X-Forwarded-Proto"); if (scheme == null || scheme.isBlank()) scheme = request.getScheme(); String host = request.getHeader("X-Forwarded-Host"); if (host == null || host.isBlank()) host = request.getHeader("Host"); if (host == null || host.isBlank()) host = request.getServerName() + (request.getServerPort()==80||request.getServerPort()==443?"":":"+request.getServerPort()); return scheme + "://" + host; }
    @PostMapping("/api/urls") public UrlDtos.UrlResponse create(@Valid @RequestBody UrlDtos.CreateRequest body, HttpServletRequest request) { return service.create(body, base(request)); }
    @GetMapping("/api/urls") public Map<String,Object> all(HttpServletRequest request) { return Map.of("urls", service.all(base(request)), "metrics", service.metrics()); }
    @GetMapping("/api/urls/{code}/analytics") public UrlDtos.UrlResponse analytics(@PathVariable String code, HttpServletRequest request) { return service.analytics(code, base(request)); }
    @GetMapping("/api/url-metrics") public UrlService.UrlMetrics metrics() { return service.metrics(); }
    @GetMapping("/{code:[a-zA-Z0-9]{7}}") public void redirect(@PathVariable String code, HttpServletResponse response) throws IOException { try { response.setStatus(302); response.setHeader(HttpHeaders.LOCATION, service.resolve(code)); } catch (IllegalArgumentException e) { response.sendError(404, e.getMessage()); } catch (IllegalStateException e) { response.sendError(410, e.getMessage()); } }
}
