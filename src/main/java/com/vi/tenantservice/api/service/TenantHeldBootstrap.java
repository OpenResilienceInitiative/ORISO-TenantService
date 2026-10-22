package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

/** Fixed held-bootstrap operation; a public selector never constructs creation authority. */
@Service
public class TenantHeldBootstrap {
  private final TenantHeldBootstrapJournal journal;
  private final TenantBootstrapCreation creation;
  private final TenantServiceFacade tenants;
  private final ConsultingTypeService consulting;
  private final Validator validator;

  public TenantHeldBootstrap(TenantHeldBootstrapJournal journal, TenantBootstrapCreation creation,
      TenantServiceFacade tenants, ConsultingTypeService consulting, Validator validator) {
    this.journal=journal;this.creation=creation;this.tenants=tenants;this.consulting=consulting;this.validator=validator;
  }

  public Result create(JsonNode command) {
    var operation=journal.prepare(command);
    try {
      if(!validator.validate(operation.input).isEmpty()) throw TenantHeldBootstrapJournal.denied();
      var dpa=operation.input.getOnboardingDpaAcceptance();
      if(dpa!=null && (!Boolean.TRUE.equals(dpa.getAccepted()) || dpa.getDpaVersion()==null))
        throw TenantHeldBootstrapJournal.denied();
      var row=journal.read(operation,false);
      MultilingualTenantDTO original;
      try {original=TenantHeldBootstrapJournal.JSON.treeToValue(
          TenantHeldBootstrapJournal.JSON.valueToTree(operation.input),MultilingualTenantDTO.class);}
      catch(Exception invalid) {throw TenantHeldBootstrapJournal.denied();}
      if(row==null) {
        journal.freeze(operation,consulting.freezeBootstrapCommand(operation.input.getId()));
        tenants.createHeldTenant(original,operation);
      } else if(!"COMPLETE".equals(row.state())) {
        tenants.resumeHeldTenant(original,operation);
      }
      // Both initial and repeated replies use the same coherent committed native data.
      MultilingualTenantDTO result=tenants.readHeldTenant(operation);
      String resultDigest=journal.digest("tenant-held-bootstrap-result/v1",TenantHeldBootstrapJournal.JSON.valueToTree(result));
      row=journal.read(operation,false);
      if(row==null) throw TenantHeldBootstrapJournal.denied();
      if(!"COMPLETE".equals(row.state())) creation.completeHeld(operation,resultDigest);
      return new Result(result,journal.receipt(operation,resultDigest));
    } finally {operation.open=false;}
  }

  public record Result(MultilingualTenantDTO tenant, String receipt) {
    @Override public String toString() { return "HeldTenantBootstrapResult[redacted]"; }
  }
}
