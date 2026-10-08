package test.frec029.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.records.Date;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


@ImplementedBy(Foo.FooDefault.class)
public abstract class Foo implements RosettaFunction {

	/**
	* @param date 
	* @param time 
	* @param zone 
	* @return result 
	*/
	public ZonedDateTime evaluate(Date date, LocalTime time, String zone) {
		ZonedDateTime result = doEvaluate(date, time, zone);
		
		return result;
	}

	protected abstract ZonedDateTime doEvaluate(Date date, LocalTime time, String zone);

	public static class FooDefault extends Foo {
		@Override
		protected ZonedDateTime doEvaluate(Date date, LocalTime time, String zone) {
			ZonedDateTime result = null;
			return assignOutput(result, date, time, zone);
		}
		
		protected ZonedDateTime assignOutput(ZonedDateTime result, Date date, LocalTime time, String zone) {
			if (date != null && time != null && zone != null) {
				result = ZonedDateTime.of(date.toLocalDate(), time, ZoneId.of(zone));
			} else {
				result = null;
			}
			
			return result;
		}
	}
}
