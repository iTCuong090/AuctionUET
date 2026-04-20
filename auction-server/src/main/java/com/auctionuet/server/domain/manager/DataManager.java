package com.auctionuet.server.domain.manager;

import com.auctionuet.server.persistence.dao.AuctionDAO;
import com.auctionuet.server.persistence.dao.BidDAO;
import com.auctionuet.server.persistence.dao.ItemDAO;
import com.auctionuet.server.persistence.dao.UserDAO;

public class DataManager {

    public static final DataManager INSTANCE = new DataManager();

    private final UserDAO userDAO;
    private final ItemDAO itemDAO;
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;

    private DataManager() {
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
        this.auctionDAO = new AuctionDAO();
        this.bidDAO = new BidDAO();
    }

    public static DataManager getInstance() {
        return INSTANCE;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public ItemDAO getItemDAO() {
        return itemDAO;
    }

    public AuctionDAO getAuctionDAO() {
        return auctionDAO;
    }

    public BidDAO getBidDAO() {
        return bidDAO;
    }
}
