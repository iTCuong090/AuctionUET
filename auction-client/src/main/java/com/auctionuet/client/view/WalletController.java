package com.auctionuet.client.view;

import com.auctionuet.client.network.WalletClient;
import com.auctionuet.client.model.ClientSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Map;

public class WalletController {

    @FXML private Label balanceLabel;
    @FXML private Label frozenLabel;
    @FXML private Label totalLabel;
    
    @FXML private TextField depositAmountField;
    @FXML private Button generateQrBtn;
    @FXML private VBox qrPlaceholder;
    @FXML private Button confirmDepositBtn;
    
    @FXML private TextField withdrawAmountField;
    @FXML private Label statusLabel;

    private final WalletClient walletClient = new WalletClient();

    @FXML
    public void initialize() {
        loadWalletInfo();
    }

    private void loadWalletInfo() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                Map<String, Double> walletInfo = walletClient.getWallet(token);
                
                double balance = walletInfo.getOrDefault("balance", 0.0);
                double frozen = walletInfo.getOrDefault("frozenBalance", 0.0);
                double total = balance + frozen;

                Platform.runLater(() -> {
                    balanceLabel.setText(String.format("%,.0f VNĐ", balance));
                    frozenLabel.setText(String.format("%,.0f VNĐ", frozen));
                    totalLabel.setText(String.format("%,.0f VNĐ", total));
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("❌ Lỗi tải ví: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleGenerateQr() {
        String amountText = depositAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            statusLabel.setText("❌ Vui lòng nhập số tiền cần nạp.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                statusLabel.setText("❌ Số tiền phải lớn hơn 0.");
                return;
            }

            // Hiện mã QR và nút xác nhận, ẩn nút tạo QR
            qrPlaceholder.setVisible(true);
            qrPlaceholder.setManaged(true);
            confirmDepositBtn.setVisible(true);
            confirmDepositBtn.setManaged(true);
            generateQrBtn.setVisible(false);
            generateQrBtn.setManaged(false);
            
            statusLabel.setText("✅ Vui lòng quét mã QR để thanh toán, sau đó nhấn Xác nhận.");
            statusLabel.setStyle("-fx-text-fill: #22c55e;"); // Green color for success tip

        } catch (NumberFormatException ex) {
            statusLabel.setText("❌ Số tiền không hợp lệ.");
        }
    }

    @FXML
    private void handleDeposit() {
        String amountText = depositAmountField.getText().replace(",", "").trim();
        try {
            double amount = Double.parseDouble(amountText);
            String token = ClientSession.getInstance().getToken();

            new Thread(() -> {
                try {
                    Map<String, Double> newWallet = walletClient.deposit(token, amount);
                    
                    double balance = newWallet.getOrDefault("balance", 0.0);
                    double frozen = newWallet.getOrDefault("frozenBalance", 0.0);
                    double total = balance + frozen;

                    Platform.runLater(() -> {
                        balanceLabel.setText(String.format("%,.0f VNĐ", balance));
                        frozenLabel.setText(String.format("%,.0f VNĐ", frozen));
                        totalLabel.setText(String.format("%,.0f VNĐ", total));
                        statusLabel.setText("✅ Nạp tiền thành công!");
                        statusLabel.setStyle("-fx-text-fill: #22c55e;");
                        
                        // Reset UI nạp tiền
                        depositAmountField.clear();
                        qrPlaceholder.setVisible(false);
                        qrPlaceholder.setManaged(false);
                        confirmDepositBtn.setVisible(false);
                        confirmDepositBtn.setManaged(false);
                        generateQrBtn.setVisible(true);
                        generateQrBtn.setManaged(true);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Lỗi nạp tiền: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();

        } catch (NumberFormatException ex) {
            statusLabel.setText("❌ Số tiền không hợp lệ.");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleWithdraw() {
        String amountText = withdrawAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            statusLabel.setText("❌ Vui lòng nhập số tiền cần rút.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                statusLabel.setText("❌ Số tiền phải lớn hơn 0.");
                return;
            }

            String token = ClientSession.getInstance().getToken();

            new Thread(() -> {
                try {
                    Map<String, Double> newWallet = walletClient.withdraw(token, amount);
                    
                    double balance = newWallet.getOrDefault("balance", 0.0);
                    double frozen = newWallet.getOrDefault("frozenBalance", 0.0);
                    double total = balance + frozen;

                    Platform.runLater(() -> {
                        balanceLabel.setText(String.format("%,.0f VNĐ", balance));
                        frozenLabel.setText(String.format("%,.0f VNĐ", frozen));
                        totalLabel.setText(String.format("%,.0f VNĐ", total));
                        statusLabel.setText("✅ Rút tiền thành công!");
                        statusLabel.setStyle("-fx-text-fill: #22c55e;");
                        withdrawAmountField.clear();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Lỗi rút tiền: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();

        } catch (NumberFormatException ex) {
            statusLabel.setText("❌ Số tiền không hợp lệ.");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }
}
