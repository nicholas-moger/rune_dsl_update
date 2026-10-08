package chaos.s99.base.validation;

import chaos.s99.base.C99Holder;
import chaos.s99.base.validation.datarule.C99NatNonNeg;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class C99HolderTypeFormatValidator implements Validator<C99Holder> {
	@Inject
	protected chaos.s99.base.validation.datarule.Integer integer;
	@Inject
	protected C99NatNonNeg c99NatNonNeg;

	private List<ComparisonResult> getComparisonResults(C99Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("ig", o.getIg(), empty(), of(0), empty(), empty()), 
				checkNumber("n", o.getN(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, C99Holder o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> ig = o.getIg();
		if (ig != null) {
			for (int i = 0; i < ig.size(); i++) {
				results.addAll(integer.getValidationResults(path.newSubPath("ig").withIndex(i), ig.get(i)));
			}
		}
		results.addAll(c99NatNonNeg.getValidationResults(path.newSubPath("n"), o.getN()));
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C99Holder o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("C99Holder", ValidationResult.ValidationType.TYPE_FORMAT, "C99Holder", path, "", res.getError());
					}
					return success("C99Holder", ValidationResult.ValidationType.TYPE_FORMAT, "C99Holder", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
