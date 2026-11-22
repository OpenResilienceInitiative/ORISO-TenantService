package com.vi.tenantservice.api.service.search;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.type.StandardBasicTypes;

/** Narrow expression collation: existing storage and unrelated queries remain unchanged. */
public final class SearchTextFunctions implements FunctionContributor {
  @Override
  public void contributeFunctions(FunctionContributions contributions) {
    String expression =
        contributions.getDialect() instanceof MariaDBDialect
            ? "(regexp_replace(convert(?1 using utf8mb4),concat(char(92),'p{M}'),'') collate utf8mb4_uca1400_ai_ci)"
            : "lower(?1)";
    // H2 is useful for scope/paging regressions, but only MariaDB certifies accent semantics.
    contributions
        .getFunctionRegistry()
        .registerPattern(
            "admin_search_text",
            expression,
            contributions
                .getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.STRING));
  }
}
