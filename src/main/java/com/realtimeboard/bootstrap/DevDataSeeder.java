package com.realtimeboard.bootstrap;

import com.realtimeboard.model.entity.AppUserRole;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.dev-seed.enabled", havingValue = "true")
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.dev-seed.sync-seed-passwords:false}")
  private boolean syncSeedPasswords;

  public DevDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedOrSync("admin@realtimeboard.local", "Administrator", "Admin123!", AppUserRole.ADMIN);
    seedOrSync("alex@realtimeboard.local", "Alex Chen", "User123!", AppUserRole.USER);
    seedOrSync("jamie@realtimeboard.local", "Jamie Rivera", "User123!", AppUserRole.USER);
    log.info("Dev seed: default users ready (see README for emails/passwords).");
  }

  private void seedOrSync(String email, String name, String rawPassword, AppUserRole role) {
    String normalized = email.toLowerCase();
    Optional<User> existing = userRepository.findByEmailIgnoreCase(normalized);
    if (existing.isEmpty()) {
      User u = new User();
      u.setEmail(normalized);
      u.setName(name);
      u.setPasswordHash(passwordEncoder.encode(rawPassword));
      u.setCreatedAt(Instant.now());
      u.setAppRole(role);
      userRepository.save(u);
      log.info("Seeded user {} ({})", email, role);
      return;
    }
    if (syncSeedPasswords) {
      User u = existing.get();
      u.setPasswordHash(passwordEncoder.encode(rawPassword));
      u.setAppRole(role);
      userRepository.save(u);
      log.info("Synced dev password and role for {} ({})", email, role);
    }
  }
}
