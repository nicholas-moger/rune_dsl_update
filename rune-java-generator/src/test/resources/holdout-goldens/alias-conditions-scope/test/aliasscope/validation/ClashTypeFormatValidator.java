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
import test.aliasscope.Clash;
import test.aliasscope.validation.datarule.NatNonNeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class ClashTypeFormatValidator implements Validator<Clash> {
	@Inject
	protected NatNonNeg natNonNeg;

	private List<ComparisonResult> getComparisonResults(Clash o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("results", o.getResults(), empty(), of(0), empty(), empty()), 
				checkNumber("o", o.getO(), empty(), of(0), empty(), empty()), 
				checkNumber("i", o.getI(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Clash o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> _results = o.getResults();
		if (_results != null) {
			for (int i0 = 0; i0 < _results.size(); i0++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("results").withIndex(i0), _results.get(i0)));
			}
		}
		final List<Integer> _o = o.getO();
		if (_o != null) {
			for (int i1 = 0; i1 < _o.size(); i1++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("o").withIndex(i1), _o.get(i1)));
			}
		}
		final List<Integer> i3 = o.getI();
		if (i3 != null) {
			for (int i2 = 0; i2 < i3.size(); i2++) {
				results.addAll(natNonNeg.getValidationResults(path.newSubPath("i").withIndex(i2), i3.get(i2)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Clash o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Clash", ValidationResult.ValidationType.TYPE_FORMAT, "Clash", path, "", res.getError());
					}
					return success("Clash", ValidationResult.ValidationType.TYPE_FORMAT, "Clash", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
