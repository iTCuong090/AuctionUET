package com.auctionuet.server.util;
import java.util.UUID;

public class IdGenerator {
    private IdGenerator() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}

