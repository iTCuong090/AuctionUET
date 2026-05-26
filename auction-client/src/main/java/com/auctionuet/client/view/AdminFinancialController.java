package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AdminClient;
import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.dto.response.admin.FinancialSummaryDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.enums.TransactionType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminFinancialController {
    private static final String ALL_TYPES = "Tất cả giao dịch";
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label availableBalanceLabel;
    @FXML private Label frozenBalanceLabel;
    @FXML private Label penaltyRevenueLabel;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<UserFilterOption> userFilter;
    @FXML private TableView<TransactionDTO> transactionTable;
    @FXML private TableColumn<TransactionDTO, String> timeColumn;
    @FXML private TableColumn<TransactionDTO, String> userColumn;
    @FXML private TableColumn<TransactionDTO, String> typeColumn;
    @FXML private TableColumn<TransactionDTO, String> amountColumn;
    @FXML private TableColumn<TransactionDTO, String> auctionColumn;
    @FXML private TableColumn<TransactionDTO, String> descriptionColumn;

    private final AdminClient adminClient = new AdminClient();

    @FXML
    public void initialize() {
        List<String> types = new ArrayList<>();
        types.add(ALL_TYPES);
        Arrays.stream(TransactionType.values()).map(Enum::name).forEach(types::add);
        typeFilter.setItems(FXCollections.observableArrayList(types));
        typeFilter.setValue(ALL_TYPES);

        userFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserFilterOption option) {
                return option != null ? option.label : "";
            }

            @Override
            public UserFilterOption fromString(String value) {
                return null;
            }
        });

        timeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTime(cell.getValue())));
        userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUsernameSnapshot()));
        typeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getType().name()));
        amountColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(CurrencyFormatter.format(cell.getValue().getAmount())));
        auctionColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getAuctionId())));
        descriptionColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(valueOrDash(cell.getValue().getDescription())));

        loadDashboard();
    }

    @FXML
    private void handleFilter() {
        loadTransactions();
    }

    @FXML
    private void handleReset() {
        typeFilter.setValue(ALL_TYPES);
        if (!userFilter.getItems().isEmpty()) {
            userFilter.setValue(userFilter.getItems().get(0));
        }
        loadTransactions();
    }

    @FXML
    private void handleRefresh() {
        loadDashboard();
    }

    private void loadDashboard() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statusLabel.setText("Phiên đăng nhập không hợp lệ.");
            return;
        }
        TransactionType selectedType = selectedType();
        String selectedUserId = selectedUserId();
        statusLabel.setText("Đang tải dữ liệu tài chính...");
        new Thread(() -> {
            try {
                FinancialSummaryDTO summary = adminClient.getFinancialSummary(token);
                List<AdminUserDTO> users = adminClient.getUsers(token, null, null, null);
                List<TransactionDTO> transactions =
                        adminClient.getGlobalTransactions(token, selectedUserId, selectedType);
                Platform.runLater(() -> {
                    renderSummary(summary);
                    populateUsers(users, selectedUserId);
                    transactionTable.setItems(FXCollections.observableArrayList(transactions));
                    statusLabel.setText("Có " + transactions.size() + " giao dịch.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi: " + e.getMessage()));
            }
        }, "admin-financial-load").start();
    }

    private void loadTransactions() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statusLabel.setText("Phiên đăng nhập không hợp lệ.");
            return;
        }
        String userId = selectedUserId();
        TransactionType type = selectedType();
        statusLabel.setText("Đang lọc giao dịch...");
        new Thread(() -> {
            try {
                List<TransactionDTO> transactions = adminClient.getGlobalTransactions(token, userId, type);
                Platform.runLater(() -> {
                    transactionTable.setItems(FXCollections.observableArrayList(transactions));
                    statusLabel.setText("Có " + transactions.size() + " giao dịch.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi: " + e.getMessage()));
            }
        }, "admin-financial-filter").start();
    }

    private void renderSummary(FinancialSummaryDTO summary) {
        CurrencyFormatter.setMoneyText(availableBalanceLabel, summary.getAvailableBalanceTotal());
        CurrencyFormatter.setMoneyText(frozenBalanceLabel, summary.getFrozenBalanceTotal());
        CurrencyFormatter.setMoneyText(penaltyRevenueLabel, summary.getPenaltyRevenueTotal());
    }

    private void populateUsers(List<AdminUserDTO> users, String selectedUserId) {
        List<UserFilterOption> options = new ArrayList<>();
        options.add(new UserFilterOption(null, "Tất cả người dùng"));
        for (AdminUserDTO user : users) {
            options.add(new UserFilterOption(user.getId(), user.getUsername()));
        }
        userFilter.setItems(FXCollections.observableArrayList(options));
        userFilter.setValue(options.stream()
                .filter(option -> selectedUserId != null && selectedUserId.equals(option.id))
                .findFirst()
                .orElse(options.get(0)));
    }

    private TransactionType selectedType() {
        String value = typeFilter.getValue();
        return value == null || ALL_TYPES.equals(value) ? null : TransactionType.valueOf(value);
    }

    private String selectedUserId() {
        UserFilterOption selected = userFilter.getValue();
        return selected != null ? selected.id : null;
    }

    private String formatTime(TransactionDTO transaction) {
        return transaction.getCreatedAt() != null
                ? transaction.getCreatedAt().format(DISPLAY_TIME)
                : "-";
    }

    private String valueOrDash(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private static final class UserFilterOption {
        private final String id;
        private final String label;

        private UserFilterOption(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }
}
