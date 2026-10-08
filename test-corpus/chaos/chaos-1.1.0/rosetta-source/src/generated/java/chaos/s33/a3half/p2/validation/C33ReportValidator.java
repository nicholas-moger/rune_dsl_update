package chaos.s33.a3half.p2.validation;

import chaos.s33.a3half.p2.C33Report;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C33ReportValidator implements Validator<C33Report> {

	private List<ComparisonResult> getComparisonResults(C33Report o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utiField", (String) o.getUtiField() != null ? 1 : 0, 1, 1), 
				checkCardinality("litField", (String) o.getLitField() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C33Report o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C33Report", ValidationResult.ValidationType.CARDINALITY, "C33Report", path, "", res.getError());
				}
				return success("C33Report", ValidationResult.ValidationType.CARDINALITY, "C33Report", path, "");
			})
			.collect(toList());
	}

}
