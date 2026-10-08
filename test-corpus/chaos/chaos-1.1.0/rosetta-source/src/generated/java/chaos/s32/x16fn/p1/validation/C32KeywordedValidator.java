package chaos.s32.x16fn.p1.validation;

import chaos.s32.x16fn.p1.C32Aux;
import chaos.s32.x16fn.p1.C32Keyworded;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C32KeywordedValidator implements Validator<C32Keyworded> {

	private List<ComparisonResult> getComparisonResults(C32Keyworded o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("class", (String) o._getClass() != null ? 1 : 0, 0, 1), 
				checkCardinality("long", (BigDecimal) o.getLong() != null ? 1 : 0, 0, 1), 
				checkCardinality("char", (String) o.getChar() != null ? 1 : 0, 0, 1), 
				checkCardinality("this", (String) o.getThis() != null ? 1 : 0, 0, 1), 
				checkCardinality("private", (String) o.getPrivate() != null ? 1 : 0, 0, 1), 
				checkCardinality("return", (BigDecimal) o.getReturn() != null ? 1 : 0, 0, 1), 
				checkCardinality("package", (String) o.getPackage() != null ? 1 : 0, 0, 1), 
				checkCardinality("null", (String) o.getNull() != null ? 1 : 0, 0, 1), 
				checkCardinality("abstract", (Boolean) o.getAbstract() != null ? 1 : 0, 0, 1), 
				checkCardinality("throws", (String) o.getThrows() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C32Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C32Keyworded o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C32Keyworded", ValidationResult.ValidationType.CARDINALITY, "C32Keyworded", path, "", res.getError());
				}
				return success("C32Keyworded", ValidationResult.ValidationType.CARDINALITY, "C32Keyworded", path, "");
			})
			.collect(toList());
	}

}
