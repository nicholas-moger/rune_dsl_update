package test.aliascond.validation;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.aliascond.Holder;
import test.aliascond.validation.datarule.CheckedOk;
import test.aliascond.validation.datarule.Code3NotPlaceholder;
import test.aliascond.validation.datarule.EvenNatNonNeg;
import test.aliascond.validation.datarule.FlagMustHold;
import test.aliascond.validation.datarule.NestedSmall;
import test.aliascond.validation.datarule.PctCapped;
import test.aliascond.validation.datarule.PctNotHalf;
import test.aliascond.validation.datarule.UnnamedDataRule0;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class HolderTypeFormatValidator implements Validator<Holder> {
	@Inject
	protected EvenNatNonNeg evenNatNonNeg;
	@Inject
	protected PctCapped pctCapped;
	@Inject
	protected PctNotHalf pctNotHalf;
	@Inject
	protected Code3NotPlaceholder code3NotPlaceholder;
	@Inject
	protected FlagMustHold flagMustHold;
	@Inject
	protected NestedSmall nestedSmall;
	@Inject
	protected UnnamedDataRule0 unnamedDataRule0;
	@Inject
	protected CheckedOk checkedOk;

	private List<ComparisonResult> getComparisonResults(Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("one", o.getOne(), empty(), of(0), empty(), empty()), 
				checkNumber("opt", o.getOpt(), empty(), of(0), empty(), empty()), 
				checkNumber("many", o.getMany(), empty(), of(0), empty(), empty()), 
				checkNumber("some", o.getSome(), empty(), of(0), empty(), empty()), 
				checkNumber("pct", o.getPct(), empty(), empty(), of(new BigDecimal("0")), empty()), 
				checkNumber("pcts", o.getPcts(), empty(), empty(), of(new BigDecimal("0")), empty()), 
				checkString("code", o.getCode(), 3, of(3), empty()), 
				checkNumber("nested", o.getNested(), empty(), of(0), empty(), empty()), 
				checkNumber("nesteds", o.getNesteds(), empty(), of(0), empty(), empty()), 
				checkNumber("unnamed", o.getUnnamed(), empty(), of(0), empty(), empty()), 
				checkNumber("plain", o.getPlain(), empty(), empty(), of(new BigDecimal("0")), empty())
			);
	}
	
	private List<ValidationResult<?>> runConditions(RosettaPath path, Holder o) {
		List<ValidationResult<?>> results = new ArrayList();
		results.addAll(evenNatNonNeg.getValidationResults(path.newSubPath("one"), o.getOne()));
		results.addAll(evenNatNonNeg.getValidationResults(path.newSubPath("opt"), o.getOpt()));
		final List<Integer> many = o.getMany();
		if (many != null) {
			for (int i0 = 0; i0 < many.size(); i0++) {
				results.addAll(evenNatNonNeg.getValidationResults(path.newSubPath("many").withIndex(i0), many.get(i0)));
			}
		}
		final List<Integer> some = o.getSome();
		if (some != null) {
			for (int i1 = 0; i1 < some.size(); i1++) {
				results.addAll(evenNatNonNeg.getValidationResults(path.newSubPath("some").withIndex(i1), some.get(i1)));
			}
		}
		results.addAll(pctCapped.getValidationResults(path.newSubPath("pct"), o.getPct()));
		results.addAll(pctNotHalf.getValidationResults(path.newSubPath("pct"), o.getPct()));
		final List<BigDecimal> pcts = o.getPcts();
		if (pcts != null) {
			for (int i2 = 0; i2 < pcts.size(); i2++) {
				results.addAll(pctCapped.getValidationResults(path.newSubPath("pcts").withIndex(i2), pcts.get(i2)));
				results.addAll(pctNotHalf.getValidationResults(path.newSubPath("pcts").withIndex(i2), pcts.get(i2)));
			}
		}
		results.addAll(code3NotPlaceholder.getValidationResults(path.newSubPath("code"), o.getCode()));
		results.addAll(flagMustHold.getValidationResults(path.newSubPath("flag"), o.getFlag()));
		results.addAll(nestedSmall.getValidationResults(path.newSubPath("nested"), o.getNested()));
		results.addAll(evenNatNonNeg.getValidationResults(path.newSubPath("nested"), o.getNested()));
		final List<Integer> nesteds = o.getNesteds();
		if (nesteds != null) {
			for (int i3 = 0; i3 < nesteds.size(); i3++) {
				results.addAll(nestedSmall.getValidationResults(path.newSubPath("nesteds").withIndex(i3), nesteds.get(i3)));
				results.addAll(evenNatNonNeg.getValidationResults(path.newSubPath("nesteds").withIndex(i3), nesteds.get(i3)));
			}
		}
		results.addAll(unnamedDataRule0.getValidationResults(path.newSubPath("unnamed"), o.getUnnamed()));
		results.addAll(checkedOk.getValidationResults(path.newSubPath("checked"), o.getChecked()));
		return results;
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder o) {
		return Streams.concat(getComparisonResults(o)
				.stream()
				.map(res -> {
					if (!isNullOrEmpty(res.getError())) {
						return failure("Holder", ValidationResult.ValidationType.TYPE_FORMAT, "Holder", path, "", res.getError());
					}
					return success("Holder", ValidationResult.ValidationType.TYPE_FORMAT, "Holder", path, "");
				}),
				runConditions(path, o).stream()
			)
			.collect(toList());
	}

}
