package test.frec052.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.records.Date;
import java.time.LocalDateTime;


@ImplementedBy(GetDate.GetDateDefault.class)
public abstract class GetDate implements RosettaFunction {

	/**
	* @param dt 
	* @return result 
	*/
	public Date evaluate(LocalDateTime dt) {
		Date result = doEvaluate(dt);
		
		return result;
	}

	protected abstract Date doEvaluate(LocalDateTime dt);

	public static class GetDateDefault extends GetDate {
		@Override
		protected Date doEvaluate(LocalDateTime dt) {
			Date result = null;
			return assignOutput(result, dt);
		}
		
		protected Date assignOutput(Date result, LocalDateTime dt) {
			result = MapperS.of(dt).<Date>map("Date", _dt -> Date.of(_dt.toLocalDate())).get();
			
			return result;
		}
	}
}
