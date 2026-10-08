package chaos.s08.a1o1.validation;

import chaos.s08.a1o1.C8Priced;
import chaos.s08.a1o1.validation.datarule.C8EvenC8NonNeg;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class C8PricedTypeFormatValidator implements Validator<C8Priced> {
	@Inject
	protected C8EvenC8NonNeg c8EvenC8NonNeg;

	private List<ComparisonResult> getComparisonResults(C8Priced o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("qty", o.getQty(), empty(), of(0), of(new BigDecimal("0")), empty()), 
				checkString("ccy", o.getCcy(), 3, of(3), of(Pattern.compile("[A-Z]{3}"))), 
				checkNumber("weights", o.getWeights(), empty(), empty(), of(new BigDecimal("0")), of(new BigDecimal("1E+2"))), 
				checkNumber("evens", o.getEvens(), empty(), of(0), empty(), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, C8Priced o) {
		List<ValidationResult<?>> results = new ArrayList();
		final List<Integer> evens = o.getEvens();
		if (evens != null) {
			for (int i = 0; i < evens.size(); i++) {
				results.addAll(c8EvenC8NonNeg.getValidationResults(path.newSubPath("evens").withIndex(i), evens.get(i)));
			}
		}
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C8Priced o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("C8Priced", ValidationResult.ValidationType.TYPE_FORMAT, "C8Priced", path, "", res.getError());
					}
					return success("C8Priced", ValidationResult.ValidationType.TYPE_FORMAT, "C8Priced", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
