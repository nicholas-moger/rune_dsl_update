package test.frec053.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.time.LocalTime;
import java.time.ZonedDateTime;


@ImplementedBy(GetTime.GetTimeDefault.class)
public abstract class GetTime implements RosettaFunction {

	/**
	* @param zdt 
	* @return result 
	*/
	public LocalTime evaluate(ZonedDateTime zdt) {
		LocalTime result = doEvaluate(zdt);
		
		return result;
	}

	protected abstract LocalTime doEvaluate(ZonedDateTime zdt);

	public static class GetTimeDefault extends GetTime {
		@Override
		protected LocalTime doEvaluate(ZonedDateTime zdt) {
			LocalTime result = null;
			return assignOutput(result, zdt);
		}
		
		protected LocalTime assignOutput(LocalTime result, ZonedDateTime zdt) {
			result = MapperS.of(zdt).<LocalTime>map("Time", ZonedDateTime::toLocalTime).get();
			
			return result;
		}
	}
}
