package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.ItemClient;
import com.auctionuet.protocol.dto.response.auction.AuctionDTO;
import com.auctionuet.protocol.dto.response.item.ItemDTO;
import com.auctionuet.protocol.dto.response.user.UserDTO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ItemHistoryController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label nameLabel, ownerLabel, typeLabel, specLabel, conditionLabel, descriptionLabel, statusLabel;
    @FXML private ImageView productImageView;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private TableView<AuctionDTO> auctionHistoryTable;
    @FXML private TableColumn<AuctionDTO, String> titleCol, priceCol, statusCol, timeCol;

    private final ItemClient itemClient = new ItemClient();
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private ItemDTO currentItem;

    @FXML
    public void initialize() {
        setupAuctionHistoryTable();
        setupPriceChart();
    }

    public void setItem(ItemDTO item) {
        this.currentItem = item;
        updateItemInfo(item);
        loadAuctionHistory();
    }

    private void updateItemInfo(ItemDTO item) {
        if (item == null) {
            statusLabel.setText("Không tìm thấy vật phẩm.");
            return;
        }

        nameLabel.setText("Vật phẩm: " + item.getName());
        ownerLabel.setText("Chủ sở hữu: " + usernameOf(item.getSeller()));
        typeLabel.setText("Loại: " + item.getType());
        conditionLabel.setText("Tình trạng: " + valueOrEmpty(item.getCondition()));
        descriptionLabel.setText(nullToEmpty(item.getDescription()));
        specLabel.setText("Thông số: " + formatExtraFields(item.getExtraFields()));
    }

    private void loadAuctionHistory() {
        String token = ClientSession.getInstance().getToken();
        if (token == null || currentItem == null) {
            statusLabel.setText("Bạn chưa đăng nhập.");
            return;
        }

        statusLabel.setText("Đang tải lịch sử vật phẩm...");
        new Thread(() -> {
            try {
                List<AuctionDTO> history = itemClient.getItemAuctionHistory(token, currentItem.getId());
                Platform.runLater(() -> renderAuctionHistory(history));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi tải lịch sử vật phẩm: " + e.getMessage()));
            }
        }).start();
    }

    private void setupAuctionHistoryTable() {
        titleCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueOrUnknown(cell.getValue().getTitle())));
        priceCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(CurrencyFormatter.format(cell.getValue().getCurrentPrice())));
        statusCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getStatus() != null ? cell.getValue().getStatus().name() : "UNKNOWN"));
        timeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTime(cell.getValue().getEndTime())));
    }

    private void setupPriceChart() {
        priceSeries.setName("Giá phiên");
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        priceChart.getData().clear();
        priceChart.getData().add(priceSeries);
    }

    private void renderAuctionHistory(List<AuctionDTO> history) {
        auctionHistoryTable.getItems().clear();
        priceSeries.getData().clear();

        if (history == null || history.isEmpty()) {
            statusLabel.setText("Vật phẩm này chưa từng gắn với phiên đấu giá nào.");
            return;
        }

        List<AuctionDTO> sortedHistory = history.stream()
                .sorted(Comparator.comparing(this::historyTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        auctionHistoryTable.getItems().setAll(sortedHistory);
        for (int i = 0; i < sortedHistory.size(); i++) {
            AuctionDTO auction = sortedHistory.get(i);
            priceSeries.getData().add(new XYChart.Data<>(i + 1, auction.getCurrentPrice()));
        }

        statusLabel.setText("Có " + sortedHistory.size() + " phiên đấu giá gắn với vật phẩm này.");
    }

    private LocalDateTime historyTime(AuctionDTO auction) {
        if (auction == null) {
            return null;
        }
        return auction.getEndTime() != null ? auction.getEndTime() : auction.getStartTime();
    }

    private String formatExtraFields(Map<String, Object> extraFields) {
        if (extraFields == null || extraFields.isEmpty()) {
            return "--";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : extraFields.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(formatExtraFieldName(entry.getKey()))
                    .append(": ")
                    .append(formatExtraFieldValue(entry.getValue()));
        }
        return builder.toString();
    }

    private String formatExtraFieldName(String key) {
        if ("brand".equals(key)) {
            return "Brand";
        }
        if ("warrantyMonths".equals(key)) {
            return "Warranty";
        }
        if ("vehicleYear".equals(key)) {
            return "Vehicle year";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            if (i > 0 && Character.isUpperCase(ch)) {
                result.append(' ');
            }
            result.append(i == 0 ? Character.toUpperCase(ch) : ch);
        }
        return result.toString();
    }

    private String formatExtraFieldValue(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (doubleValue == Math.rint(doubleValue)) {
                return String.format("%.0f", doubleValue);
            }
        }
        return value != null ? value.toString() : "";
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DISPLAY_TIME) : "Chưa rõ";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String valueOrEmpty(Object value) {
        return value != null ? value.toString() : "";
    }

    private String valueOrUnknown(String value) {
        return value != null && !value.isBlank() ? value : "Chưa rõ";
    }

    private String usernameOf(UserDTO user) {
        return user != null ? user.getUsername() : "Chưa rõ";
    }
}
