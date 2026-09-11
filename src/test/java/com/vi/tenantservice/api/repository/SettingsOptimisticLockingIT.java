package com.vi.tenantservice.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vi.tenantservice.api.model.TenantAdminControlsEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(
    properties = {
      "spring.profiles.active=testing",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SettingsOptimisticLockingIT {

  @Autowired private TenantAdminControlsRepository repository;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  void staleSettingsWriter_shouldFailInsteadOfOverwritingTheLatestValue() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    Long id =
        transaction.execute(
            ignored ->
                repository
                    .saveAndFlush(
                        TenantAdminControlsEntity.builder()
                            .controls("{\"permissionsPageEnabled\":true}")
                            .updateDate(LocalDateTime.now())
                            .build())
                    .getId());

    TenantAdminControlsEntity firstWriter =
        transaction.execute(ignored -> repository.findById(id).orElseThrow());
    TenantAdminControlsEntity staleWriter =
        transaction.execute(ignored -> repository.findById(id).orElseThrow());

    firstWriter.setControls("{\"permissionsPageEnabled\":false}");
    transaction.executeWithoutResult(ignored -> repository.saveAndFlush(firstWriter));

    staleWriter.setControls("{\"permissionsPageEnabled\":true,\"stale\":true}");
    assertThatThrownBy(
            () -> transaction.executeWithoutResult(ignored -> repository.saveAndFlush(staleWriter)))
        .isInstanceOf(OptimisticLockingFailureException.class);

    String stored =
        transaction.execute(ignored -> repository.findById(id).orElseThrow().getControls());
    assertThat(stored).isEqualTo("{\"permissionsPageEnabled\":false}");
  }
}
