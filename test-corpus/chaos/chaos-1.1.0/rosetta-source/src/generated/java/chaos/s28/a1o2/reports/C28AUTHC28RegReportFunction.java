package chaos.s28.a1o2.reports;

import chaos.s28.a1o2.C28Report;
import chaos.s28.a1o2.C28Trade;
import chaos.s28.a1o2.labels.C28AUTHC28RegLabelProvider;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;


@RosettaReport(namespace="chaos.s28.a1o2", body="C28AUTH", corpusList={"C28Reg"})
@RuneLabelProvider(labelProvider=C28AUTHC28RegLabelProvider.class)
@ImplementedBy(C28AUTHC28RegReportFunction.C28AUTHC28RegReportFunctionDefault.class)
public abstract class C28AUTHC28RegReportFunction implements ReportFunction<C28Trade, C28Report> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected C28AvRule c28AvRule;
	@Inject protected C28UtidRule c28UtidRule;
	@Inject protected C28VenueRule c28VenueRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public C28Report evaluate(C28Trade input) {
		C28Report.C28ReportBuilder outputBuilder = doEvaluate(input);
		
		final C28Report output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(C28Report.class, output);
		}
		
		return output;
	}

	protected abstract C28Report.C28ReportBuilder doEvaluate(C28Trade input);

	public static class C28AUTHC28RegReportFunctionDefault extends C28AUTHC28RegReportFunction {
		@Override
		protected C28Report.C28ReportBuilder doEvaluate(C28Trade input) {
			C28Report.C28ReportBuilder output = C28Report.builder();
			return assignOutput(output, input);
		}
		
		protected C28Report.C28ReportBuilder assignOutput(C28Report.C28ReportBuilder output, C28Trade input) {
			output
				.setUtiField(c28UtidRule.evaluate(input));
			
			output
				.setAvField(c28AvRule.evaluate(input));
			
			output
				.setVenueField(c28VenueRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
