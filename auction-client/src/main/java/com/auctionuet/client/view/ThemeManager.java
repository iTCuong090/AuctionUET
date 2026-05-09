package com.auctionuet.client.view;

import javafx.scene.Scene;

/**
 * Singleton quản lý trạng thái Light/Dark theme toàn cục.
 * Khi chuyển scene, theme sẽ được giữ nguyên.
 */
public class ThemeManager {

    public enum Theme { DARK, LIGHT }

    private static ThemeManager instance;
    private Theme currentTheme = Theme.DARK; // Mặc định là Dark Mode

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
        // Xoá file theme cũ (giữ lại styles.css)
        scene.getStylesheets().removeIf(s -> s.contains("dark-theme") || s.contains("light-theme"));
        // Thêm file theme mới
        String cssPath = getClass().getResource(getThemeCssPath()).toExternalForm();
        scene.getStylesheets().add(cssPath);
    }
}
