package ext.reports;

import base.layer.Instruction;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import ext.ExtRegReport;
import ext.labels.RegTradeExtLabelProvider;
import java.util.Optional;
import javax.inject.Inject;
import reg.reports.PriceNotationRule;


@RosettaReport(namespace="ext", body="Reg", corpusList={"Trade", "Ext"})
@RuneLabelProvider(labelProvider=RegTradeExtLabelProvider.class)
@ImplementedBy(RegTradeExtReportFunction.RegTradeExtReportFunctionDefault.class)
public abstract class RegTradeExtReportFunction implements ReportFunction<Instruction, ExtRegReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected PriceNotationRule priceNotationRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public ExtRegReport evaluate(Instruction input) {
		ExtRegReport.ExtRegReportBuilder outputBuilder = doEvaluate(input);
		
		final ExtRegReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(ExtRegReport.class, output);
		}
		
		return output;
	}

	protected abstract ExtRegReport.ExtRegReportBuilder doEvaluate(Instruction input);

	public static class RegTradeExtReportFunctionDefault extends RegTradeExtReportFunction {
		@Override
		protected ExtRegReport.ExtRegReportBuilder doEvaluate(Instruction input) {
			ExtRegReport.ExtRegReportBuilder output = ExtRegReport.builder();
			return assignOutput(output, input);
		}
		
		protected ExtRegReport.ExtRegReportBuilder assignOutput(ExtRegReport.ExtRegReportBuilder output, Instruction input) {
			output
				.setNotation(priceNotationRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
