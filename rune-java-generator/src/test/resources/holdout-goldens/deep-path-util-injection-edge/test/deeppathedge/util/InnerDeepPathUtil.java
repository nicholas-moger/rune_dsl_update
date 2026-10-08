package test.deeppathedge.util;

import com.rosetta.model.lib.mapper.MapperS;
import test.deeppathedge.CashLeg;
import test.deeppathedge.Inner;
import test.deeppathedge.StockLeg;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class InnerDeepPathUtil {
	public String chooseCommon(Inner inner) {
		final MapperS<CashLeg> cashLeg = MapperS.of(inner).<CashLeg>map("getCashLeg", _inner -> _inner.getCashLeg());
		if (exists(cashLeg).getOrDefault(false)) {
			return cashLeg.<String>map("getCommon", _cashLeg -> _cashLeg.getCommon()).get();
		}
		final MapperS<StockLeg> stockLeg = MapperS.of(inner).<StockLeg>map("getStockLeg", _inner -> _inner.getStockLeg());
		if (exists(stockLeg).getOrDefault(false)) {
			return stockLeg.<String>map("getCommon", _stockLeg -> _stockLeg.getCommon()).get();
		}
		return null;
	}
	
}
