package holdout.argcoercionbarelocal.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.argcoercionbarelocal.Holder;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class HolderTypeFormatValidator implements Validator<Holder> {

	private List<ComparisonResult> getComparisonResults(Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("tally", o.getTally(), empty(), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Holder", ValidationResult.ValidationType.TYPE_FORMAT, "Holder", path, "", res.getError());
				}
				return success("Holder", ValidationResult.ValidationType.TYPE_FORMAT, "Holder", path, "");
			})
			.collect(toList());
	}

}
