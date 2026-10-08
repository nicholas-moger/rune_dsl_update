package chaos.s28.a2qual.util;

import chaos.s28.a2qual.C28ChoiceReport;
import chaos.s28.a2qual.C28Report;
import chaos.s28.a2qual.C28ReportChoice;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C28ReportChoiceDeepPathUtil {
	public String chooseUtiField(C28ReportChoice c28ReportChoice) {
		final MapperS<C28ChoiceReport> c28ChoiceReport = MapperS.of(c28ReportChoice).<C28ChoiceReport>map("getC28ChoiceReport", _c28ReportChoice -> _c28ReportChoice.getC28ChoiceReport());
		if (exists(c28ChoiceReport).getOrDefault(false)) {
			return c28ChoiceReport.<String>map("getUtiField", _c28ChoiceReport -> _c28ChoiceReport.getUtiField()).get();
		}
		final MapperS<C28Report> c28Report = MapperS.of(c28ReportChoice).<C28Report>map("getC28Report", _c28ReportChoice -> _c28ReportChoice.getC28Report());
		if (exists(c28Report).getOrDefault(false)) {
			return c28Report.<String>map("getUtiField", _c28Report -> _c28Report.getUtiField()).get();
		}
		return null;
	}
	
}
