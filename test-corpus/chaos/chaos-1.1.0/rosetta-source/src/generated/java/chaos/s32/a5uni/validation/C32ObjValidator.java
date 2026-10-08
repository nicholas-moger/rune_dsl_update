package chaos.s32.a5uni.validation;

import chaos.s32.a5uni.C32Obj;
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

public class C32ObjValidator implements Validator<C32Obj> {

	private List<ComparisonResult> getComparisonResults(C32Obj o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("o", (String) o.getO() != null ? 1 : 0, 0, 1), 
				checkCardinality("Class", (String) o._getClass() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C32Obj o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C32Obj", ValidationResult.ValidationType.CARDINALITY, "C32Obj", path, "", res.getError());
				}
				return success("C32Obj", ValidationResult.ValidationType.CARDINALITY, "C32Obj", path, "");
			})
			.collect(toList());
	}

}
