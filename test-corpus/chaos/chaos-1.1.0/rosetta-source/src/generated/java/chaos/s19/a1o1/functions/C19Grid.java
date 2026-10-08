package chaos.s19.a1o1.functions;

import chaos.s19.a1o1.C19Whole;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;


@ImplementedBy(C19Grid.C19GridDefault.class)
public abstract class C19Grid implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param n 
	* @return ws 
	*/
	public List<? extends C19Whole> evaluate(BigDecimal n) {
		List<C19Whole.C19WholeBuilder> wsBuilder = doEvaluate(n);
		
		final List<? extends C19Whole> ws;
		if (wsBuilder == null) {
			ws = null;
		} else {
			ws = wsBuilder.stream().map(C19Whole::build).collect(Collectors.toList());
			objectValidator.validate(C19Whole.class, ws);
		}
		
		return ws;
	}

	protected abstract List<C19Whole.C19WholeBuilder> doEvaluate(BigDecimal n);

	public static class C19GridDefault extends C19Grid {
		@Override
		protected List<C19Whole.C19WholeBuilder> doEvaluate(BigDecimal n) {
			List<C19Whole.C19WholeBuilder> ws = new ArrayList<>();
			return assignOutput(ws, n);
		}
		
		protected List<C19Whole.C19WholeBuilder> assignOutput(List<C19Whole.C19WholeBuilder> ws, BigDecimal n) {
			ws.addAll(toBuilder(MapperC.<C19Whole>of(MapperS.of(C19Whole.builder()
				.setName("a")
				.setScores(MapperC.<BigDecimal>of(MapperS.of(n)).getMulti())
				.build()), MapperS.of(C19Whole.builder()
				.setName("b")
				.setScores(MapperC.<BigDecimal>of(MapperS.of(n), MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(n), MapperS.of(BigDecimal.valueOf(2)))).getMulti())
				.build())).getMulti()));
			
			return Optional.ofNullable(ws)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
