package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.model.UserDTO;

import java.io.IOException;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private StackPane contentArea;

    // Các nút trên Sidebar
    @FXML private Button btnHome, btnMyItems, btnCreateAuction, btnAuctionList, btnProfile, btnLogout;

    @FXML
    public void initialize() {
        // 1. Lấy thông tin User từ Session mới
        UserDTO currentUser = ClientSession.getInstance().getCurrentUser();

        if (currentUser != null) {
            String name = currentUser.getUsername();
            // Lấy tên của Role từ Enum
            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "UNKNOWN";
            userNameLabel.setText("◎ " + name + " (" + roleName + ")");
        } else {
            userNameLabel.setText("◎ Guest");
        }

        // 2. Phân quyền: Ẩn các nút dựa trên Role (Test D.1, D.2)
        setupPermissions();

        // 3. Mặc định vừa vào thì hiện màn hình Danh sách đấu giá
        loadView("/fxml/AuctionListView.fxml");

        // 4. Gắn sự kiện cho các nút (Bấm vào đâu thì sáng nút đó)
        btnHome.setOnAction(e -> {
            loadView("/fxml/HomeView.fxml");
            setActiveButton(btnHome);
        });

        btnMyItems.setOnAction(e -> {
            loadView("/fxml/CreateItemView.fxml"); // Đổi thành CreateItemView tạm vì MyItemsView chưa làm
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

        btnProfile.setOnAction(e -> {
            loadView("/fxml/ProfileView.fxml");
            setActiveButton(btnProfile);
        });

        btnLogout.setOnAction(e -> handleLogout());

        // Ép nó sáng sẵn nút Auction List khi vừa mở lên
        setActiveButton(btnAuctionList);
    }

    private void setupPermissions() {
        // Sử dụng hàm isSeller() siêu tiện lợi trong ClientSession
        boolean isSeller = ClientSession.getInstance().isSeller();

        if (!isSeller) {
            // Nếu KHÔNG phải Seller (tức là BIDDER hoặc GUEST) -> Ẩn các nút tạo hàng
            hideButton(btnMyItems);
            hideButton(btnCreateAuction);
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
        } catch (IOException e) {
            System.err.println("Lỗi load màn hình: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Tuyệt chiêu đổi màu nút Sidebar
    private void setActiveButton(Button clickedButton) {
        // Lột sạch áo "active" của tất cả các nút
        if (btnHome != null) btnHome.getStyleClass().remove("active");
        if (btnMyItems != null) btnMyItems.getStyleClass().remove("active");
        if (btnCreateAuction != null) btnCreateAuction.getStyleClass().remove("active");
        if (btnAuctionList != null) btnAuctionList.getStyleClass().remove("active");
        if (btnProfile != null) btnProfile.getStyleClass().remove("active");

        // Khoác áo "active" cho cái nút vừa được bấm
        if (clickedButton != null && !clickedButton.getStyleClass().contains("active")) {
            clickedButton.getStyleClass().add("active");
        }
    }

    private void handleLogout() {
        // Dọn dẹp session bằng hàm mới clearSession() thay vì setSession()
        ClientSession.getInstance().clearSession();
        // Quay về đăng nhập
        SceneManager.getInstance().switchScene("/fxml/LoginView.fxml");
    }
}