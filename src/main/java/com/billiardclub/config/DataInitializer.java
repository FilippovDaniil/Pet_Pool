package com.billiardclub.config;

import com.billiardclub.model.*;
import com.billiardclub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TableRepository tableRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUsers();
        initTables();
        initClients();
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .build());

            userRepository.save(User.builder()
                    .username("reception")
                    .password(passwordEncoder.encode("reception"))
                    .role(Role.RECEPTION)
                    .build());

            log.info("Созданы тестовые пользователи: admin/admin, reception/reception");
        }
    }

    private void initTables() {
        if (tableRepository.count() == 0) {
            // 15 Russian tables
            for (int i = 1; i <= 15; i++) {
                tableRepository.save(BilliardTable.builder()
                        .number(i)
                        .type(TableType.RUSSIAN)
                        .build());
            }
            // 3 American tables
            for (int i = 16; i <= 18; i++) {
                tableRepository.save(BilliardTable.builder()
                        .number(i)
                        .type(TableType.AMERICAN)
                        .build());
            }
            log.info("Создано 15 столов русского бильярда и 3 стола американки");
        }
    }

    private void initClients() {
        if (clientRepository.count() == 0) {
            clientRepository.save(Client.builder()
                    .fullName("Иванов Иван Иванович")
                    .rank("Любитель")
                    .phone("+7-900-000-0001")
                    .build());

            clientRepository.save(Client.builder()
                    .fullName("Петров Пётр Петрович")
                    .rank("1 разряд")
                    .phone("+7-900-000-0002")
                    .build());

            clientRepository.save(Client.builder()
                    .fullName("Сидоров Алексей Николаевич")
                    .rank("Кандидат в мастера")
                    .phone("+7-900-000-0003")
                    .build());

            clientRepository.save(Client.builder()
                    .fullName("Козлов Дмитрий Сергеевич")
                    .rank("Мастер")
                    .phone("+7-900-000-0004")
                    .build());

            clientRepository.save(Client.builder()
                    .fullName("Морозова Анна Владимировна")
                    .rank("Любитель")
                    .phone("+7-900-000-0005")
                    .build());

            log.info("Созданы тестовые клиенты");
        }
    }
}
