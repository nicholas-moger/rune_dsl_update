package chaos.s17.a2dangle.validation.datarule;

import chaos.s17.a2dangle.C17ActionEnum;
import chaos.s17.a2dangle.C17Books;
import chaos.s17.a2dangle.C17SideEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("C17BooksC17Agree")
@ImplementedBy(C17BooksC17Agree.Default.class)
public interface C17BooksC17Agree extends Validator<C17Books> {
	
	String NAME = "C17BooksC17Agree";
	String DEFINITION = "if side exists and action exists then (side = C17SideEnum -> Buy and action = C17ActionEnum -> Buy) or side = C17SideEnum -> Sell or action = C17ActionEnum -> Amend";
	
	class Default implements C17BooksC17Agree {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C17Books c17Books) {
			ComparisonResult result = executeDataRule(c17Books);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C17Books", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C17Books", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C17Books c17Books) {
			try {
				if (exists(MapperS.of(c17Books).<C17SideEnum>map("getSide", _c17Books -> _c17Books.getSide())).andNullSafe(exists(MapperS.of(c17Books).<C17ActionEnum>map("getAction", _c17Books -> _c17Books.getAction()))).getOrDefault(false)) {
					return areEqual(MapperS.of(c17Books).<C17SideEnum>map("getSide", _c17Books -> _c17Books.getSide()), MapperS.of(C17SideEnum.BUY), CardinalityOperator.All).andNullSafe(areEqual(MapperS.of(c17Books).<C17ActionEnum>map("getAction", _c17Books -> _c17Books.getAction()), MapperS.of(C17ActionEnum.BUY), CardinalityOperator.All)).orNullSafe(areEqual(MapperS.of(c17Books).<C17SideEnum>map("getSide", _c17Books -> _c17Books.getSide()), MapperS.of(C17SideEnum.SELL), CardinalityOperator.All)).orNullSafe(areEqual(MapperS.of(c17Books).<C17ActionEnum>map("getAction", _c17Books -> _c17Books.getAction()), MapperS.of(C17ActionEnum.AMEND), CardinalityOperator.All));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C17BooksC17Agree {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C17Books c17Books) {
			return Collections.emptyList();
		}
	}
}
