package test.datetimeadd.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.records.Date;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import javax.inject.Inject;
import test.datetimeadd.FoncOut;
import test.datetimeadd.FuncIn;


@ImplementedBy(Calc.CalcDefault.class)
public abstract class Calc implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param funIn 
	* @return res 
	*/
	public FoncOut evaluate(FuncIn funIn) {
		FoncOut.FoncOutBuilder resBuilder = doEvaluate(funIn);
		
		final FoncOut res;
		if (resBuilder == null) {
			res = null;
		} else {
			res = resBuilder.build();
			objectValidator.validate(FoncOut.class, res);
		}
		
		return res;
	}

	protected abstract FoncOut.FoncOutBuilder doEvaluate(FuncIn funIn);

	protected abstract MapperS<Date> arg1(FuncIn funIn);

	protected abstract MapperS<LocalTime> arg2(FuncIn funIn);

	public static class CalcDefault extends Calc {
		@Override
		protected FoncOut.FoncOutBuilder doEvaluate(FuncIn funIn) {
			FoncOut.FoncOutBuilder res = FoncOut.builder();
			return assignOutput(res, funIn);
		}
		
		protected FoncOut.FoncOutBuilder assignOutput(FoncOut.FoncOutBuilder res, FuncIn funIn) {
			res
				.setRes1(MapperMaths.<LocalDateTime, Date, LocalTime>add(arg1(funIn), arg2(funIn)).get());
			
			res
				.setRes2(MapperMaths.<LocalDateTime, Date, LocalTime>add(arg1(funIn), arg2(funIn)).get());
			
			return Optional.ofNullable(res)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected MapperS<Date> arg1(FuncIn funIn) {
			return MapperS.of(funIn).<Date>map("getVal1", funcIn -> funcIn.getVal1());
		}
		
		@Override
		protected MapperS<LocalTime> arg2(FuncIn funIn) {
			return MapperS.of(funIn).<LocalTime>map("getVal2", funcIn -> funcIn.getVal2());
		}
	}
}
