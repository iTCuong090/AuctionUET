package com.auctionuet.server.domain.model;

import com.auctionuet.protocol.dto.response.admin.SystemMonitorDTO;

public interface SystemMonitorObserver {
    void onSystemMonitorUpdated(SystemMonitorDTO snapshot);
}
