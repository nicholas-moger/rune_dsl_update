package reg.reports;

import base.layer.Instruction;
import cde.layer.price.NotationEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(PriceNotationRule.PriceNotationRuleDefault.class)
public abstract class PriceNotationRule implements ReportFunction<Instruction, NotationEnum> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public NotationEnum evaluate(Instruction input) {
		NotationEnum output = doEvaluate(input);
		
		return output;
	}

	protected abstract NotationEnum doEvaluate(Instruction input);

	public static class PriceNotationRuleDefault extends PriceNotationRule {
		@Override
		protected NotationEnum doEvaluate(Instruction input) {
			NotationEnum output = null;
			return assignOutput(output, input);
		}
		
		protected NotationEnum assignOutput(NotationEnum output, Instruction input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(NotationEnum.X)).get();
			
			return output;
		}
	}
}
