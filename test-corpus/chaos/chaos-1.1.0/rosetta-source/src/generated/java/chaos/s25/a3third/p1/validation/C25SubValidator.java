package chaos.s25.a3third.p1.validation;

import chaos.s25.a3third.p1.C25Sub;
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

public class C25SubValidator implements Validator<C25Sub> {

	private List<ComparisonResult> getComparisonResults(C25Sub o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("sname", (String) o.getSname() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Sub o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25Sub", ValidationResult.ValidationType.CARDINALITY, "C25Sub", path, "", res.getError());
				}
				return success("C25Sub", ValidationResult.ValidationType.CARDINALITY, "C25Sub", path, "");
			})
			.collect(toList());
	}

}
