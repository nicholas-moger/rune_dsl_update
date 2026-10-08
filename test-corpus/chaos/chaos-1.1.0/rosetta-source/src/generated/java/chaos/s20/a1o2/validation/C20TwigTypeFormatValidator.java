package chaos.s20.a1o2.validation;

import chaos.s20.a1o2.C20Twig;
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

public class C20TwigTypeFormatValidator implements Validator<C20Twig> {

	private List<ComparisonResult> getComparisonResults(C20Twig o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C20Twig o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C20Twig", ValidationResult.ValidationType.TYPE_FORMAT, "C20Twig", path, "", res.getError());
				}
				return success("C20Twig", ValidationResult.ValidationType.TYPE_FORMAT, "C20Twig", path, "");
			})
			.collect(toList());
	}

}
