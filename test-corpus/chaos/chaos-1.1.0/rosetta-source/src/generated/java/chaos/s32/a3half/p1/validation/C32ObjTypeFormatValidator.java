package chaos.s32.a3half.p1.validation;

import chaos.s32.a3half.p1.C32Obj;
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

public class C32ObjTypeFormatValidator implements Validator<C32Obj> {

	private List<ComparisonResult> getComparisonResults(C32Obj o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C32Obj o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C32Obj", ValidationResult.ValidationType.TYPE_FORMAT, "C32Obj", path, "", res.getError());
				}
				return success("C32Obj", ValidationResult.ValidationType.TYPE_FORMAT, "C32Obj", path, "");
			})
			.collect(toList());
	}

}
