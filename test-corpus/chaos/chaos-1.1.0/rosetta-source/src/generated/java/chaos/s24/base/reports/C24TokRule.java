package chaos.s24.base.reports;

import chaos.s24.base.C24Carrier;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C24TokRule.C24TokRuleDefault.class)
public abstract class C24TokRule implements ReportFunction<C24Carrier, Void> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Void evaluate(C24Carrier input) {
		Void output = doEvaluate(input);
		
		return output;
	}

	protected abstract Void doEvaluate(C24Carrier input);

	public static class C24TokRuleDefault extends C24TokRule {
		@Override
		protected Void doEvaluate(C24Carrier input) {
			Void output = null;
			return assignOutput(output, input);
		}
		
		protected Void assignOutput(Void output, C24Carrier input) {
			output = null;
			
			return output;
		}
	}
}
