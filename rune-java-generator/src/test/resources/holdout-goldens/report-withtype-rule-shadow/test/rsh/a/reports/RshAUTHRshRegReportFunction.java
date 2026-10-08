package test.rsh.a.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.rsh.a.RshReport;
import test.rsh.a.RshTrade;
import test.rsh.a.labels.RshAUTHRshRegLabelProvider;


@RosettaReport(namespace="test.rsh.a", body="RshAUTH", corpusList={"RshReg"})
@RuneLabelProvider(labelProvider=RshAUTHRshRegLabelProvider.class)
@ImplementedBy(RshAUTHRshRegReportFunction.RshAUTHRshRegReportFunctionDefault.class)
public abstract class RshAUTHRshRegReportFunction implements ReportFunction<RshTrade, RshReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected RshNotionalRule rshNotionalRule;
	@Inject protected RshReportRule rshReportRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public RshReport evaluate(RshTrade input) {
		RshReport.RshReportBuilder outputBuilder = doEvaluate(input);
		
		final RshReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(RshReport.class, output);
		}
		
		return output;
	}

	protected abstract RshReport.RshReportBuilder doEvaluate(RshTrade input);

	public static class RshAUTHRshRegReportFunctionDefault extends RshAUTHRshRegReportFunction {
		@Override
		protected RshReport.RshReportBuilder doEvaluate(RshTrade input) {
			RshReport.RshReportBuilder output = RshReport.builder();
			return assignOutput(output, input);
		}
		
		protected RshReport.RshReportBuilder assignOutput(RshReport.RshReportBuilder output, RshTrade input) {
			output
				.setUtiField(rshReportRule.evaluate(input));
			
			output
				.setNotionalField(rshNotionalRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
