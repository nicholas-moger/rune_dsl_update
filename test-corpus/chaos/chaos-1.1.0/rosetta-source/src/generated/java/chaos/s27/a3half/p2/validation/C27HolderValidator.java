package chaos.s27.a3half.p2.validation;

import chaos.s27.a3half.p1.C27Ref;
import chaos.s27.a3half.p2.C27Holder;
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

public class C27HolderValidator implements Validator<C27Holder> {

	private List<ComparisonResult> getComparisonResults(C27Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("vr", (Integer) o.getVr() != null ? 1 : 0, 0, 1), 
				checkCardinality("h", (Integer) o.getH() != null ? 1 : 0, 0, 1), 
				checkCardinality("ref", (C27Ref) o.getRef() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C27Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C27Holder", ValidationResult.ValidationType.CARDINALITY, "C27Holder", path, "", res.getError());
				}
				return success("C27Holder", ValidationResult.ValidationType.CARDINALITY, "C27Holder", path, "");
			})
			.collect(toList());
	}

}
