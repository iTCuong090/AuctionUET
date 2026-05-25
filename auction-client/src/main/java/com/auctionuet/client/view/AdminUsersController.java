package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.AdminClient;
import com.auctionuet.protocol.dto.response.admin.AdminUserDTO;
import com.auctionuet.protocol.enums.AccountStatus;
import com.auctionuet.protocol.enums.UserRole;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

public class AdminUsersController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<AdminUserDTO> usersTable;
    @FXML private TableColumn<AdminUserDTO, String> usernameColumn;
    @FXML private TableColumn<AdminUserDTO, String> roleColumn;
    @FXML private TableColumn<AdminUserDTO, String> statusColumn;
    @FXML private TableColumn<AdminUserDTO, Void> actionColumn;
    @FXML private Label statusLabel;

    private final AdminClient adminClient = new AdminClient();

    @FXML
    public void initialize() {
        roleFilter.setItems(FXCollections.observableArrayList("Tất cả", "BIDDER", "SELLER", "ADMIN"));
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "ACTIVE", "BLOCKED"));
        roleFilter.setValue("Tất cả");
        statusFilter.setValue("Tất cả");

        usernameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUsername()));
        roleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRole().name()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus().name()));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button actionButton = new Button();

            {
                actionButton.setOnAction(event -> {
                    AdminUserDTO user = getTableView().getItems().get(getIndex());
                    AccountStatus nextStatus = user.getStatus() == AccountStatus.ACTIVE
                            ? AccountStatus.BLOCKED
                            : AccountStatus.ACTIVE;
                    updateStatus(user, nextStatus);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                AdminUserDTO user = getTableView().getItems().get(getIndex());
                actionButton.setText(user.getStatus() == AccountStatus.ACTIVE ? "Khóa" : "Mở khóa");
                actionButton.setDisable(user.getRole() == UserRole.ADMIN);
                setGraphic(actionButton);
            }
        });

        refreshUsers();
    }

    @FXML
    private void handleSearch() {
        refreshUsers();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        roleFilter.setValue("Tất cả");
        statusFilter.setValue("Tất cả");
        refreshUsers();
    }

    private void refreshUsers() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) {
            statusLabel.setText("Phiên đăng nhập không hợp lệ.");
            return;
        }
        statusLabel.setText("Đang tải...");
        String query = searchField.getText();
        UserRole role = parseRole(roleFilter.getValue());
        AccountStatus status = parseStatus(statusFilter.getValue());

        new Thread(() -> {
            try {
                List<AdminUserDTO> users = adminClient.getUsers(token, query, role, status);
                Platform.runLater(() -> {
                    usersTable.setItems(FXCollections.observableArrayList(users));
                    statusLabel.setText("Có " + users.size() + " tài khoản.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi: " + e.getMessage()));
            }
        }, "admin-user-load").start();
    }

    private void updateStatus(AdminUserDTO user, AccountStatus status) {
        String token = ClientSession.getInstance().getToken();
        statusLabel.setText("Đang cập nhật...");
        new Thread(() -> {
            try {
                adminClient.updateUserStatus(token, user.getId(), status);
                Platform.runLater(this::refreshUsers);
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Lỗi: " + e.getMessage()));
            }
        }, "admin-user-status").start();
    }

    private UserRole parseRole(String value) {
        return value == null || "Tất cả".equals(value) ? null : UserRole.valueOf(value);
    }

    private AccountStatus parseStatus(String value) {
        return value == null || "Tất cả".equals(value) ? null : AccountStatus.valueOf(value);
    }
}
