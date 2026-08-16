package com.vi.tenantservice.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.vi.tenantservice.api.model.DpiaDocumentMetadataDTO;
import com.vi.tenantservice.api.model.DpiaKeyFigureDTO;
import com.vi.tenantservice.api.model.DpiaKeyFiguresDTO;
import com.vi.tenantservice.api.model.DpiaOperatorDTO;
import com.vi.tenantservice.api.model.DpiaSupervisoryAuthorityDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlatformDpiaMasterDataConverterTest {

  private final PlatformDpiaMasterDataConverter converter = new PlatformDpiaMasterDataConverter();

  private PlatformDpiaMasterDataDTO fullDto() {
    return new PlatformDpiaMasterDataDTO()
        .operator(
            new DpiaOperatorDTO()
                .legalName("Deutscher Caritasverband e. V.")
                .shortName("DCV")
                .address("Karlstrasse 40, 79104 Freiburg")
                .contactEmail("datenschutz@example.org")
                .contactPhone("+49 761 200-0")
                .dpoName("Jane Doe")
                .department("Online counselling")
                .responsiblePerson("John Doe"))
        .supervisoryAuthority(
            new DpiaSupervisoryAuthorityDTO()
                .legalFramework(DpiaSupervisoryAuthorityDTO.LegalFrameworkEnum.KDG)
                .name("Diocesan data protection officer")
                .address("Some Street 1, 50667 Cologne")
                .email("supervision@example.org"))
        .document(
            new DpiaDocumentMetadataDTO()
                .documentDate(LocalDate.of(2026, 1, 29))
                .nextReviewDate(LocalDate.of(2027, 1, 29)))
        .keyFigures(
            new DpiaKeyFiguresDTO()
                .tenants(new DpiaKeyFigureDTO().count(12L).asOfDate(LocalDate.of(2026, 8, 1)))
                .counsellingCentres(
                    new DpiaKeyFigureDTO().count(340L).asOfDate(LocalDate.of(2026, 8, 1)))
                .activeCounsellors(
                    new DpiaKeyFigureDTO().count(1500L).asOfDate(LocalDate.of(2026, 8, 1)))
                .registeredClients(
                    new DpiaKeyFigureDTO().count(52000L).asOfDate(LocalDate.of(2026, 8, 1))));
  }

  @Test
  void toEntityAndBack_Should_preserveAllFields() {
    PlatformDpiaMasterDataDTO dto = fullDto();

    PlatformDpiaMasterDataEntity entity = new PlatformDpiaMasterDataEntity();
    converter.applyDtoToEntity(dto, entity);
    PlatformDpiaMasterDataDTO roundTripped = converter.toDto(entity);

    assertThat(roundTripped).usingRecursiveComparison().isEqualTo(dto);
  }

  @Test
  void toDto_Should_returnEmptyGroups_When_entityIsEmpty() {
    PlatformDpiaMasterDataDTO dto = converter.toDto(new PlatformDpiaMasterDataEntity());

    assertThat(dto.getOperator()).isNotNull();
    assertThat(dto.getOperator().getLegalName()).isNull();
    assertThat(dto.getSupervisoryAuthority()).isNotNull();
    assertThat(dto.getDocument()).isNotNull();
    assertThat(dto.getKeyFigures()).isNotNull();
    assertThat(dto.getKeyFigures().getTenants()).isNotNull();
    assertThat(dto.getKeyFigures().getTenants().getCount()).isNull();
  }

  @Test
  void applyDtoToEntity_Should_clearFields_When_dtoGroupsAreNull() {
    PlatformDpiaMasterDataEntity entity = new PlatformDpiaMasterDataEntity();
    converter.applyDtoToEntity(fullDto(), entity);

    converter.applyDtoToEntity(new PlatformDpiaMasterDataDTO(), entity);

    assertThat(entity.getOperatorLegalName()).isNull();
    assertThat(entity.getOperatorShortName()).isNull();
    assertThat(entity.getOperatorAddress()).isNull();
    assertThat(entity.getOperatorContactEmail()).isNull();
    assertThat(entity.getOperatorContactPhone()).isNull();
    assertThat(entity.getOperatorDpoName()).isNull();
    assertThat(entity.getOperatorDepartment()).isNull();
    assertThat(entity.getOperatorResponsiblePerson()).isNull();
    assertThat(entity.getSupervisoryLegalFramework()).isNull();
    assertThat(entity.getSupervisoryAuthorityName()).isNull();
    assertThat(entity.getSupervisoryAuthorityAddress()).isNull();
    assertThat(entity.getSupervisoryAuthorityEmail()).isNull();
    assertThat(entity.getDocumentDate()).isNull();
    assertThat(entity.getDocumentNextReviewDate()).isNull();
    assertThat(entity.getKeyFigureTenants()).isNull();
    assertThat(entity.getKeyFigureTenantsAsOf()).isNull();
    assertThat(entity.getKeyFigureCounsellingCentres()).isNull();
    assertThat(entity.getKeyFigureCounsellingCentresAsOf()).isNull();
    assertThat(entity.getKeyFigureActiveCounsellors()).isNull();
    assertThat(entity.getKeyFigureActiveCounsellorsAsOf()).isNull();
    assertThat(entity.getKeyFigureRegisteredClients()).isNull();
    assertThat(entity.getKeyFigureRegisteredClientsAsOf()).isNull();
  }
}
