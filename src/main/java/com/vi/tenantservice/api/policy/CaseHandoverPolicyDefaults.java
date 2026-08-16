package com.vi.tenantservice.api.policy;

import com.vi.tenantservice.api.model.BooleanPermissionPolicy;
import com.vi.tenantservice.api.model.CaseHandoverPolicies;
import com.vi.tenantservice.api.model.CaseHandoverReasonPolicy;
import com.vi.tenantservice.api.model.IntegerPermissionPolicy;
import com.vi.tenantservice.api.model.MultilingualTextPermissionPolicy;
import com.vi.tenantservice.api.model.PermissionPolicyMode;
import com.vi.tenantservice.api.model.StringListPermissionPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Platform defaults for the stable Case Handover reason codes. */
public final class CaseHandoverPolicyDefaults {

  public static final String ADVICE_NEEDED = "COUNSELLOR_ASKED_FOR_ADVICE";

  private CaseHandoverPolicyDefaults() {}

  public static CaseHandoverPolicies create() {
    Map<String, CaseHandoverReasonPolicy> reasons = new LinkedHashMap<>();
    reasons.put(
        ADVICE_NEEDED,
        reason(
                CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_ASKED_FOR_ADVICE,
                Map.of("de", "Rat benötigt", "en", "Advice needed"),
                true,
                Set.of("CLIENT"),
                templates(
                    "Du hast einem zeitlich begrenzten Einblick zugestimmt. {{newAdvisor}} kann diese Sitzung für {{duration}} mitlesen. Deine bisherige Berater:in bleibt für dich zuständig.",
                    "You agreed to a time-limited review. {{newAdvisor}} can read this session for {{duration}}. Your current counsellor remains responsible for you.",
                    "Vous avez accepté une consultation temporaire. {{newAdvisor}} peut consulter cette session pendant {{duration}}. Votre conseiller·ère habituel·le reste responsable de votre accompagnement.",
                    "Вы согласились на временный просмотр консультации. {{newAdvisor}} может просматривать её в течение {{duration}}. Ваш текущий консультант по-прежнему отвечает за ваше консультирование.",
                    "Süreli incelemeyi onayladınız. {{newAdvisor}} bu oturumu {{duration}} boyunca okuyabilir. Mevcut danışmanınız sizden sorumlu olmaya devam eder.",
                    "Ви погодилися на тимчасовий перегляд консультації. {{newAdvisor}} може читати цю сесію протягом {{duration}}. Ваш поточний консультант залишається відповідальним за вас.",
                    "ንግዜኡ ዝተወሰነ ምርኣይ ተሰማሚዕኩም። {{newAdvisor}} ነዚ ክፍለ ግዜ ን{{duration}} ከንብቦ ይኽእል። እቲ ሕጂ ዘሎ ኣማኻሪኹም ብሓላፍነት ይቕጽል።"))
            .maxAccessDurationMinutes(
                new IntegerPermissionPolicy(
                    CaseHandoverDurationPolicy.DEFAULT_MINUTES, PermissionPolicyMode.SUGGESTED)));
    reasons.put(
        "COUNSELLOR_ON_HOLIDAY",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_ON_HOLIDAY,
            Map.of("de", "Freistellung", "en", "Leave"),
            false,
            Set.of(),
            templates(
                "Während der Abwesenheit übernimmt {{newAdvisor}} deine Beratung.",
                "During the absence, {{newAdvisor}} takes over your counselling.",
                "Pendant l’absence de votre conseiller·ère, {{newAdvisor}} prend en charge votre accompagnement.",
                "На время отсутствия вашего консультанта {{newAdvisor}} продолжит вашу консультацию.",
                "Danışmanınızın yokluğunda danışmanlığınızı {{newAdvisor}} devralır.",
                "Під час відсутності вашого консультанта ваше консультування продовжить {{newAdvisor}}.",
                "ኣማኻሪኹም ኣብ ዘየለሉ ግዜ፣ {{newAdvisor}} ምኽርኹም ይቕጽል።")));
    reasons.put(
        "OTHER_EMERGENCY",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.OTHER_EMERGENCY,
            Map.of("de", "Anderer Notfall", "en", "Other emergency"),
            false,
            Set.of(),
            templates(
                "Aus einem dringenden Grund hat {{newAdvisor}} deinen Fall übernommen.",
                "For an urgent reason, {{newAdvisor}} has taken over your case.",
                "Pour une raison urgente, {{newAdvisor}} a repris votre dossier.",
                "По неотложной причине {{newAdvisor}} принял(а) ваше дело.",
                "Acil bir nedenden dolayı vakanızı {{newAdvisor}} devraldı.",
                "З невідкладної причини вашу справу перейняв(-ла) {{newAdvisor}}.",
                "ብህጹጽ ምኽንያት፣ {{newAdvisor}} ጉዳይኩም ተረኪቡ።")));
    reasons.put(
        "COUNSELLOR_IS_ILL",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_IS_ILL,
            Map.of("de", "Ausfall", "en", "Absence"),
            false,
            Set.of(),
            templates(
                "Wegen eines Ausfalls hat {{newAdvisor}} deinen Fall übernommen.",
                "Because of an absence, {{newAdvisor}} has taken over your case.",
                "En raison d’une absence, {{newAdvisor}} a repris votre dossier.",
                "Из-за отсутствия вашего консультанта {{newAdvisor}} принял(а) ваше дело.",
                "Danışmanınızın yokluğu nedeniyle vakanızı {{newAdvisor}} devraldı.",
                "Через відсутність вашого консультанта вашу справу перейняв(-ла) {{newAdvisor}}.",
                "ኣማኻሪኹም ብምብኳሩ፣ {{newAdvisor}} ጉዳይኩም ተረኪቡ።")));
    reasons.put(
        "COUNSELLOR_LEFT",
        reason(
            CaseHandoverReasonPolicy.CodeEnum.COUNSELLOR_LEFT,
            Map.of("de", "Beratungsstelle verlassen", "en", "Counsellor left"),
            false,
            Set.of(),
            templates(
                "{{newAdvisor}} führt deine Beratung ab jetzt weiter.",
                "{{newAdvisor}} will continue your counselling from now on.",
                "{{newAdvisor}} poursuivra désormais votre accompagnement.",
                "Теперь вашу консультацию продолжит {{newAdvisor}}.",
                "Danışmanlığınıza bundan sonra {{newAdvisor}} devam edecek.",
                "Відтепер ваше консультування продовжить {{newAdvisor}}.",
                "ካብ ሕጂ ንደሓር {{newAdvisor}} ምኽርኹም ይቕጽል።")));
    return new CaseHandoverPolicies(Map.copyOf(reasons));
  }

  private static CaseHandoverReasonPolicy reason(
      CaseHandoverReasonPolicy.CodeEnum code,
      Map<String, String> labels,
      boolean clientConsentRequired,
      Set<String> approvalRoles,
      Map<String, String> templates) {
    return new CaseHandoverReasonPolicy(
        code,
        multilingual(labels),
        bool(true),
        bool(true),
        bool(clientConsentRequired),
        new StringListPermissionPolicy(approvalRoles, PermissionPolicyMode.SUGGESTED),
        multilingual(templates));
  }

  private static BooleanPermissionPolicy bool(boolean value) {
    return new BooleanPermissionPolicy(value, PermissionPolicyMode.SUGGESTED);
  }

  private static MultilingualTextPermissionPolicy multilingual(Map<String, String> value) {
    return new MultilingualTextPermissionPolicy(value, PermissionPolicyMode.SUGGESTED);
  }

  private static Map<String, String> templates(
      String german,
      String english,
      String french,
      String russian,
      String turkish,
      String ukrainian,
      String tigrinya) {
    return Map.of(
        "de", german,
        "en", english,
        "fr", french,
        "ru", russian,
        "tr", turkish,
        "uk", ukrainian,
        "ti", tigrinya);
  }
}
