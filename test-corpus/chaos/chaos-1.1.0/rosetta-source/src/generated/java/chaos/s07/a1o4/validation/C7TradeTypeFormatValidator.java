package chaos.s07.a1o4.validation;

import chaos.s07.a1o4.C7Trade;
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

public class C7TradeTypeFormatValidator implements Validator<C7Trade> {

	private List<ComparisonResult> getComparisonResults(C7Trade o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7Trade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7Trade", ValidationResult.ValidationType.TYPE_FORMAT, "C7Trade", path, "", res.getError());
				}
				return success("C7Trade", ValidationResult.ValidationType.TYPE_FORMAT, "C7Trade", path, "");
			})
			.collect(toList());
	}

}
