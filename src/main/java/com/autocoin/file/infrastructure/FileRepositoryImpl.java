package com.autocoin.file.infrastructure;

import com.autocoin.file.domain.File;
import com.autocoin.file.domain.FileRepository;
import com.autocoin.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileRepositoryImpl implements FileRepository {

    private final FileJpaRepository fileJpaRepository;

    @Override
    public File save(File file) {
        return fileJpaRepository.save(file);
    }

    @Override
    public Optional<File> findById(Long id) {
        return fileJpaRepository.findById(id);
    }

    @Override
    public List<File> findByUser(User user) {
        return fileJpaRepository.findByUser(user);
    }

    @Override
    public void delete(File file) {
        fileJpaRepository.delete(file);
    }
}
