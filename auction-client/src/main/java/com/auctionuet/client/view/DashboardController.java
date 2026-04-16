package com.auctionuet.client.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import com.auctionuet.client.model.ClientSession;

import java.io.IOException;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private StackPane contentArea;

    // Các nút trên Sidebar
    @FXML private Button btnHome, btnMyItems, btnCreateAuction, btnAuctionList, btnProfile, btnLogout;

    @FXML
    public void initialize() {
        // 1. Hiển thị tên người dùng lên Header

        String name = ClientSession.getInstance().getUsername();
        String role = ClientSession.getInstance().getRole();
        userNameLabel.setText("◎ " + name + " (" + role + ")");

        // 2. Phân quyền: Ẩn các nút không thuộc về Role
        setupPermissions(role);

        // 3. Mặc định vừa vào thì hiện màn hình Home hoặc Danh sách đấu giá
        loadView("/fxml/AuctionListView.fxml");

        // 4. Gắn sự kiện cho các nút
        btnHome.setOnAction(e -> loadView("/fxml/HomeView.fxml"));
        btnMyItems.setOnAction(e -> loadView("/fxml/MyItemsView.fxml"));
        btnCreateAuction.setOnAction(e -> loadView("/fxml/CreateAuctionView.fxml"));
        btnAuctionList.setOnAction(e -> loadView("/fxml/AuctionListView.fxml"));
        btnProfile.setOnAction(e -> loadView("/fxml/ProfileView.fxml"));
        btnLogout.setOnAction(e -> handleLogout());
        // 4. Gắn sự kiện cho các nút (Bấm vào đâu thì sáng nút đó)
        btnHome.setOnAction(e -> {
            loadView("/fxml/HomeView.fxml");
            setActiveButton(btnHome); // Sáng nút Home
        });

        btnMyItems.setOnAction(e -> {
            loadView("/fxml/MyItemsView.fxml");
            setActiveButton(btnMyItems); // Sáng nút My Items
        });

        btnCreateAuction.setOnAction(e -> {
            loadView("/fxml/CreateAuctionView.fxml");
            setActiveButton(btnCreateAuction); // Sáng nút Create
        });

        btnAuctionList.setOnAction(e -> {
            loadView("/fxml/AuctionListView.fxml");
            setActiveButton(btnAuctionList); // Sáng nút Auction List
        });

        btnProfile.setOnAction(e -> {
            loadView("/fxml/ProfileView.fxml");
            setActiveButton(btnProfile); // Sáng nút Profile
        });

        btnLogout.setOnAction(e -> handleLogout());

        // Tùy chọn: Khi vừa bật app lên, ép nó sáng sẵn nút Auction List (vì mặc định đang load màn này)
        setActiveButton(btnAuctionList);
    }


    private void setupPermissions(String role) {
        if ("BIDDER".equals(role)) {
            // Thằng đi mua thì không được đăng hàng hay tạo phiên
            hideButton(btnMyItems);
            hideButton(btnCreateAuction);
        }
        // Admin hay Seller thì cứ để hiện hết hoặc tùy chỉnh thêm ở đây
    }

    // Tuyệt chiêu ẩn nút mà không để lại khoảng trống
    private void hideButton(Button btn) {
        btn.setVisible(false);
        btn.setManaged(false);
    }

    // Hàm "thần thánh" để đổi ruột màn hình
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Xóa cái cũ, nhét cái mới vào vùng CENTER
            contentArea.getChildren().setAll(root);

        } catch (IOException e) {
            System.err.println("Lỗi load màn hình: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // Tuyệt chiêu đổi màu nút Sidebar
    private void setActiveButton(Button clickedButton) {
        // 1. Lột sạch áo "active" của tất cả các nút
        btnHome.getStyleClass().remove("active");
        btnMyItems.getStyleClass().remove("active");
        btnCreateAuction.getStyleClass().remove("active");
        btnAuctionList.getStyleClass().remove("active");
        btnProfile.getStyleClass().remove("active");

        // 2. Khoác áo "active" cho cái nút vừa được bấm (nếu nó chưa có)
        if (clickedButton != null && !clickedButton.getStyleClass().contains("active")) {
            clickedButton.getStyleClass().add("active");
        }
    }
    private void handleLogout() {
        // Xóa session và quay về màn hình đăng nhập
        ClientSession.getInstance().setSession(null, null, null);
        SceneManager.getInstance().switchScene("/fxml/LoginView.fxml");
    }
}