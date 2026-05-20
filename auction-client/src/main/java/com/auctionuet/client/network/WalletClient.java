package com.auctionuet.client.network;

import com.auctionuet.protocol.ActionType;
import com.auctionuet.protocol.Request;
import com.auctionuet.protocol.Response;
import com.auctionuet.protocol.dto.request.wallet.AmountRequestDTO;
import com.auctionuet.protocol.dto.response.transaction.TransactionDTO;
import com.auctionuet.protocol.dto.response.wallet.WalletResponseDTO;

import java.util.List;

public class WalletClient {

    public WalletResponseDTO deposit(String token, double amount) throws Exception {
        return sendAmountRequest(ActionType.DEPOSIT, token, amount);
    }

    public WalletResponseDTO withdraw(String token, double amount) throws Exception {
        return sendAmountRequest(ActionType.WITHDRAW, token, amount);
    }

    public WalletResponseDTO getWallet(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_WALLET, null, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(WalletResponseDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    public List<TransactionDTO> getMyTransactions(String token) throws Exception {
        Response response = ServerConnection.getInstance()
                .sendRequest(new Request(ActionType.GET_MY_TRANSACTIONS, null, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataListAs(TransactionDTO.class);
        }
        throw new Exception(response.getMessage());
    }

    private WalletResponseDTO sendAmountRequest(ActionType action, String token, double amount) throws Exception {
        AmountRequestDTO data = new AmountRequestDTO();
        data.setAmount(amount);

        Response response = ServerConnection.getInstance()
                .sendRequest(Request.fromDto(action, data, token));

        if ("OK".equals(response.getStatus())) {
            return response.getDataAs(WalletResponseDTO.class);
        }
        throw new Exception(response.getMessage());
    }
}
