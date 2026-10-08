package chaos.s16.a6choice.rival.validation;

import chaos.s16.a6choice.rival.C16Pick;
import chaos.s16.a6choice.rival.C16RivalOpt;
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

public class C16PickValidator implements Validator<C16Pick> {

	private List<ComparisonResult> getComparisonResults(C16Pick o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C16RivalOpt", (C16RivalOpt) o.getC16RivalOpt() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Pick o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16Pick", ValidationResult.ValidationType.CARDINALITY, "C16Pick", path, "", res.getError());
				}
				return success("C16Pick", ValidationResult.ValidationType.CARDINALITY, "C16Pick", path, "");
			})
			.collect(toList());
	}

}
