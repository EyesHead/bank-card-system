package com.example.bankcards.config;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@RequiredArgsConstructor
@Component
@Slf4j
public class AdminInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminUsername = "admin";
        String adminRawPassword = "admin";

        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Администратор [{}] уже существует в базе данных.", adminUsername);
            return;
        }

        log.info("Инициализация администратора [{}] через Java-код...", adminUsername);

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow(() ->
                        new IllegalStateException("Критическая ошибка: Роль ROLE_ADMIN не найдена в БД. Проверьте миграции"));

        User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminRawPassword))
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Администратор [{}] успешно создан и сохранен.", adminUsername);
    }
}
