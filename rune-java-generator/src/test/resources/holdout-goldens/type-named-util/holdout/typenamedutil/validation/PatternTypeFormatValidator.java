package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Pattern;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class PatternTypeFormatValidator implements Validator<Pattern> {

	private List<ComparisonResult> getComparisonResults(Pattern o) {
		return Lists.<ComparisonResult>newArrayList(
				checkString("p", o.getP(), 0, empty(), of(java.util.regex.Pattern.compile("[a-z]+")))
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Pattern o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Pattern", ValidationResult.ValidationType.TYPE_FORMAT, "Pattern", path, "", res.getError());
				}
				return success("Pattern", ValidationResult.ValidationType.TYPE_FORMAT, "Pattern", path, "");
			})
			.collect(toList());
	}

}
