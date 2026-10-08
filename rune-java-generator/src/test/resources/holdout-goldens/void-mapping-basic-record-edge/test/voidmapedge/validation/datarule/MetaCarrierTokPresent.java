package test.voidmapedge.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import test.voidmapedge.MetaCarrier;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("MetaCarrierTokPresent")
@ImplementedBy(MetaCarrierTokPresent.Default.class)
public interface MetaCarrierTokPresent extends Validator<MetaCarrier> {
	
	String NAME = "MetaCarrierTokPresent";
	String DEFINITION = "if flag = True then tok exists";
	
	class Default implements MetaCarrierTokPresent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, MetaCarrier metaCarrier) {
			ComparisonResult result = executeDataRule(metaCarrier);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "MetaCarrier", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "MetaCarrier", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(MetaCarrier metaCarrier) {
			try {
				if (areEqual(MapperS.of(metaCarrier).<Boolean>map("getFlag", _metaCarrier -> _metaCarrier.getFlag()), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
					return exists(MapperS.of(metaCarrier).<FieldWithMetaVoid>map("getTok", _metaCarrier -> _metaCarrier.getTok()));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements MetaCarrierTokPresent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, MetaCarrier metaCarrier) {
			return Collections.emptyList();
		}
	}
}
