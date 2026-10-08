package route.fixture.util;

import com.rosetta.model.lib.mapper.MapperS;
import route.fixture.RouteAltA;
import route.fixture.RouteAltB;
import route.fixture.RouteDeep;
import route.fixture.RouteLeaf;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class RouteDeepDeepPathUtil {
	public RouteLeaf chooseShared(RouteDeep routeDeep) {
		final MapperS<RouteAltA> a = MapperS.of(routeDeep).<RouteAltA>map("getA", _routeDeep -> _routeDeep.getA());
		if (exists(a).getOrDefault(false)) {
			return a.<RouteLeaf>map("getShared", routeAltA -> routeAltA.getShared()).get();
		}
		final MapperS<RouteAltB> b = MapperS.of(routeDeep).<RouteAltB>map("getB", _routeDeep -> _routeDeep.getB());
		if (exists(b).getOrDefault(false)) {
			return b.<RouteLeaf>map("getShared", routeAltB -> routeAltB.getShared()).get();
		}
		return null;
	}
	
}
