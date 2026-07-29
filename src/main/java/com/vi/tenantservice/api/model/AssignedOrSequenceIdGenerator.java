package com.vi.tenantservice.api.model;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * Sequence-style ID generator that honours application-assigned identifiers (TEN-INV-U1).
 *
 * <p>The authoritative tenant ID allocation (smallest free ID under a database lock, reserved IDs
 * from open invites) assigns the tenant ID explicitly before the entity is persisted. Entities
 * without a pre-assigned ID (legacy paths and tests) keep drawing from the database sequence
 * exactly as before.
 */
public class AssignedOrSequenceIdGenerator extends SequenceStyleGenerator {

  @Override
  public boolean allowAssignedIdentifiers() {
    return true;
  }

  @Override
  public Object generate(SharedSessionContractImplementor session, Object owner) {
    Object assignedId = session.getEntityPersister(null, owner).getIdentifier(owner, session);
    return assignedId != null ? assignedId : super.generate(session, owner);
  }
}
