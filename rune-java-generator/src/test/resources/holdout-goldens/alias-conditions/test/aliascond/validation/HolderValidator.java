package test.aliascond.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.aliascond.Holder;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderValidator implements Validator<Holder> {

	private List<ComparisonResult> getComparisonResults(Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("one", (Integer) o.getOne() != null ? 1 : 0, 1, 1), 
				checkCardinality("opt", (Integer) o.getOpt() != null ? 1 : 0, 0, 1), 
				checkCardinality("some", (List<Integer>) o.getSome() == null ? 0 : o.getSome().size(), 1, 0), 
				checkCardinality("pct", (BigDecimal) o.getPct() != null ? 1 : 0, 0, 1), 
				checkCardinality("code", (String) o.getCode() != null ? 1 : 0, 0, 1), 
				checkCardinality("flag", (Boolean) o.getFlag() != null ? 1 : 0, 0, 1), 
				checkCardinality("nested", (Integer) o.getNested() != null ? 1 : 0, 0, 1), 
				checkCardinality("unnamed", (Integer) o.getUnnamed() != null ? 1 : 0, 0, 1), 
				checkCardinality("checked", (BigDecimal) o.getChecked() != null ? 1 : 0, 0, 1), 
				checkCardinality("plain", (BigDecimal) o.getPlain() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Holder", ValidationResult.ValidationType.CARDINALITY, "Holder", path, "", res.getError());
				}
				return success("Holder", ValidationResult.ValidationType.CARDINALITY, "Holder", path, "");
			})
			.collect(toList());
	}

}
