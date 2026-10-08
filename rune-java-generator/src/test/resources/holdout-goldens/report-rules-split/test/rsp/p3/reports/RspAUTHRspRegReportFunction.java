package test.rsp.p3.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.rsp.p2.RspTrade;
import test.rsp.p2.reports.RspNotionalRule;
import test.rsp.p2.reports.RspUtidRule;
import test.rsp.p3.RspReport;
import test.rsp.p3.labels.RspAUTHRspRegLabelProvider;


@RosettaReport(namespace="test.rsp.p3", body="RspAUTH", corpusList={"RspReg"})
@RuneLabelProvider(labelProvider=RspAUTHRspRegLabelProvider.class)
@ImplementedBy(RspAUTHRspRegReportFunction.RspAUTHRspRegReportFunctionDefault.class)
public abstract class RspAUTHRspRegReportFunction implements ReportFunction<RspTrade, RspReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected RspNotionalRule rspNotionalRule;
	@Inject protected RspUtidRule rspUtidRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public RspReport evaluate(RspTrade input) {
		RspReport.RspReportBuilder outputBuilder = doEvaluate(input);
		
		final RspReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(RspReport.class, output);
		}
		
		return output;
	}

	protected abstract RspReport.RspReportBuilder doEvaluate(RspTrade input);

	public static class RspAUTHRspRegReportFunctionDefault extends RspAUTHRspRegReportFunction {
		@Override
		protected RspReport.RspReportBuilder doEvaluate(RspTrade input) {
			RspReport.RspReportBuilder output = RspReport.builder();
			return assignOutput(output, input);
		}
		
		protected RspReport.RspReportBuilder assignOutput(RspReport.RspReportBuilder output, RspTrade input) {
			output
				.setUtiField(rspUtidRule.evaluate(input));
			
			output
				.setNotionalField(rspNotionalRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
