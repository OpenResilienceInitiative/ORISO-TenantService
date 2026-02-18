package com.vi.tenantservice.api.converter;

import com.vi.tenantservice.api.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTOWelcomeMessage;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsultingTypePatchDTOConverter {

  public ConsultingTypePatchDTO convertConsultingTypePatchDTO(
      FullConsultingTypeResponseDTO consultingTypeResponseDTO) {
    ConsultingTypePatchDTO consultingTypePatchDTO = initializeExtendedTenantSettings();
    consultingTypePatchDTO.setLanguageFormal(consultingTypeResponseDTO.getLanguageFormal());
    consultingTypePatchDTO.setIsVideoCallAllowed(consultingTypeResponseDTO.getIsVideoCallAllowed());

    if (consultingTypeResponseDTO.getWelcomeMessage() != null) {
      consultingTypePatchDTO
          .getWelcomeMessage()
          .setSendWelcomeMessage(
              consultingTypeResponseDTO.getWelcomeMessage().getSendWelcomeMessage());
      consultingTypePatchDTO
          .getWelcomeMessage()
          .setWelcomeMessageText(
              consultingTypeResponseDTO.getWelcomeMessage().getWelcomeMessageText());
    }

    consultingTypePatchDTO.setSendFurtherStepsMessage(
        consultingTypeResponseDTO.getSendFurtherStepsMessage());

    return consultingTypePatchDTO;
  }

  private ConsultingTypePatchDTO initializeExtendedTenantSettings() {
    ConsultingTypePatchDTO consultingTypePatchDTO = new ConsultingTypePatchDTO();
    consultingTypePatchDTO.setWelcomeMessage(new ConsultingTypePatchDTOWelcomeMessage());
    return consultingTypePatchDTO;
  }

  public com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypePatchDTO
      convertToConsultingTypeServiceModel(ConsultingTypePatchDTO extendedSettings) {
    com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypePatchDTO
        targetDTO =
            new com.vi.tenantservice.consultingtypeservice.generated.web.model
                .ConsultingTypePatchDTO();

    BeanUtils.copyProperties(extendedSettings, targetDTO);
    convertWelcomeMessage(extendedSettings, targetDTO);
    return targetDTO;
  }

  private void convertWelcomeMessage(
      ConsultingTypePatchDTO extendedSettings,
      com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypePatchDTO
          targetDTO) {
    if (extendedSettings.getWelcomeMessage() != null) {
      targetDTO.setWelcomeMessage(
          new com.vi.tenantservice.consultingtypeservice.generated.web.model
              .ExtendedConsultingTypeResponseDTOAllOfWelcomeMessage());
      BeanUtils.copyProperties(extendedSettings.getWelcomeMessage(), targetDTO.getWelcomeMessage());
    }
  }
}
