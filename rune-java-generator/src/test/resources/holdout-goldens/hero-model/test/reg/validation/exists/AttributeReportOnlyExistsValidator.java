package test.reg.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.reg.AttributeReport;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class AttributeReportOnlyExistsValidator implements ValidatorWithArg<AttributeReport, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends AttributeReport> ValidationResult<AttributeReport> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("heroInt", ExistenceChecker.isSet((Integer) o.getHeroInt()))
				.put("heroNumber", ExistenceChecker.isSet((BigDecimal) o.getHeroNumber()))
				.put("heroTime", ExistenceChecker.isSet((LocalTime) o.getHeroTime()))
				.put("heroZonedDateTime", ExistenceChecker.isSet((ZonedDateTime) o.getHeroZonedDateTime()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("AttributeReport", ValidationResult.ValidationType.ONLY_EXISTS, "AttributeReport", path, "");
		}
		return failure("AttributeReport", ValidationResult.ValidationType.ONLY_EXISTS, "AttributeReport", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
