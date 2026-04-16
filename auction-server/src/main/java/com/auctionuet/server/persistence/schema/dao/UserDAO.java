package com.auctionuet.server.persistence.schema.dao;

import com.auctionuet.server.util.json.JsonFileHelper;
import com.auctionuet.server.persistence.schema.UserSchema;

import java.time.LocalDateTime;
import java.util.List;

//Comment: Hiện tại database đang thiết kế đơn giản, mỗi lần ghi database lại
//là một lần đọc toàn bộ file database ra sau đó sửa list trong ram, tiếp theo lại
//ghi đè nguyên cục database. 
//Phương án này là tạm thời và chấp nhận được với dự án bài tập lớn này. 

public class UserDAO implements GenericDAO<UserSchema> {

    private final String filePath;

    public UserDAO() { // Mặc định đặt đường dẫn file database user là data/users.json.
        this.filePath = "data/users.json";
    }

    public UserDAO(String filePath) {
        this.filePath = filePath;
    }
    // Constructor thứ hai, cho phép chỉ định cụ thể đường dẫn file json lưu trữ thông tin user phục vụ test.

    @Override
    public void save(UserSchema entity) {
        List<UserSchema> list = JsonFileHelper.readList(filePath, UserSchema.class);
        list.add(entity);
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public UserSchema findById(String id) {
        List<UserSchema> list = JsonFileHelper.readList(filePath, UserSchema.class);
        for (UserSchema entity : list) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public List<UserSchema> findAll() {
        return JsonFileHelper.readList(filePath, UserSchema.class);
    }

    @Override
    public void update(UserSchema entity) {
        List<UserSchema> list = JsonFileHelper.readList(filePath, UserSchema.class);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(entity.getId())) {
                entity.setUpdatedAt(LocalDateTime.now());
                list.set(i, entity);
                break;
            }
        }
        JsonFileHelper.writeList(filePath, list);
    }

    @Override
    public void delete(String id) {
        List<UserSchema> list = JsonFileHelper.readList(filePath, UserSchema.class);
        list.removeIf(e -> e.getId().equals(id));
        JsonFileHelper.writeList(filePath, list);
    }

    public UserSchema findByUsername(String username) {
        List<UserSchema> list = JsonFileHelper.readList(filePath, UserSchema.class);
        for (UserSchema entity : list) {
            if (entity.getUsername().equals(username)) {
                return entity;
            }
        }
        return null;
    }
}
