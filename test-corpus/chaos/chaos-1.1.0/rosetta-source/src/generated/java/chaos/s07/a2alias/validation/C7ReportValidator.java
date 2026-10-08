package chaos.s07.a2alias.validation;

import chaos.s07.a2alias.C7Report;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C7ReportValidator implements Validator<C7Report> {

	private List<ComparisonResult> getComparisonResults(C7Report o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utiField", (String) o.getUtiField() != null ? 1 : 0, 1, 1), 
				checkCardinality("notionalField", (BigDecimal) o.getNotionalField() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7Report o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7Report", ValidationResult.ValidationType.CARDINALITY, "C7Report", path, "", res.getError());
				}
				return success("C7Report", ValidationResult.ValidationType.CARDINALITY, "C7Report", path, "");
			})
			.collect(toList());
	}

}
