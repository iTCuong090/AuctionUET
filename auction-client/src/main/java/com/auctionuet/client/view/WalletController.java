package com.auctionuet.client.view;

import com.auctionuet.client.model.ClientSession;
import com.auctionuet.client.network.WalletClient;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class WalletController {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML private Label balanceLabel;
    @FXML private Label frozenLabel;
    @FXML private Label totalLabel;

    @FXML private TextField depositAmountField;
    @FXML private Button generateQrBtn;
    @FXML private VBox qrPlaceholder;
    @FXML private Button confirmDepositBtn;

    @FXML private TextField withdrawAmountField;
    @FXML private Label statusLabel;
    @FXML private ListView<String> transactionListView;

    private final WalletClient walletClient = new WalletClient();

    @FXML
    public void initialize() {
        loadWalletInfo();
        loadTransactionHistory();
    }

    private void loadWalletInfo() {
        String token = ClientSession.getInstance().getToken();
        if (token == null) return;

        new Thread(() -> {
            try {
                WalletResponseDTO walletInfo = walletClient.getWallet(token);
                Platform.runLater(() -> {
                    renderWallet(walletInfo);
                    statusLabel.setText("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Loi tai vi: " + e.getMessage()));
            }
        }).start();
    }

    private void loadTransactionHistory() {
        String token = ClientSession.getInstance().getToken();
        if (token == null || transactionListView == null) return;

        new Thread(() -> {
            try {
                List<TransactionDTO> transactions = walletClient.getMyTransactions(token);
                Platform.runLater(() -> renderTransactions(transactions));
            } catch (Exception e) {
                Platform.runLater(() -> transactionListView.getItems().setAll("Loi tai lich su: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleGenerateQr() {
        String amountText = depositAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            statusLabel.setText("Vui long nhap so tien can nap.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                statusLabel.setText("So tien phai lon hon 0.");
                return;
            }

            qrPlaceholder.setVisible(true);
            qrPlaceholder.setManaged(true);
            confirmDepositBtn.setVisible(true);
            confirmDepositBtn.setManaged(true);
            generateQrBtn.setVisible(false);
            generateQrBtn.setManaged(false);

            statusLabel.setText("Vui long quet ma QR, sau do nhan Xac nhan.");
            statusLabel.setStyle("-fx-text-fill: #22c55e;");
        } catch (NumberFormatException ex) {
            statusLabel.setText("So tien khong hop le.");
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
                    WalletResponseDTO newWallet = walletClient.deposit(token, amount);
                    Platform.runLater(() -> {
                        renderWallet(newWallet);
                        statusLabel.setText("Nap tien thanh cong!");
                        statusLabel.setStyle("-fx-text-fill: #22c55e;");

                        depositAmountField.clear();
                        qrPlaceholder.setVisible(false);
                        qrPlaceholder.setManaged(false);
                        confirmDepositBtn.setVisible(false);
                        confirmDepositBtn.setManaged(false);
                        generateQrBtn.setVisible(true);
                        generateQrBtn.setManaged(true);
                        loadTransactionHistory();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Loi nap tien: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            statusLabel.setText("So tien khong hop le.");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleWithdraw() {
        String amountText = withdrawAmountField.getText().replace(",", "").trim();
        if (amountText.isEmpty()) {
            statusLabel.setText("Vui long nhap so tien can rut.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                statusLabel.setText("So tien phai lon hon 0.");
                return;
            }

            String token = ClientSession.getInstance().getToken();

            new Thread(() -> {
                try {
                    WalletResponseDTO newWallet = walletClient.withdraw(token, amount);
                    Platform.runLater(() -> {
                        renderWallet(newWallet);
                        statusLabel.setText("Rut tien thanh cong!");
                        statusLabel.setStyle("-fx-text-fill: #22c55e;");
                        withdrawAmountField.clear();
                        loadTransactionHistory();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Loi rut tien: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                }
            }).start();
        } catch (NumberFormatException ex) {
            statusLabel.setText("So tien khong hop le.");
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void renderWallet(WalletResponseDTO wallet) {
        double balance = wallet != null ? wallet.getBalance() : 0.0;
        double frozen = wallet != null ? wallet.getFrozenBalance() : 0.0;
        double total = wallet != null ? wallet.getTotalBalance() : 0.0;

        balanceLabel.setText(String.format("%,.0f VND", balance));
        frozenLabel.setText(String.format("%,.0f VND", frozen));
        totalLabel.setText(String.format("%,.0f VND", total));
    }

    private void renderTransactions(List<TransactionDTO> transactions) {
        transactionListView.getItems().clear();
        if (transactions == null || transactions.isEmpty()) {
            transactionListView.getItems().add("Chua co giao dich nao.");
            return;
        }

        for (TransactionDTO transaction : transactions) {
            String time = transaction.getCreatedAt() != null
                    ? transaction.getCreatedAt().format(DISPLAY_TIME)
                    : "Chua ro";
            String auctionText = transaction.getAuctionId() != null
                    ? " | Auction: " + transaction.getAuctionId()
                    : "";
            String description = transaction.getDescription() != null ? " | " + transaction.getDescription() : "";
            transactionListView.getItems().add(String.format(
                    "%s | %s | %,.0f VND%s%s",
                    time,
                    transaction.getType(),
                    transaction.getAmount(),
                    auctionText,
                    description));
        }
    }
}
