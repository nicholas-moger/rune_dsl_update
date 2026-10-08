package test.chswitchedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class EitherValidator implements Validator<Either> {

	private List<ComparisonResult> getComparisonResults(Either o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("OptA", (OptA) o.getOptA() != null ? 1 : 0, 0, 1), 
				checkCardinality("OptB", (OptB) o.getOptB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Either o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Either", ValidationResult.ValidationType.CARDINALITY, "Either", path, "", res.getError());
				}
				return success("Either", ValidationResult.ValidationType.CARDINALITY, "Either", path, "");
			})
			.collect(toList());
	}

}
