package test.rws.b.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.rws.b.RwsReport;
import test.rws.b.RwsTrade;
import test.rws.b.labels.RwsAUTHRwsRegLabelProvider;


@RosettaReport(namespace="test.rws.b", body="RwsAUTH", corpusList={"RwsReg"})
@RuneLabelProvider(labelProvider=RwsAUTHRwsRegLabelProvider.class)
@ImplementedBy(RwsAUTHRwsRegReportFunction.RwsAUTHRwsRegReportFunctionDefault.class)
public abstract class RwsAUTHRwsRegReportFunction implements ReportFunction<RwsTrade, RwsReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected RwsNotionalRule rwsNotionalRule;
	@Inject protected RwsUtidRule rwsUtidRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public RwsReport evaluate(RwsTrade input) {
		RwsReport.RwsReportBuilder outputBuilder = doEvaluate(input);
		
		final RwsReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(RwsReport.class, output);
		}
		
		return output;
	}

	protected abstract RwsReport.RwsReportBuilder doEvaluate(RwsTrade input);

	public static class RwsAUTHRwsRegReportFunctionDefault extends RwsAUTHRwsRegReportFunction {
		@Override
		protected RwsReport.RwsReportBuilder doEvaluate(RwsTrade input) {
			RwsReport.RwsReportBuilder output = RwsReport.builder();
			return assignOutput(output, input);
		}
		
		protected RwsReport.RwsReportBuilder assignOutput(RwsReport.RwsReportBuilder output, RwsTrade input) {
			output
				.setUtiField(rwsUtidRule.evaluate(input));
			
			output
				.setNotionalField(rwsNotionalRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
