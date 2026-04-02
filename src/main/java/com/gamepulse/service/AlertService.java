package com.gamepulse.service;

import com.gamepulse.domain.alert.AlertRepository;
import com.gamepulse.domain.alert.PriceAlert;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<PriceAlert> getAlertsByEmail(String email) {
        return alertRepository.findByEmail(email);
    }
}