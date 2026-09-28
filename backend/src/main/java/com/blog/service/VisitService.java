package com.blog.service;

import com.blog.entity.VisitLog;
import com.blog.mapper.DailyStatMapper;
import com.blog.mapper.VisitLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitLogMapper visitLogMapper;
    private final DailyStatMapper dailyStatMapper;

    public void track(String path, String ip, String ua) {
        VisitLog log = new VisitLog();
        log.setPath(path == null ? "/" : path);
        log.setIpMask(maskIp(ip));
        log.setUserAgent(ua != null && ua.length() > 255 ? ua.substring(0, 255) : ua);
        log.setVisitDate(LocalDate.now());
        visitLogMapper.insert(log);
        dailyStatMapper.incrPv(LocalDate.now());
    }

    private String maskIp(String ip) {
        if (ip == null) {
            return "unknown";
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        return ip;
    }
}
