package test.chswitchedge.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import test.chswitchedge.Bag;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("BagTexts")
@ImplementedBy(BagTexts.Default.class)
public interface BagTexts extends Validator<Bag> {
	
	String NAME = "BagTexts";
	String DEFINITION = "if eths exists then (eths extract [ item switch OptA then item -> av, OptB then item -> bv to-string, default \"none\" ] then count) <= eths count";
	
	class Default implements BagTexts {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Bag bag) {
			ComparisonResult result = executeDataRule(bag);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Bag", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Bag", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Bag bag) {
			try {
				if (exists(MapperS.of(bag).<Either>mapC("getEths", _bag -> _bag.getEths())).getOrDefault(false)) {
					final MapperC<String> thenArg = MapperS.of(bag).<Either>mapC("getEths", _bag -> _bag.getEths())
						.mapItem(item -> {
							if (item.get() == null) {
								return MapperS.<String>ofNull();
							}
							if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
								final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
								return optA.<String>map("getAv", _optA -> _optA.getAv());
							}
							if (item.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
								final MapperS<OptB> optB = item.<OptB>map("getOptB", either -> either.getOptB());
								return optB.<BigDecimal>map("getBv", _optB -> _optB.getBv()).map("to-string", Object::toString);
							}
							return MapperS.of("none");
						});
					return lessThanEquals(MapperS.of(thenArg.resultCount()), MapperS.of(MapperS.of(bag).<Either>mapC("getEths", _bag -> _bag.getEths()).resultCount()), CardinalityOperator.All);
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements BagTexts {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Bag bag) {
			return Collections.emptyList();
		}
	}
}
