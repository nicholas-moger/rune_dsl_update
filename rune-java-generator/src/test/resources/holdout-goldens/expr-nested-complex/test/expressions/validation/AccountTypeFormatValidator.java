package test.expressions.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.expressions.Account;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AccountTypeFormatValidator implements Validator<Account> {

	private List<ComparisonResult> getComparisonResults(Account o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Account o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Account", ValidationResult.ValidationType.TYPE_FORMAT, "Account", path, "", res.getError());
				}
				return success("Account", ValidationResult.ValidationType.TYPE_FORMAT, "Account", path, "");
			})
			.collect(toList());
	}

}
