package com.att.tdp.issueflow.scheduler;

import com.att.tdp.issueflow.service.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EscalationScheduler {

    private final EscalationService escalationService;

    @Scheduled(fixedRateString = "${issueflow.escalation.rate}")
    public void runEscalation() {
        log.debug("Running ticket escalation check...");
        escalationService.escalateOverdueTickets();
    }
}
