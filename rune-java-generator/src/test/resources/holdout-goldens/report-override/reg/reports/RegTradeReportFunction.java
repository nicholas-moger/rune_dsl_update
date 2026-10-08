package reg.reports;

import base.layer.Instruction;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import reg.RegReport;
import reg.labels.RegTradeLabelProvider;


@RosettaReport(namespace="reg", body="Reg", corpusList={"Trade"})
@RuneLabelProvider(labelProvider=RegTradeLabelProvider.class)
@ImplementedBy(RegTradeReportFunction.RegTradeReportFunctionDefault.class)
public abstract class RegTradeReportFunction implements ReportFunction<Instruction, RegReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected PriceNotationRule priceNotationRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public RegReport evaluate(Instruction input) {
		RegReport.RegReportBuilder outputBuilder = doEvaluate(input);
		
		final RegReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(RegReport.class, output);
		}
		
		return output;
	}

	protected abstract RegReport.RegReportBuilder doEvaluate(Instruction input);

	public static class RegTradeReportFunctionDefault extends RegTradeReportFunction {
		@Override
		protected RegReport.RegReportBuilder doEvaluate(Instruction input) {
			RegReport.RegReportBuilder output = RegReport.builder();
			return assignOutput(output, input);
		}
		
		protected RegReport.RegReportBuilder assignOutput(RegReport.RegReportBuilder output, Instruction input) {
			output
				.setNotation(priceNotationRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
