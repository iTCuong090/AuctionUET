package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.persistence.schema.SystemSettingSchema;
import com.auctionuet.server.util.json.JsonFileHelper;

import java.time.LocalDateTime;
import java.util.List;

public class SystemSettingDAO {
    private static final String SETTINGS_ID = "system-settings";
    private final String filePath;

    public SystemSettingDAO() {
        this("data/system_settings.json");
    }

    public SystemSettingDAO(String filePath) {
        this.filePath = filePath;
    }

    public boolean isItemApprovalEnabled() {
        List<SystemSettingSchema> settings = JsonFileHelper.readList(filePath, SystemSettingSchema.class);
        return settings.isEmpty() || settings.get(0).isItemApprovalEnabled();
    }

    public void setItemApprovalEnabled(boolean enabled) {
        List<SystemSettingSchema> settings = JsonFileHelper.readList(filePath, SystemSettingSchema.class);
        LocalDateTime now = LocalDateTime.now();
        if (settings.isEmpty()) {
            settings.add(new SystemSettingSchema(SETTINGS_ID, now, now, enabled));
        } else {
            SystemSettingSchema schema = settings.get(0);
            schema.setItemApprovalEnabled(enabled);
            schema.setUpdatedAt(now);
        }
        JsonFileHelper.writeList(filePath, settings);
    }
}
