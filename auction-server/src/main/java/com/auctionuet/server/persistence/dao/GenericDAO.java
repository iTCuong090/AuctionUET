package com.auctionuet.server.persistence.dao;

import com.auctionuet.server.persistence.schema.BaseSchema;

import java.util.List;

public interface GenericDAO<T extends BaseSchema> {
    void save(T entity);

    T findById(String id);

    List<T> findAll();

    void update(T entity);

    void delete(String id);
}
