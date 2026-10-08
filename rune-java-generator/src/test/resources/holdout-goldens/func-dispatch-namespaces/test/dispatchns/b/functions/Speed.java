package test.dispatchns.b.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import javax.inject.Inject;
import test.dispatchns.b.ModeB;


/**
 * Dispatch BASE in namespace b - the same group name as namespace a, different bodies.
 * @version 0.0.0
 */
public class Speed implements RosettaFunction {
	
	@Inject protected Speed.SpeedFAST speedFast;
	@Inject protected Speed.SpeedSLOW speedSlow;
	
	public BigDecimal evaluate(ModeB mode, BigDecimal x) {
		switch (mode) {
			case FAST:
				return speedFast.evaluate(mode, x);
			case SLOW:
				return speedSlow.evaluate(mode, x);
			default:
				throw new IllegalArgumentException("Enum value not implemented: " + mode);
		}
	}
	
	@ImplementedBy(Speed.SpeedFAST.SpeedFASTDefault.class)
	public static abstract class SpeedFAST implements RosettaFunction {
	
		/**
		* @param mode 
		* @param x 
		* @return y 
		*/
		public BigDecimal evaluate(ModeB mode, BigDecimal x) {
			BigDecimal y = doEvaluate(mode, x);
			
			return y;
		}
	
		protected abstract BigDecimal doEvaluate(ModeB mode, BigDecimal x);
	
		public static class SpeedFASTDefault extends Speed.SpeedFAST {
			@Override
			protected BigDecimal doEvaluate(ModeB mode, BigDecimal x) {
				BigDecimal y = null;
				return assignOutput(y, mode, x);
			}
			
			protected BigDecimal assignOutput(BigDecimal y, ModeB mode, BigDecimal x) {
				y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(3))).get();
				
				return y;
			}
		}
	}
	@ImplementedBy(Speed.SpeedSLOW.SpeedSLOWDefault.class)
	public static abstract class SpeedSLOW implements RosettaFunction {
	
		/**
		* @param mode 
		* @param x 
		* @return y 
		*/
		public BigDecimal evaluate(ModeB mode, BigDecimal x) {
			BigDecimal y = doEvaluate(mode, x);
			
			return y;
		}
	
		protected abstract BigDecimal doEvaluate(ModeB mode, BigDecimal x);
	
		public static class SpeedSLOWDefault extends Speed.SpeedSLOW {
			@Override
			protected BigDecimal doEvaluate(ModeB mode, BigDecimal x) {
				BigDecimal y = null;
				return assignOutput(y, mode, x);
			}
			
			protected BigDecimal assignOutput(BigDecimal y, ModeB mode, BigDecimal x) {
				y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide(MapperS.of(x), MapperS.of(BigDecimal.valueOf(3))).get();
				
				return y;
			}
		}
	}
}
