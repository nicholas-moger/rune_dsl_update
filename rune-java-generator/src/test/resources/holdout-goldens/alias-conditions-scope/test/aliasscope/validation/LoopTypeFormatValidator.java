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
import test.aliasscope.Loop;
import test.aliasscope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class LoopTypeFormatValidator implements Validator<Loop> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(Loop o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("i", o.getI(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Loop o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> i1 = o.getI();
		if (i1 != null) {
			for (int i0 = 0; i0 < i1.size(); i0++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("i").withIndex(i0), i1.get(i0)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Loop o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Loop", ValidationResult.ValidationType.TYPE_FORMAT, "Loop", path, "", res.getError());
					}
					return success("Loop", ValidationResult.ValidationType.TYPE_FORMAT, "Loop", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
