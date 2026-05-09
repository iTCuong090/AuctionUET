package com.auctionuet.client.view;

import javafx.scene.Scene;

/**
 * Singleton quản lý trạng thái Light/Dark theme toàn cục.
 * Khi chuyển scene, theme sẽ được giữ nguyên.
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static ThemeManager instance;
    private Theme currentTheme = Theme.LIGHT; // Mặc định là Light Mode

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
    }

    public boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public void toggleTheme() {
        currentTheme = isDarkMode() ? Theme.LIGHT : Theme.DARK;
    }

    /**
     * Trả về đường dẫn resource của file CSS theme tương ứng.
     */
    public String getThemeCssPath() {
        return isDarkMode() ? "/css/dark-theme.css" : "/css/light-theme.css";
    }

    /**
     * Áp dụng theme hiện tại lên Scene.
     * Xoá theme cũ (nếu có) rồi thêm theme mới.
     */
    public void applyTheme(Scene scene) {
        if (scene == null) return;
        
        // 1. Đảm bảo luôn có styles.css ở cấp Scene (nếu chưa có)
        String baseCss = getClass().getResource("/css/styles.css").toExternalForm();
        if (!scene.getStylesheets().contains(baseCss)) {
            scene.getStylesheets().add(0, baseCss); // Cho vào đầu để theme có thể override
        }

        // 2. Xoá file theme cũ
        scene.getStylesheets().removeIf(s -> s.contains("dark-theme") || s.contains("light-theme"));
        
        // 3. Thêm file theme mới
        String themeCss = getClass().getResource(getThemeCssPath()).toExternalForm();
        scene.getStylesheets().add(themeCss);
    }
}
