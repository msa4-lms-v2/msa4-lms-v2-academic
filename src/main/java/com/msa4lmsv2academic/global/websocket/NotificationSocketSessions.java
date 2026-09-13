package com.msa4lmsv2academic.global.websocket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * spring.threads.virtual.enabled=true 환경에서 Spring {@code @Scheduled}가 배포 pod에서 실행되지
 * 않는 현상이 확인돼(OutboxWorker 참고), 별도 ScheduledExecutorService로 직접 폴링한다.
 */
@Slf4j
@Component
public class NotificationSocketSessions {
    private final ConcurrentHashMap<String, Entry> connections = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "notification-socket-session-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::runCloseExpired, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    public void open(WebSocketSession session) {
        connections.put(session.getId(), new Entry(session, Instant.now().plusSeconds(10), false));
    }
    public void remove(String id) { connections.remove(id); }
    public void authenticate(String id, Instant expiry) {
        connections.computeIfPresent(id, (key, old) -> new Entry(old.session(), expiry, true));
    }
    public boolean isAuthenticated(String id) {
        var entry = connections.get(id);
        return entry != null && entry.authenticated() && Instant.now().isBefore(entry.deadline());
    }
    private void runCloseExpired() {
        try {
            closeExpired();
        } catch (Exception exception) {
            log.error("만료된 웹소켓 세션 정리 중 예상치 못한 예외 발생", exception);
        }
    }

    public void closeExpired() {
        connections.forEach((id, entry) -> {
            if (!Instant.now().isBefore(entry.deadline())) {
                try { entry.session().close(CloseStatus.POLICY_VIOLATION); }
                catch (java.io.IOException ignored) { /* Transport already closed. */ }
                finally { connections.remove(id, entry); }
            }
        });
    }
    private record Entry(WebSocketSession session, Instant deadline, boolean authenticated) {}
}
