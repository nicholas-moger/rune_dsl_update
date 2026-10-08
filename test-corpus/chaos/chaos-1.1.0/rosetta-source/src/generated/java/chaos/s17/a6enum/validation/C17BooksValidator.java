package chaos.s17.a6enum.validation;

import chaos.s17.a6enum.C17ActionEnum;
import chaos.s17.a6enum.C17Books;
import chaos.s17.a6enum.C17Held;
import chaos.s17.a6enum.C17SideEnum;
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

public class C17BooksValidator implements Validator<C17Books> {

	private List<ComparisonResult> getComparisonResults(C17Books o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("side", (C17SideEnum) o.getSide() != null ? 1 : 0, 0, 1), 
				checkCardinality("action", (C17ActionEnum) o.getAction() != null ? 1 : 0, 0, 1), 
				checkCardinality("held", (C17Held) o.getHeld() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C17Books o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C17Books", ValidationResult.ValidationType.CARDINALITY, "C17Books", path, "", res.getError());
				}
				return success("C17Books", ValidationResult.ValidationType.CARDINALITY, "C17Books", path, "");
			})
			.collect(toList());
	}

}
