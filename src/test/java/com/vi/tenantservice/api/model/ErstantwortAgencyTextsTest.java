package com.vi.tenantservice.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * ORISO-Admin#601: storage for the Bausteine a Träger authors in its own voice — greeting, who
 * reads along, the emergency-numbers addition, the single free notice, and the closing — plus the
 * Antwortfrist.
 *
 * <p>Modelled on the existing legal-text columns (`content_impressum`, `content_privacy`) rather
 * than on a new table: these are per-tenant rich-text bodies with exactly the same lifecycle, and
 * the machine-translation-on-publish mechanism ADR-018 §8 reuses is already wired to that shape.
 *
 * <p><b>Exactly one free notice</b> is enforced structurally, by there being a single column. A
 * collection would have made "a Träger cannot add a second" a validation rule somebody could relax;
 * one column cannot hold two.
 *
 * <p><b>The Antwortfrist is a number</b>, never prose (ADR-018). Held as a number so the promise is
 * one value that can be compared against what actually happened, and so the wording can be rendered
 * around it in seven locales without anyone retyping it.
 */
class ErstantwortAgencyTextsTest {

  @Test
  void aFreshTenantAuthorsNothingAndFallsBackToThePlatform() {
    /* Absent, not empty. An empty string would render as a blank Baustein in the
    middle of the sequence; null is what the resolution chain reads as "this
    level said nothing, ask the level below". */
    var entity = new TenantEntity();

    assertThat(entity.getContentErstantwortGreeting()).isNull();
    assertThat(entity.getContentErstantwortWhoReadsAlong()).isNull();
    assertThat(entity.getContentErstantwortEmergencyAddition()).isNull();
    assertThat(entity.getContentErstantwortFreeNotice()).isNull();
    assertThat(entity.getContentErstantwortClosing()).isNull();
  }

  @Test
  void theResponseDeadlineIsAbsentUntilATraegerSetsIt() {
    /* Not defaulted to 2 here. The platform default belongs to the renderer, so a
    tenant that never chose a number is distinguishable from one that chose 2 —
    the difference matters the day the platform default changes. */
    assertThat(new TenantEntity().getErstantwortResponseDeadlineDays()).isNull();
  }

  @Test
  void everyAuthoredBausteinRoundTrips() {
    var entity = new TenantEntity();
    entity.setContentErstantwortGreeting("<p>Schön, dass Du da bist.</p>");
    entity.setContentErstantwortWhoReadsAlong("<p>Wir beraten im Peer-Modell.</p>");
    entity.setContentErstantwortEmergencyAddition("<p>U25 ist rund um die Uhr da.</p>");
    entity.setContentErstantwortFreeNotice("<p>Supervision durch Fachkräfte.</p>");
    entity.setContentErstantwortClosing("<p>Bis bald.</p>");
    entity.setErstantwortResponseDeadlineDays(5);

    assertThat(entity.getContentErstantwortGreeting()).contains("Schön, dass Du da bist.");
    assertThat(entity.getContentErstantwortWhoReadsAlong()).contains("Peer-Modell");
    assertThat(entity.getContentErstantwortEmergencyAddition()).contains("U25");
    assertThat(entity.getContentErstantwortFreeNotice()).contains("Supervision");
    assertThat(entity.getContentErstantwortClosing()).contains("Bis bald.");
    assertThat(entity.getErstantwortResponseDeadlineDays()).isEqualTo(5);
  }

  @Test
  void thereIsExactlyOneFreeNoticeBecauseThereIsExactlyOneColumn() {
    /* ADR-018 §2: one deliberate escape hatch, not a list. Enforced by the shape
    rather than by a rule, so it cannot be relaxed by changing a validator. */
    var fields =
        java.util.Arrays.stream(TenantEntity.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .filter(name -> name.toLowerCase().contains("freenotice"))
            .toList();

    assertThat(fields).hasSize(1);
  }

  @Test
  void theAuthoredBausteineAreTheFiveTheTraegerOwns() {
    /* Derived Bausteine are never editable (ADR-018 §2 guardrail): no column exists
    for the response deadline *text*, for "send us no personal data", or for the
    modality note — what the system knows, the system renders. */
    var authored =
        java.util.Arrays.stream(TenantEntity.class.getDeclaredFields())
            .map(java.lang.reflect.Field::getName)
            .filter(name -> name.startsWith("contentErstantwort"))
            .sorted()
            .toList();

    assertThat(authored)
        .containsExactly(
            "contentErstantwortClosing",
            "contentErstantwortEmergencyAddition",
            "contentErstantwortFreeNotice",
            "contentErstantwortGreeting",
            "contentErstantwortWhoReadsAlong");
  }
}
