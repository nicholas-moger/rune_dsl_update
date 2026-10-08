package test.rsr.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.rsr.b.RsrReport;
import test.rsr.b.RsrTrade;
import test.rsr.b.labels.RsrAUTHRsrRegLabelProvider;


@RosettaReport(namespace="test.rsr.b", body="RsrAUTH", corpusList={"RsrReg"})
@RuneLabelProvider(labelProvider=RsrAUTHRsrRegLabelProvider.class)
@ImplementedBy(RsrAUTHRsrRegReportFunction.RsrAUTHRsrRegReportFunctionDefault.class)
public abstract class RsrAUTHRsrRegReportFunction implements ReportFunction<RsrTrade, RsrReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected RsrNotionalRule rsrNotionalRule;
	@Inject protected RsrUtidRule rsrUtidRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public RsrReport evaluate(RsrTrade input) {
		RsrReport.RsrReportBuilder outputBuilder = doEvaluate(input);
		
		final RsrReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(RsrReport.class, output);
		}
		
		return output;
	}

	protected abstract RsrReport.RsrReportBuilder doEvaluate(RsrTrade input);

	public static class RsrAUTHRsrRegReportFunctionDefault extends RsrAUTHRsrRegReportFunction {
		@Override
		protected RsrReport.RsrReportBuilder doEvaluate(RsrTrade input) {
			RsrReport.RsrReportBuilder output = RsrReport.builder();
			return assignOutput(output, input);
		}
		
		protected RsrReport.RsrReportBuilder assignOutput(RsrReport.RsrReportBuilder output, RsrTrade input) {
			output
				.setUtiField(rsrUtidRule.evaluate(input));
			
			output
				.setNotionalField(rsrNotionalRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
