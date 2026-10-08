package test.aliasscope.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliasscope.FieldClash;
import test.aliasscope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class FieldClashTypeFormatValidator implements Validator<FieldClash> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(FieldClash o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("natNonNeg", o.getNatNonNeg(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, FieldClash o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> _natNonNeg = o.getNatNonNeg();
		if (_natNonNeg != null) {
			for (int i = 0; i < _natNonNeg.size(); i++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("natNonNeg").withIndex(i), _natNonNeg.get(i)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, FieldClash o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("FieldClash", ValidationResult.ValidationType.TYPE_FORMAT, "FieldClash", path, "", res.getError());
					}
					return success("FieldClash", ValidationResult.ValidationType.TYPE_FORMAT, "FieldClash", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
