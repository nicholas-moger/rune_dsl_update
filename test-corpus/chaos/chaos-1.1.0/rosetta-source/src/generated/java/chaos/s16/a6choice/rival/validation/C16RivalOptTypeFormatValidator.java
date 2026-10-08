package chaos.s16.a6choice.rival.validation;

import chaos.s16.a6choice.rival.C16RivalOpt;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C16RivalOptTypeFormatValidator implements Validator<C16RivalOpt> {

	private List<ComparisonResult> getComparisonResults(C16RivalOpt o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16RivalOpt o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16RivalOpt", ValidationResult.ValidationType.TYPE_FORMAT, "C16RivalOpt", path, "", res.getError());
				}
				return success("C16RivalOpt", ValidationResult.ValidationType.TYPE_FORMAT, "C16RivalOpt", path, "");
			})
			.collect(toList());
	}

}
