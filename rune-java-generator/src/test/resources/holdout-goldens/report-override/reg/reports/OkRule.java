package reg.reports;

import base.layer.Instruction;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(OkRule.OkRuleDefault.class)
public abstract class OkRule implements ReportFunction<Instruction, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(Instruction input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(Instruction input);

	public static class OkRuleDefault extends OkRule {
		@Override
		protected Boolean doEvaluate(Instruction input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, Instruction input) {
			output = true;
			
			return output;
		}
	}
}
