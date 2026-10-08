package chaos.s04.a1o3.functions;

import chaos.s04.a1o3.C4ModeEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import javax.inject.Inject;


/**
 * Dispatch BASE - declares the inputs and output the branches inherit (the CDM YearFraction shape).
 * @version 1.0.0
 */
public class C4Speed implements RosettaFunction {
	
	@Inject protected C4Speed.C4SpeedFAST c4SpeedFast;
	@Inject protected C4Speed.C4SpeedSLOW c4SpeedSlow;
	
	public BigDecimal evaluate(C4ModeEnum mode, BigDecimal x) {
		switch (mode) {
			case FAST:
				return c4SpeedFast.evaluate(mode, x);
			case SLOW:
				return c4SpeedSlow.evaluate(mode, x);
			default:
				throw new IllegalArgumentException("Enum value not implemented: " + mode);
		}
	}
	
	@ImplementedBy(C4Speed.C4SpeedFAST.C4SpeedFASTDefault.class)
	public static abstract class C4SpeedFAST implements RosettaFunction {
	
		/**
		* @param mode 
		* @param x 
		* @return y 
		*/
		public BigDecimal evaluate(C4ModeEnum mode, BigDecimal x) {
			BigDecimal y = doEvaluate(mode, x);
			
			return y;
		}
	
		protected abstract BigDecimal doEvaluate(C4ModeEnum mode, BigDecimal x);
	
		public static class C4SpeedFASTDefault extends C4Speed.C4SpeedFAST {
			@Override
			protected BigDecimal doEvaluate(C4ModeEnum mode, BigDecimal x) {
				BigDecimal y = null;
				return assignOutput(y, mode, x);
			}
			
			protected BigDecimal assignOutput(BigDecimal y, C4ModeEnum mode, BigDecimal x) {
				y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(2))).get();
				
				return y;
			}
		}
	}
	@ImplementedBy(C4Speed.C4SpeedSLOW.C4SpeedSLOWDefault.class)
	public static abstract class C4SpeedSLOW implements RosettaFunction {
	
		/**
		* @param mode 
		* @param x 
		* @return y 
		*/
		public BigDecimal evaluate(C4ModeEnum mode, BigDecimal x) {
			BigDecimal y = doEvaluate(mode, x);
			
			return y;
		}
	
		protected abstract BigDecimal doEvaluate(C4ModeEnum mode, BigDecimal x);
	
		public static class C4SpeedSLOWDefault extends C4Speed.C4SpeedSLOW {
			@Override
			protected BigDecimal doEvaluate(C4ModeEnum mode, BigDecimal x) {
				BigDecimal y = null;
				return assignOutput(y, mode, x);
			}
			
			protected BigDecimal assignOutput(BigDecimal y, C4ModeEnum mode, BigDecimal x) {
				y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide(MapperS.of(x), MapperS.of(BigDecimal.valueOf(2))).get();
				
				return y;
			}
		}
	}
}
