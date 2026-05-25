package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.protocol.dto.response.user.UserDTO;

import java.util.function.Consumer;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private StackPane contentArea;

    // Các nút trên Sidebar
    @FXML private Button btnHome, btnMyItems, btnCreateAuction, btnAuctionList, btnPayments, btnProfile, btnLogout, btnWallet;
    @FXML private Button btnAdminUsers, btnAdminItems;
    @FXML private Button btnToggleTheme;
    private final Consumer<UserDTO> userChangeListener = user ->
            Platform.runLater(() -> renderUserHeader(user));

    @FXML
    public void initialize() {
        ClientSession.getInstance().addUserChangeListener(userChangeListener);
        renderUserHeader(ClientSession.getInstance().getCurrentUser());

        // 2. Phân quyền: Ẩn các nút dựa trên Role (Test D.1, D.2)
        setupPermissions();

        // 3. Tài khoản dùng mật khẩu mặc định phải đổi mật khẩu trước.
        loadView(ClientSession.getInstance().isMustChangePassword()
                ? "/fxml/ProfileView.fxml"
                : "/fxml/AuctionListView.fxml");

        // 4. Gắn sự kiện cho các nút (Bấm vào đâu thì sáng nút đó)
        btnHome.setOnAction(e -> {
            loadView("/fxml/HomeView.fxml");
            setActiveButton(btnHome);
        });

        btnMyItems.setOnAction(e -> {
            loadView("/fxml/MyItemsView.fxml");
            setActiveButton(btnMyItems);
        });

        btnCreateAuction.setOnAction(e -> {
            loadView("/fxml/CreateAuctionView.fxml");
            setActiveButton(btnCreateAuction);
        });

        btnAuctionList.setOnAction(e -> {
            loadView("/fxml/AuctionListView.fxml");
            setActiveButton(btnAuctionList);
        });

        if (btnPayments != null) {
            btnPayments.setOnAction(e -> {
                loadView("/fxml/PaymentView.fxml");
                setActiveButton(btnPayments);
            });
        }

        if (btnWallet != null) {
            btnWallet.setOnAction(e -> {
                loadView("/fxml/WalletView.fxml");
                setActiveButton(btnWallet);
            });
        }

        if (btnAdminUsers != null) {
            btnAdminUsers.setOnAction(e -> {
                loadView("/fxml/AdminUsersView.fxml");
                setActiveButton(btnAdminUsers);
            });
        }
        if (btnAdminItems != null) {
            btnAdminItems.setOnAction(e -> {
                loadView("/fxml/AdminItemsView.fxml");
                setActiveButton(btnAdminItems);
            });
        }

        btnProfile.setOnAction(e -> {
            loadView("/fxml/ProfileView.fxml");
            setActiveButton(btnProfile);
        });

        btnLogout.setOnAction(e -> handleLogout());

        setActiveButton(ClientSession.getInstance().isMustChangePassword() ? btnProfile : btnAuctionList);

        // 5. Cập nhật icon nút toggle theme theo trạng thái hiện tại
        if (btnToggleTheme != null) {
            btnToggleTheme.setText(ThemeManager.getInstance().isDarkMode() ? "T" : "S");
        }
    }

    private void renderUserHeader(UserDTO currentUser) {
        if (currentUser != null) {
            String name = currentUser.getUsername();
            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "UNKNOWN";
            userNameLabel.setText("◎ " + name + " (" + roleName + ")");
        } else {
            userNameLabel.setText("◎ Guest");
        }
    }

    private void setupPermissions() {
        // Sử dụng hàm isSeller() siêu tiện lợi trong ClientSession
        boolean isSeller = ClientSession.getInstance().isSeller();
        boolean isBidder = ClientSession.getInstance().isBidder();
        boolean isAdmin = ClientSession.getInstance().isAdmin();

        if (ClientSession.getInstance().isMustChangePassword()) {
            hideButton(btnHome);
            hideButton(btnMyItems);
            hideButton(btnCreateAuction);
            hideButton(btnAuctionList);
            hideButton(btnPayments);
            hideButton(btnWallet);
            hideButton(btnAdminUsers);
            hideButton(btnAdminItems);
            return;
        }

        if (!isSeller) {
            // Nếu KHÔNG phải Seller (tức là BIDDER hoặc GUEST) -> Ẩn các nút tạo hàng
            hideButton(btnCreateAuction);
        }
        if (!isBidder) {
            hideButton(btnPayments);
        }
        if (!isAdmin) {
            hideButton(btnAdminUsers);
            hideButton(btnAdminItems);
        } else {
            hideButton(btnMyItems);
            hideButton(btnWallet);
        }
        // Nếu là Seller -> Các nút vẫn hiển thị bình thường (do FXML mặc định là visible)
    }

    // Tuyệt chiêu ẩn nút mà không để lại khoảng trống
    private void hideButton(Button btn) {
        if (btn != null) {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    // Hàm "thần thánh" để đổi ruột màn hình
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            System.err.println("Lỗi load màn hình: " + fxmlPath);
            e.printStackTrace();
            Label errorLabel = new Label("Không thể mở màn hình này: " + e.getMessage());
            errorLabel.getStyleClass().add("error-label");
            errorLabel.setWrapText(true);
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    // Tuyệt chiêu đổi màu nút Sidebar
    private void setActiveButton(Button clickedButton) {
        // Lột sạch áo "active" của tất cả các nút
        if (btnHome != null) btnHome.getStyleClass().remove("active");
        if (btnMyItems != null) btnMyItems.getStyleClass().remove("active");
        if (btnCreateAuction != null) btnCreateAuction.getStyleClass().remove("active");
        if (btnAuctionList != null) btnAuctionList.getStyleClass().remove("active");
        if (btnPayments != null) btnPayments.getStyleClass().remove("active");
        if (btnWallet != null) btnWallet.getStyleClass().remove("active");
        if (btnProfile != null) btnProfile.getStyleClass().remove("active");
        if (btnAdminUsers != null) btnAdminUsers.getStyleClass().remove("active");
        if (btnAdminItems != null) btnAdminItems.getStyleClass().remove("active");
        // Khoác áo "active" cho cái nút vừa được bấm
        if (clickedButton != null && !clickedButton.getStyleClass().contains("active")) {
            clickedButton.getStyleClass().add("active");
        }
    }

    private void handleLogout() {
        // Dọn dẹp session bằng hàm mới clearSession() thay vì setSession()
        ClientSession.getInstance().clearSession();
        // Quay về trang chủ MainView
        SceneManager.getInstance().switchScene("/fxml/MainView.fxml");
    }

    @FXML
    private void handleToggleTheme() {
        ThemeManager.getInstance().toggleTheme();
        ThemeManager.getInstance().applyTheme(btnToggleTheme.getScene());
        btnToggleTheme.setText(ThemeManager.getInstance().isDarkMode() ? "T" : "S");
    }
}
