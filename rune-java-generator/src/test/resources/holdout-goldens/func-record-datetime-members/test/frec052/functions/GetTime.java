package test.frec052.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.time.LocalDateTime;
import java.time.LocalTime;


@ImplementedBy(GetTime.GetTimeDefault.class)
public abstract class GetTime implements RosettaFunction {

	/**
	* @param dt 
	* @return result 
	*/
	public LocalTime evaluate(LocalDateTime dt) {
		LocalTime result = doEvaluate(dt);
		
		return result;
	}

	protected abstract LocalTime doEvaluate(LocalDateTime dt);

	public static class GetTimeDefault extends GetTime {
		@Override
		protected LocalTime doEvaluate(LocalDateTime dt) {
			LocalTime result = null;
			return assignOutput(result, dt);
		}
		
		protected LocalTime assignOutput(LocalTime result, LocalDateTime dt) {
			result = MapperS.of(dt).<LocalTime>map("Time", LocalDateTime::toLocalTime).get();
			
			return result;
		}
	}
}
