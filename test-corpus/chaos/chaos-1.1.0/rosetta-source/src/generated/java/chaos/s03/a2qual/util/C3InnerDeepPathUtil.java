package chaos.s03.a2qual.util;

import chaos.s03.a2qual.C3CashLeg;
import chaos.s03.a2qual.C3Inner;
import chaos.s03.a2qual.C3StockLeg;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C3InnerDeepPathUtil {
	public String chooseCommon(C3Inner c3Inner) {
		final MapperS<C3CashLeg> c3CashLeg = MapperS.of(c3Inner).<C3CashLeg>map("getC3CashLeg", _c3Inner -> _c3Inner.getC3CashLeg());
		if (exists(c3CashLeg).getOrDefault(false)) {
			return c3CashLeg.<String>map("getCommon", _c3CashLeg -> _c3CashLeg.getCommon()).get();
		}
		final MapperS<C3StockLeg> c3StockLeg = MapperS.of(c3Inner).<C3StockLeg>map("getC3StockLeg", _c3Inner -> _c3Inner.getC3StockLeg());
		if (exists(c3StockLeg).getOrDefault(false)) {
			return c3StockLeg.<String>map("getCommon", _c3StockLeg -> _c3StockLeg.getCommon()).get();
		}
		return null;
	}
	
}
