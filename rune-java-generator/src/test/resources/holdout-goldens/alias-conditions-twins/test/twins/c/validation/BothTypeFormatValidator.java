package test.twins.c.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.twins.a.validation.datarule.EvenNatNonNeg;
import test.twins.c.Both;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class BothTypeFormatValidator implements Validator<Both> {
	@Inject
	protected EvenNatNonNeg evenNatNonNeg0;
	@Inject
	protected test.twins.b.validation.datarule.EvenNatNonNeg evenNatNonNeg1;

	private List<ComparisonResult> getComparisonResults(Both o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("left", o.getLeft(), empty(), of(0), empty(), empty()), 
				checkNumber("right", o.getRight(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Both o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> left = o.getLeft();
		if (left != null) {
			for (int i = 0; i < left.size(); i++) {
				results.addAll(evenNatNonNeg0.getValidationResults(path.newSubPath("left").withIndex(i), left.get(i)));
			}
		}
		results.addAll(evenNatNonNeg1.getValidationResults(path.newSubPath("right"), o.getRight()));
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Both o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Both", ValidationResult.ValidationType.TYPE_FORMAT, "Both", path, "", res.getError());
					}
					return success("Both", ValidationResult.ValidationType.TYPE_FORMAT, "Both", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
