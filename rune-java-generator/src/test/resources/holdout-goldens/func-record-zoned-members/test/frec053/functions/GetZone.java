package test.frec053.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.time.ZonedDateTime;


@ImplementedBy(GetZone.GetZoneDefault.class)
public abstract class GetZone implements RosettaFunction {

	/**
	* @param zdt 
	* @return result 
	*/
	public String evaluate(ZonedDateTime zdt) {
		String result = doEvaluate(zdt);
		
		return result;
	}

	protected abstract String doEvaluate(ZonedDateTime zdt);

	public static class GetZoneDefault extends GetZone {
		@Override
		protected String doEvaluate(ZonedDateTime zdt) {
			String result = null;
			return assignOutput(result, zdt);
		}
		
		protected String assignOutput(String result, ZonedDateTime zdt) {
			result = MapperS.of(zdt).<String>map("Timezone", _zdt -> _zdt.getZone().getId()).get();
			
			return result;
		}
	}
}
