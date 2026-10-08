package chaos.s11.a1o1.validation;

import chaos.s11.a1o1.C11Aux;
import chaos.s11.a1o1.C11Mapped;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C11MappedValidator implements Validator<C11Mapped> {

	private List<ComparisonResult> getComparisonResults(C11Mapped o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("ident", (String) o.getIdent() != null ? 1 : 0, 1, 1), 
				checkCardinality("total", (BigDecimal) o.getTotal() != null ? 1 : 0, 0, 1), 
				checkCardinality("kindCode", (String) o.getKindCode() != null ? 1 : 0, 0, 1), 
				checkCardinality("flagged", (String) o.getFlagged() != null ? 1 : 0, 0, 1), 
				checkCardinality("tagged", (String) o.getTagged() != null ? 1 : 0, 0, 1), 
				checkCardinality("metaCarrier", (String) o.getMetaCarrier() != null ? 1 : 0, 0, 1), 
				checkCardinality("defaulted", (String) o.getDefaulted() != null ? 1 : 0, 0, 1), 
				checkCardinality("tested", (String) o.getTested() != null ? 1 : 0, 0, 1), 
				checkCardinality("pathed", (String) o.getPathed() != null ? 1 : 0, 0, 1), 
				checkCardinality("dated", (Date) o.getDated() != null ? 1 : 0, 0, 1), 
				checkCardinality("patterned", (String) o.getPatterned() != null ? 1 : 0, 0, 1), 
				checkCardinality("metaOnly", (String) o.getMetaOnly() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C11Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C11Mapped o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C11Mapped", ValidationResult.ValidationType.CARDINALITY, "C11Mapped", path, "", res.getError());
				}
				return success("C11Mapped", ValidationResult.ValidationType.CARDINALITY, "C11Mapped", path, "");
			})
			.collect(toList());
	}

}
