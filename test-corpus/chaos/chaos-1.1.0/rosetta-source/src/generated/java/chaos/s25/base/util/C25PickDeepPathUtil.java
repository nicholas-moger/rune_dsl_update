package chaos.s25.base.util;

import chaos.s25.base.C25OptA;
import chaos.s25.base.C25OptB;
import chaos.s25.base.C25Pick;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C25PickDeepPathUtil {
	public String chooseShared(C25Pick c25Pick) {
		final MapperS<C25OptA> c25OptA = MapperS.of(c25Pick).<C25OptA>map("getC25OptA", _c25Pick -> _c25Pick.getC25OptA());
		if (exists(c25OptA).getOrDefault(false)) {
			return c25OptA.<String>map("getShared", _c25OptA -> _c25OptA.getShared()).get();
		}
		final MapperS<C25OptB> c25OptB = MapperS.of(c25Pick).<C25OptB>map("getC25OptB", _c25Pick -> _c25Pick.getC25OptB());
		if (exists(c25OptB).getOrDefault(false)) {
			return c25OptB.<String>map("getShared", _c25OptB -> _c25OptB.getShared()).get();
		}
		return null;
	}
	
}
