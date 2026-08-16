package com.vi.tenantservice.api.converter;

import com.vi.tenantservice.api.model.DpiaDocumentMetadataDTO;
import com.vi.tenantservice.api.model.DpiaKeyFigureDTO;
import com.vi.tenantservice.api.model.DpiaKeyFiguresDTO;
import com.vi.tenantservice.api.model.DpiaOperatorDTO;
import com.vi.tenantservice.api.model.DpiaSupervisoryAuthorityDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataDTO;
import com.vi.tenantservice.api.model.PlatformDpiaMasterDataEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between the DPIA master data DTO groups and the typed singleton entity. The PUT is a full
 * replace: groups absent from the DTO clear the corresponding columns, so the API never merges
 * stale state back in.
 */
@Component
public class PlatformDpiaMasterDataConverter {

  public PlatformDpiaMasterDataDTO toDto(PlatformDpiaMasterDataEntity entity) {
    return new PlatformDpiaMasterDataDTO()
        .operator(
            new DpiaOperatorDTO()
                .legalName(entity.getOperatorLegalName())
                .shortName(entity.getOperatorShortName())
                .address(entity.getOperatorAddress())
                .contactEmail(entity.getOperatorContactEmail())
                .contactPhone(entity.getOperatorContactPhone())
                .dpoName(entity.getOperatorDpoName())
                .department(entity.getOperatorDepartment())
                .responsiblePerson(entity.getOperatorResponsiblePerson()))
        .supervisoryAuthority(
            new DpiaSupervisoryAuthorityDTO()
                .legalFramework(
                    entity.getSupervisoryLegalFramework() != null
                        ? DpiaSupervisoryAuthorityDTO.LegalFrameworkEnum.fromValue(
                            entity.getSupervisoryLegalFramework())
                        : null)
                .name(entity.getSupervisoryAuthorityName())
                .address(entity.getSupervisoryAuthorityAddress())
                .email(entity.getSupervisoryAuthorityEmail()))
        .document(
            new DpiaDocumentMetadataDTO()
                .documentDate(entity.getDocumentDate())
                .nextReviewDate(entity.getDocumentNextReviewDate()))
        .keyFigures(
            new DpiaKeyFiguresDTO()
                .tenants(
                    new DpiaKeyFigureDTO()
                        .count(entity.getKeyFigureTenants())
                        .asOfDate(entity.getKeyFigureTenantsAsOf()))
                .counsellingCentres(
                    new DpiaKeyFigureDTO()
                        .count(entity.getKeyFigureCounsellingCentres())
                        .asOfDate(entity.getKeyFigureCounsellingCentresAsOf()))
                .activeCounsellors(
                    new DpiaKeyFigureDTO()
                        .count(entity.getKeyFigureActiveCounsellors())
                        .asOfDate(entity.getKeyFigureActiveCounsellorsAsOf()))
                .registeredClients(
                    new DpiaKeyFigureDTO()
                        .count(entity.getKeyFigureRegisteredClients())
                        .asOfDate(entity.getKeyFigureRegisteredClientsAsOf())));
  }

  public void applyDtoToEntity(PlatformDpiaMasterDataDTO dto, PlatformDpiaMasterDataEntity entity) {
    var operator = dto.getOperator() != null ? dto.getOperator() : new DpiaOperatorDTO();
    entity.setOperatorLegalName(operator.getLegalName());
    entity.setOperatorShortName(operator.getShortName());
    entity.setOperatorAddress(operator.getAddress());
    entity.setOperatorContactEmail(operator.getContactEmail());
    entity.setOperatorContactPhone(operator.getContactPhone());
    entity.setOperatorDpoName(operator.getDpoName());
    entity.setOperatorDepartment(operator.getDepartment());
    entity.setOperatorResponsiblePerson(operator.getResponsiblePerson());

    var authority =
        dto.getSupervisoryAuthority() != null
            ? dto.getSupervisoryAuthority()
            : new DpiaSupervisoryAuthorityDTO();
    entity.setSupervisoryLegalFramework(
        authority.getLegalFramework() != null ? authority.getLegalFramework().getValue() : null);
    entity.setSupervisoryAuthorityName(authority.getName());
    entity.setSupervisoryAuthorityAddress(authority.getAddress());
    entity.setSupervisoryAuthorityEmail(authority.getEmail());

    var document = dto.getDocument() != null ? dto.getDocument() : new DpiaDocumentMetadataDTO();
    entity.setDocumentDate(document.getDocumentDate());
    entity.setDocumentNextReviewDate(document.getNextReviewDate());

    var keyFigures = dto.getKeyFigures() != null ? dto.getKeyFigures() : new DpiaKeyFiguresDTO();
    var tenants = orEmpty(keyFigures.getTenants());
    entity.setKeyFigureTenants(tenants.getCount());
    entity.setKeyFigureTenantsAsOf(tenants.getAsOfDate());
    var counsellingCentres = orEmpty(keyFigures.getCounsellingCentres());
    entity.setKeyFigureCounsellingCentres(counsellingCentres.getCount());
    entity.setKeyFigureCounsellingCentresAsOf(counsellingCentres.getAsOfDate());
    var activeCounsellors = orEmpty(keyFigures.getActiveCounsellors());
    entity.setKeyFigureActiveCounsellors(activeCounsellors.getCount());
    entity.setKeyFigureActiveCounsellorsAsOf(activeCounsellors.getAsOfDate());
    var registeredClients = orEmpty(keyFigures.getRegisteredClients());
    entity.setKeyFigureRegisteredClients(registeredClients.getCount());
    entity.setKeyFigureRegisteredClientsAsOf(registeredClients.getAsOfDate());
  }

  private DpiaKeyFigureDTO orEmpty(DpiaKeyFigureDTO keyFigure) {
    return keyFigure != null ? keyFigure : new DpiaKeyFigureDTO();
  }
}
