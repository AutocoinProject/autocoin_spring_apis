package com.autocoin.file.domain;

import com.autocoin.user.domain.User;

import java.util.List;
import java.util.Optional;

public interface FileRepository {
    File save(File file);
    Optional<File> findById(Long id);
    List<File> findByUser(User user);
    void delete(File file);
}
