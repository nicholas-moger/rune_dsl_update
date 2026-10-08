package chaos.s33.a3hub.p2.reports;

import chaos.s33.a3hub.p2.C33Report;
import chaos.s33.a3hub.p2.C33Trade;
import chaos.s33.a3hub.p2.labels.C33AUTHC33RegLabelProvider;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;


@RosettaReport(namespace="chaos.s33.a3hub.p2", body="C33AUTH", corpusList={"C33Reg"})
@RuneLabelProvider(labelProvider=C33AUTHC33RegLabelProvider.class)
@ImplementedBy(C33AUTHC33RegReportFunction.C33AUTHC33RegReportFunctionDefault.class)
public abstract class C33AUTHC33RegReportFunction implements ReportFunction<C33Trade, C33Report> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected C33LitRule c33LitRule;
	@Inject protected C33UtidRule c33UtidRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public C33Report evaluate(C33Trade input) {
		C33Report.C33ReportBuilder outputBuilder = doEvaluate(input);
		
		final C33Report output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(C33Report.class, output);
		}
		
		return output;
	}

	protected abstract C33Report.C33ReportBuilder doEvaluate(C33Trade input);

	public static class C33AUTHC33RegReportFunctionDefault extends C33AUTHC33RegReportFunction {
		@Override
		protected C33Report.C33ReportBuilder doEvaluate(C33Trade input) {
			C33Report.C33ReportBuilder output = C33Report.builder();
			return assignOutput(output, input);
		}
		
		protected C33Report.C33ReportBuilder assignOutput(C33Report.C33ReportBuilder output, C33Trade input) {
			output
				.setUtiField(c33UtidRule.evaluate(input));
			
			output
				.setLitField(c33LitRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
