package test.rwq.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.rwq.a.RwqReport;
import test.rwq.a.RwqTrade;
import test.rwq.a.reports.RwqNotionalRule;
import test.rwq.a.reports.RwqUtidRule;
import test.rwq.b.labels.RwqAUTHRwqRegLabelProvider;


@RosettaReport(namespace="test.rwq.b", body="RwqAUTH", corpusList={"RwqReg"})
@RuneLabelProvider(labelProvider=RwqAUTHRwqRegLabelProvider.class)
@ImplementedBy(RwqAUTHRwqRegReportFunction.RwqAUTHRwqRegReportFunctionDefault.class)
public abstract class RwqAUTHRwqRegReportFunction implements ReportFunction<RwqTrade, RwqReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected RwqNotionalRule rwqNotionalRule;
	@Inject protected RwqUtidRule rwqUtidRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public RwqReport evaluate(RwqTrade input) {
		RwqReport.RwqReportBuilder outputBuilder = doEvaluate(input);
		
		final RwqReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(RwqReport.class, output);
		}
		
		return output;
	}

	protected abstract RwqReport.RwqReportBuilder doEvaluate(RwqTrade input);

	public static class RwqAUTHRwqRegReportFunctionDefault extends RwqAUTHRwqRegReportFunction {
		@Override
		protected RwqReport.RwqReportBuilder doEvaluate(RwqTrade input) {
			RwqReport.RwqReportBuilder output = RwqReport.builder();
			return assignOutput(output, input);
		}
		
		protected RwqReport.RwqReportBuilder assignOutput(RwqReport.RwqReportBuilder output, RwqTrade input) {
			output
				.setUtiField(rwqUtidRule.evaluate(input));
			
			output
				.setNotionalField(rwqNotionalRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
