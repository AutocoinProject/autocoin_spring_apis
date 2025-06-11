package com.autocoin.file.infrastructure;

import com.autocoin.file.domain.File;
import com.autocoin.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileJpaRepository extends JpaRepository<File, Long> {
    List<File> findByUser(User user);
}
