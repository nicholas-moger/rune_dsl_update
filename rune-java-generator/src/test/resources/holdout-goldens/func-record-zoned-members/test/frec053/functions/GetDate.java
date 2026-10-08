package test.frec053.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.records.Date;
import java.time.ZonedDateTime;


@ImplementedBy(GetDate.GetDateDefault.class)
public abstract class GetDate implements RosettaFunction {

	/**
	* @param zdt 
	* @return result 
	*/
	public Date evaluate(ZonedDateTime zdt) {
		Date result = doEvaluate(zdt);
		
		return result;
	}

	protected abstract Date doEvaluate(ZonedDateTime zdt);

	public static class GetDateDefault extends GetDate {
		@Override
		protected Date doEvaluate(ZonedDateTime zdt) {
			Date result = null;
			return assignOutput(result, zdt);
		}
		
		protected Date assignOutput(Date result, ZonedDateTime zdt) {
			result = MapperS.of(zdt).<Date>map("Date", _zdt -> Date.of(_zdt.toLocalDate())).get();
			
			return result;
		}
	}
}
