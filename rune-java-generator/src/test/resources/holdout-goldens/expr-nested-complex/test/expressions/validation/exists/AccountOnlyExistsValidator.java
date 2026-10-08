package test.expressions.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.expressions.Account;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class AccountOnlyExistsValidator implements ValidatorWithArg<Account, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Account> ValidationResult<Account> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("balance", ExistenceChecker.isSet((BigDecimal) o.getBalance()))
				.put("active", ExistenceChecker.isSet((Boolean) o.getActive()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Account", ValidationResult.ValidationType.ONLY_EXISTS, "Account", path, "");
		}
		return failure("Account", ValidationResult.ValidationType.ONLY_EXISTS, "Account", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
