package chaos.s07.a2wild.reports;

import chaos.s07.a2wild.C7Report;
import chaos.s07.a2wild.C7Trade;
import chaos.s07.a2wild.labels.C7AUTHC7RegLabelProvider;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;


@RosettaReport(namespace="chaos.s07.a2wild", body="C7AUTH", corpusList={"C7Reg"})
@RuneLabelProvider(labelProvider=C7AUTHC7RegLabelProvider.class)
@ImplementedBy(C7AUTHC7RegReportFunction.C7AUTHC7RegReportFunctionDefault.class)
public abstract class C7AUTHC7RegReportFunction implements ReportFunction<C7Trade, C7Report> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected C7NotionalRule c7NotionalRule;
	@Inject protected C7UtidRule c7UtidRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public C7Report evaluate(C7Trade input) {
		C7Report.C7ReportBuilder outputBuilder = doEvaluate(input);
		
		final C7Report output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(C7Report.class, output);
		}
		
		return output;
	}

	protected abstract C7Report.C7ReportBuilder doEvaluate(C7Trade input);

	public static class C7AUTHC7RegReportFunctionDefault extends C7AUTHC7RegReportFunction {
		@Override
		protected C7Report.C7ReportBuilder doEvaluate(C7Trade input) {
			C7Report.C7ReportBuilder output = C7Report.builder();
			return assignOutput(output, input);
		}
		
		protected C7Report.C7ReportBuilder assignOutput(C7Report.C7ReportBuilder output, C7Trade input) {
			output
				.setUtiField(c7UtidRule.evaluate(input));
			
			output
				.setNotionalField(c7NotionalRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
