package chaos.s25.a5mixed.validation;

import chaos.s25.a5mixed.C25Paths;
import chaos.s25.a5mixed.C25Pick;
import chaos.s25.a5mixed.C25Sub;
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

public class C25PathsValidator implements Validator<C25Paths> {

	private List<ComparisonResult> getComparisonResults(C25Paths o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("p", (String) o.getP() != null ? 1 : 0, 0, 1), 
				checkCardinality("q", (String) o.getQ() != null ? 1 : 0, 0, 1), 
				checkCardinality("sub", (C25Sub) o.getSub() != null ? 1 : 0, 0, 1), 
				checkCardinality("pick", (C25Pick) o.getPick() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Paths o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25Paths", ValidationResult.ValidationType.CARDINALITY, "C25Paths", path, "", res.getError());
				}
				return success("C25Paths", ValidationResult.ValidationType.CARDINALITY, "C25Paths", path, "");
			})
			.collect(toList());
	}

}
