package chaos.s28.a3half.p2.reports;

import chaos.s28.a3half.p1.C28Trade;
import chaos.s28.a3half.p2.C28ReportChoice;
import chaos.s28.a3half.p2.labels.C28ALTC28RegLabelProvider;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;


@RosettaReport(namespace="chaos.s28.a3half.p2", body="C28ALT", corpusList={"C28Reg"})
@RuneLabelProvider(labelProvider=C28ALTC28RegLabelProvider.class)
@ImplementedBy(C28ALTC28RegReportFunction.C28ALTC28RegReportFunctionDefault.class)
public abstract class C28ALTC28RegReportFunction implements ReportFunction<C28Trade, C28ReportChoice> {
	
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
	public C28ReportChoice evaluate(C28Trade input) {
		C28ReportChoice.C28ReportChoiceBuilder outputBuilder = doEvaluate(input);
		
		final C28ReportChoice output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(C28ReportChoice.class, output);
		}
		
		return output;
	}

	protected abstract C28ReportChoice.C28ReportChoiceBuilder doEvaluate(C28Trade input);

	public static class C28ALTC28RegReportFunctionDefault extends C28ALTC28RegReportFunction {
		@Override
		protected C28ReportChoice.C28ReportChoiceBuilder doEvaluate(C28Trade input) {
			C28ReportChoice.C28ReportChoiceBuilder output = C28ReportChoice.builder();
			return assignOutput(output, input);
		}
		
		protected C28ReportChoice.C28ReportChoiceBuilder assignOutput(C28ReportChoice.C28ReportChoiceBuilder output, C28Trade input) {
			output
				.getOrCreateC28ChoiceReport()
				.setUtiField(c28UtidRule.evaluate(input));
			
			output
				.getOrCreateC28Report()
				.setUtiField(c28UtidRule.evaluate(input));
			
			output
				.getOrCreateC28Report()
				.setAvField(c28AvRule.evaluate(input));
			
			output
				.getOrCreateC28Report()
				.setVenueField(c28VenueRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
