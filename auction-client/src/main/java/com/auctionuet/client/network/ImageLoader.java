package com.auctionuet.client.network;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Bộ quản lý tải ảnh và phóng to thu nhỏ (Image Viewer/Loader) của AuctionUET.
 * Hỗ trợ tải ảnh ngầm, tự động ẩn/hiển thị label placeholder và cung cấp cửa sổ xem chi tiết ảnh có khả năng zoom/pan.
 */
public class ImageLoader {

    /**
     * Tải hình ảnh an toàn bất đồng bộ và tự động ẩn/hiện placeholder label.
     *
     * @param imageView Khung ImageView hiển thị ảnh
     * @param placeholderLabel Label hiển thị chữ placeholder (như "Chưa có ảnh" hoặc "[Ảnh sản phẩm]")
     * @param imageUrl Đường dẫn URL của hình ảnh
     */
    public static void loadImage(ImageView imageView, Label placeholderLabel, String imageUrl) {
        if (imageView == null) return;

        // Nếu URL trống hoặc bị lỗi, hiển thị lại placeholder và xóa ảnh hiện tại
        if (imageUrl == null || imageUrl.isBlank()) {
            imageView.setImage(null);
            if (placeholderLabel != null) {
                placeholderLabel.setVisible(true);
                placeholderLabel.setManaged(true);
            }
            return;
        }

        try {
            // Khởi tạo đối tượng Image tải ngầm (backgroundLoading = true) để tránh nghẽn UI
            Image image = new Image(imageUrl, true);

            // Theo dõi tiến trình tải ảnh để ẩn label placeholder khi tải xong thành công
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() == 1.0 && !image.isError()) {
                    Platform.runLater(() -> {
                        if (placeholderLabel != null) {
                            placeholderLabel.setVisible(false);
                            placeholderLabel.setManaged(false);
                        }
                    });
                }
            });

            // Lắng nghe lỗi trong quá trình tải ảnh (ví dụ: mất mạng, link chết)
            image.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> {
                        imageView.setImage(null);
                        if (placeholderLabel != null) {
                            placeholderLabel.setVisible(true);
                            placeholderLabel.setManaged(true);
                        }
                    });
                }
            });

            // Gán ảnh ngay lập tức
            imageView.setImage(image);

            // Bật sự kiện click để phóng to hình ảnh
            setupClickToZoom(imageView, imageUrl);

        } catch (Exception e) {
            // Bất kỳ ngoại lệ nào phát sinh (như URL sai định dạng) cũng sẽ được xử lý an toàn
            imageView.setImage(null);
            if (placeholderLabel != null) {
                placeholderLabel.setVisible(true);
                placeholderLabel.setManaged(true);
            }
        }
    }

    /**
     * Đăng ký sự kiện click chuột trái vào ImageView để mở popup phóng to.
     */
    public static void setupClickToZoom(ImageView imageView, String imageUrl) {
        if (imageView == null || imageUrl == null || imageUrl.isBlank()) return;

        imageView.setCursor(Cursor.HAND);
        imageView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                showZoomableImagePopup(imageUrl, imageView.getScene().getWindow());
            }
        });
    }

    /**
     * Hiển thị một cửa sổ modal (popup) mới cho phép xem ảnh to hơn, zoom bằng cuộn chuột, drag để di chuyển (panning).
     */
    private static void showZoomableImagePopup(String imageUrl, javafx.stage.Window owner) {
        try {
            Stage popupStage = new Stage();
            popupStage.setTitle("Chi tiết hình ảnh");
            popupStage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                popupStage.initOwner(owner);
            }

            // Tạo ImageView hiển thị ảnh lớn chất lượng cao
            ImageView largeImageView = new ImageView();
            largeImageView.setPreserveRatio(true);
            largeImageView.setSmooth(true);

            // Tải ảnh chất lượng gốc
            Image largeImage = new Image(imageUrl, true);
            largeImageView.setImage(largeImage);

            // Bọc ImageView trong một Group để việc thu phóng (scaling) hoạt động mượt mà
            Group imageGroup = new Group(largeImageView);
            StackPane centeringPane = new StackPane(imageGroup);
            centeringPane.setAlignment(Pos.CENTER);
            centeringPane.setStyle("-fx-background-color: #1a1a2e;");

            // ScrollPane cho phép kéo thả di chuyển góc nhìn (Panning) khi zoom lớn
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setPannable(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setContent(centeringPane);
            scrollPane.setStyle("-fx-background: #1a1a2e; -fx-border-color: transparent;");

            // Tự động điều chỉnh kích thước ImageView theo kích thước cửa sổ hiển thị ban đầu
            largeImage.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0 && !largeImage.isError()) {
                    Platform.runLater(() -> {
                        double width = largeImage.getWidth();
                        double height = largeImage.getHeight();
                        if (width > 800 || height > 600) {
                            largeImageView.setFitWidth(800);
                            largeImageView.setFitHeight(600);
                        } else {
                            largeImageView.setFitWidth(width);
                            largeImageView.setFitHeight(height);
                        }
                    });
                }
            });

            // Tích hợp tính năng cuộn chuột để zoom ảnh
            centeringPane.setOnScroll(event -> {
                double zoomFactor = 1.1;
                if (event.getDeltaY() < 0) {
                    zoomFactor = 0.9;
                }

                double newScaleX = largeImageView.getScaleX() * zoomFactor;
                double newScaleY = largeImageView.getScaleY() * zoomFactor;

                // Giới hạn zoom từ 0.3x đến 8.0x
                if (newScaleX >= 0.3 && newScaleX <= 8.0) {
                    largeImageView.setScaleX(newScaleX);
                    largeImageView.setScaleY(newScaleY);
                }
                event.consume();
            });

            // Double click để đặt lại zoom về mặc định
            centeringPane.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    largeImageView.setScaleX(1.0);
                    largeImageView.setScaleY(1.0);
                }
            });

            // Thanh công cụ điều khiển
            HBox toolbar = new HBox(15);
            toolbar.setAlignment(Pos.CENTER);
            toolbar.setPadding(new Insets(12));
            toolbar.setStyle("-fx-background-color: #0f3460;");

            Button btnZoomIn = new Button("Phóng to 🔍+");
            btnZoomIn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnZoomIn.setOnAction(e -> {
                double newScaleX = largeImageView.getScaleX() * 1.25;
                double newScaleY = largeImageView.getScaleY() * 1.25;
                if (newScaleX <= 8.0) {
                    largeImageView.setScaleX(newScaleX);
                    largeImageView.setScaleY(newScaleY);
                }
            });

            Button btnZoomOut = new Button("Thu nhỏ 🔍-");
            btnZoomOut.setStyle("-fx-background-color: #162447; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnZoomOut.setOnAction(e -> {
                double newScaleX = largeImageView.getScaleX() * 0.8;
                double newScaleY = largeImageView.getScaleY() * 0.8;
                if (newScaleX >= 0.3) {
                    largeImageView.setScaleX(newScaleX);
                    largeImageView.setScaleY(newScaleY);
                }
            });

            Button btnReset = new Button("Mặc định 🔄");
            btnReset.setStyle("-fx-background-color: #1f4068; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnReset.setOnAction(e -> {
                largeImageView.setScaleX(1.0);
                largeImageView.setScaleY(1.0);
            });

            Button btnClose = new Button("Đóng ❌");
            btnClose.setStyle("-fx-background-color: #ff4757; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnClose.setOnAction(e -> popupStage.close());

            toolbar.getChildren().addAll(btnZoomIn, btnZoomOut, btnReset, btnClose);

            BorderPane layout = new BorderPane();
            layout.setCenter(scrollPane);
            layout.setBottom(toolbar);

            Scene scene = new Scene(layout, 850, 680);
            popupStage.setScene(scene);
            popupStage.show();

        } catch (Exception e) {
            System.err.println("Không thể mở cửa sổ phóng to hình ảnh: " + e.getMessage());
        }
    }
}
