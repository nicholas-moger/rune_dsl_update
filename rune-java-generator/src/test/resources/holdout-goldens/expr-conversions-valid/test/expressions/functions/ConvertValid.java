package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.records.Date;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import test.expressions.Colour;


@ImplementedBy(ConvertValid.ConvertValidDefault.class)
public abstract class ConvertValid implements RosettaFunction {

	/**
	* @param s 
	* @param n 
	* @param c 
	* @return result 
	*/
	public String evaluate(String s, BigDecimal n, Colour c) {
		String result = doEvaluate(s, n, c);
		
		return result;
	}

	protected abstract String doEvaluate(String s, BigDecimal n, Colour c);

	protected abstract MapperS<BigDecimal> asNum(String s, BigDecimal n, Colour c);

	protected abstract MapperS<Integer> asInt(String s, BigDecimal n, Colour c);

	protected abstract MapperS<Date> asDate(String s, BigDecimal n, Colour c);

	protected abstract MapperS<LocalDateTime> asDateTime(String s, BigDecimal n, Colour c);

	protected abstract MapperS<ZonedDateTime> asZoned(String s, BigDecimal n, Colour c);

	protected abstract MapperS<LocalTime> asTime(String s, BigDecimal n, Colour c);

	protected abstract MapperS<Colour> asColour(String s, BigDecimal n, Colour c);

	protected abstract MapperS<String> numStr(String s, BigDecimal n, Colour c);

	protected abstract MapperS<String> colourStr(String s, BigDecimal n, Colour c);

	public static class ConvertValidDefault extends ConvertValid {
		@Override
		protected String doEvaluate(String s, BigDecimal n, Colour c) {
			String result = null;
			return assignOutput(result, s, n, c);
		}
		
		protected String assignOutput(String result, String s, BigDecimal n, Colour c) {
			result = MapperS.of(n).map("to-string", Object::toString).get();
			
			return result;
		}
		
		@Override
		protected MapperS<BigDecimal> asNum(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-number", BigDecimal::new, NumberFormatException.class);
		}
		
		@Override
		protected MapperS<Integer> asInt(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-int", Integer::parseInt, NumberFormatException.class);
		}
		
		@Override
		protected MapperS<Date> asDate(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-date", Date::parse, DateTimeParseException.class);
		}
		
		@Override
		protected MapperS<LocalDateTime> asDateTime(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-date-time", LocalDateTime::parse, DateTimeParseException.class);
		}
		
		@Override
		protected MapperS<ZonedDateTime> asZoned(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-zoned-date-time", ZonedDateTime::parse, DateTimeParseException.class);
		}
		
		@Override
		protected MapperS<LocalTime> asTime(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-time", _s -> LocalTime.parse(_s, DateTimeFormatter.ISO_LOCAL_TIME), DateTimeParseException.class);
		}
		
		@Override
		protected MapperS<Colour> asColour(String s, BigDecimal n, Colour c) {
			return MapperS.of(s).checkedMap("to-enum", Colour::fromDisplayName, IllegalArgumentException.class);
		}
		
		@Override
		protected MapperS<String> numStr(String s, BigDecimal n, Colour c) {
			return MapperS.of(n).map("to-string", Object::toString);
		}
		
		@Override
		protected MapperS<String> colourStr(String s, BigDecimal n, Colour c) {
			return MapperS.of(c).map("to-string", Colour::toDisplayString);
		}
	}
}
