package reg.validation;

import cde.layer.price.NotationEnum;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import reg.RegReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RegReportValidator implements Validator<RegReport> {

	private List<ComparisonResult> getComparisonResults(RegReport o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("notation", (NotationEnum) o.getNotation() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RegReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RegReport", ValidationResult.ValidationType.CARDINALITY, "RegReport", path, "", res.getError());
				}
				return success("RegReport", ValidationResult.ValidationType.CARDINALITY, "RegReport", path, "");
			})
			.collect(toList());
	}

}
