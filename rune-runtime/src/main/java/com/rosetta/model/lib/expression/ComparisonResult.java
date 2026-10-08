/*
 * Copyright 2024 REGnosys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rosetta.model.lib.expression;

import com.rosetta.model.lib.mapper.Mapper;
import com.rosetta.model.lib.mapper.MapperS;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ComparisonResult implements Mapper<Boolean> {
	private final Boolean result;
	@Deprecated
	private final Boolean emptyOperand;
	// The error channel is dual-field: at most ONE of {error, errorSupplier} is
	// non-null at construction (both null = no channel, i.e. success/empty).
	// error is written once by getError() when a supplier materializes — the
	// benign-race String.hashCode pattern: the supplier is deterministic and
	// side-effect-free, so a duplicate race produces identical content.
	private String error;
	private final Supplier<String> errorSupplier;

	public static ComparisonResult success() {
		return new ComparisonResult(true, false, (String) null);
	}
	
	@Deprecated
	public static ComparisonResult successEmptyOperand(String error) {
		return new ComparisonResult(true, true, error);
	}

	/** Lazy twin of {@link #successEmptyOperand(String)}; see {@link #failureLazy(Supplier)}. */
	@Deprecated
	static ComparisonResult successEmptyOperandLazy(Supplier<String> error) {
		return new ComparisonResult(true, true, Objects.requireNonNull(error, "error supplier"));
	}

	
	public static ComparisonResult failure(String error) {
		return new ComparisonResult(false, false, error);
	}

	/**
	 * The lazy failure channel: the message is built only if the text is ever
	 * observed ({@code getError()} materializes and memoizes). Package-private
	 * on purpose — every formatter-backed producer seat lives in this package,
	 * and the public factory surface must not change (BC posture).
	 * Suppliers must be deterministic, side-effect-free and non-null-returning.
	 */
	static ComparisonResult failureLazy(Supplier<String> error) {
		return new ComparisonResult(false, false, Objects.requireNonNull(error, "error supplier"));
	}

	@Deprecated
	public static ComparisonResult failureEmptyOperand(String error) {
		return new ComparisonResult(false, true, error);
	}

	/** Lazy twin of {@link #failureEmptyOperand(String)}; see {@link #failureLazy(Supplier)}. */
	@Deprecated
	static ComparisonResult failureEmptyOperandLazy(Supplier<String> error) {
		return new ComparisonResult(false, true, Objects.requireNonNull(error, "error supplier"));
	}

	public static ComparisonResult ofEmpty() {
		return new ComparisonResult(null, null, (String) null);
	}

    public static ComparisonResult ofNullSafe(Mapper<Boolean> result) {
        if (result.getMulti().isEmpty() || result.getMulti().stream().allMatch(Objects::isNull)) {
            return ofEmpty();
        }
        List<Boolean> filteredResults = result.getMulti().stream().filter(Objects::nonNull).collect(Collectors.toList());
        return new ComparisonResult(filteredResults.stream().allMatch(r -> r == true), false, (String) null);
    }

    @Deprecated
	public static ComparisonResult of(Mapper<Boolean> result) {
		return new ComparisonResult(result.getMulti().stream().allMatch(r -> r == true), false, (String) null);
	}

	private ComparisonResult(Boolean result, Boolean emptyOperand, String error) {
		this.result = result;
		this.emptyOperand = emptyOperand;
		this.error = error;
		this.errorSupplier = null;
	}

	private ComparisonResult(Boolean result, Boolean emptyOperand, Supplier<String> errorSupplier) {
		this.result = result;
		this.emptyOperand = emptyOperand;
		this.error = null;
		this.errorSupplier = errorSupplier;
	}

	@Override
	public Boolean get() {
		return result;
	}

	@Override
	public Boolean getOrDefault(Boolean defaultValue) {
		return result == null ? defaultValue : result;
	}

	public String getError() {
		if (error == null && errorSupplier != null) {
			error = errorSupplier.get();
		}
		return error;
	}

	/**
	 * The single internal read seam: every in-class error observation routes
	 * here (never the raw field), so no path can see an unmaterialized supplier.
	 */
	private String errorText() {
		return getError();
	}

	// and
	
	public boolean isEmptyOperand() {
		return result == null || (emptyOperand != null && emptyOperand);
	}

    public ComparisonResult andNullSafe(ComparisonResult other) {
        return andNullSafe(this, other);
    }

    @Deprecated
	public ComparisonResult and(ComparisonResult other) {
		return and(this, other);
	}

    @Deprecated
	public ComparisonResult andIgnoreEmptyOperand(ComparisonResult other) {
		return combineIgnoreEmptyOperand(other, this::and);
	}

    private ComparisonResult andNullSafe(ComparisonResult r1, ComparisonResult r2) {
        if (r1.isEmptyOperand() && r2.isEmptyOperand()) {
            return ComparisonResult.ofEmpty();
        }

        boolean newResult = r1.getOrDefault(false) && r2.getOrDefault(false);
        if (newResult) {
            // Both operands passed, so both building arms would be skipped:
            // the eager path stored the constant "" — keep that exact quirk
            // without paying a supplier allocation on the common passing path.
            return new ComparisonResult(newResult, false, "");
        }
        // The boolean logic above stays eager; only the text composes lazily.
        // Child texts materialize FIRST (through errorText()), then the
        // building code runs on those strings exactly as it always has.
        return new ComparisonResult(newResult, false, (Supplier<String>) () -> {
            String r1Error = r1.errorText();
            String r2Error = r2.errorText();
            String newError = "";
            if (!r1.getOrDefault(false)) {
                String r1Value = r1.isEmptyOperand() ? "empty" : r1.getOrDefault(false).toString();
                newError+=r1Error == null ? String.format("left of `and` operation is %s", r1Value) : r1Error;
            }
            if (!r2.getOrDefault(false)) {
                if (!r1.getOrDefault(false)) {
                    newError+=" and ";
                }
                String r2Value = r2.isEmptyOperand() ? "empty" : r2.getOrDefault(false).toString();
                newError+=r2Error == null ? String.format("right of `and` operation is %s", r2Value) : r2Error;
            }
            return newError;
        });
    }

    @Deprecated
	private ComparisonResult and(ComparisonResult r1, ComparisonResult r2) {
		boolean newResult = r1.result && r2.result;
		if (newResult) {
			return new ComparisonResult(newResult, false, "");
		}
		return new ComparisonResult(newResult, false, (Supplier<String>) () -> {
			String r1Error = r1.errorText();
			String r2Error = r2.errorText();
			String newError = "";
			if (!r1.result) {
				newError+=r1Error;
			}
			if (!r2.result) {
				if (!r1.result) {
					newError+=" and ";
				}
				newError+=r2Error;
			}
			return newError;
		});
	}
	
	// or

    public ComparisonResult orNullSafe(ComparisonResult other) {
        return orNullSafe(this, other);
    }

    @Deprecated
	public ComparisonResult or(ComparisonResult other) {
		return or(this, other);
	}

    @Deprecated
	public ComparisonResult orIgnoreEmptyOperand(ComparisonResult other) {
		return combineIgnoreEmptyOperand(other, this::or);
	}

    private ComparisonResult orNullSafe(ComparisonResult r1, ComparisonResult r2) {
        if (r1.isEmptyOperand() && r2.isEmptyOperand()) {
            return ComparisonResult.ofEmpty();
        }

        boolean newResult = r1.getOrDefault(false) || r2.getOrDefault(false);
        if (newResult) {
            // The eager code stored newResult?null:newError — a passing `or`
            // has NO error channel; preserved exactly.
            return new ComparisonResult(newResult, false, (String) null);
        }
        return new ComparisonResult(newResult, false, (Supplier<String>) () -> {
            String r1Error = r1.errorText();
            String r2Error = r2.errorText();
            String newError = "";
            newError+=r1Error;
            newError+=" and ";
            newError+=r2Error;
            return newError;
        });
    }

    @Deprecated
	private ComparisonResult or(ComparisonResult r1, ComparisonResult r2) {
		boolean newResult = r1.result || r2.result;
		if (newResult) {
			return new ComparisonResult(newResult, false, (String) null);
		}
		return new ComparisonResult(newResult, false, (Supplier<String>) () -> {
			String r1Error = r1.errorText();
			String r2Error = r2.errorText();
			String newError = "";
			newError+=r1Error;
			newError+=" and ";
			newError+=r2Error;
			return newError;
		});
	}
	
	// utils

    @Deprecated
	private ComparisonResult combineIgnoreEmptyOperand(ComparisonResult other, BinaryOperator<ComparisonResult> combineFunc) {
		if(this.emptyOperand && other.emptyOperand) {
			// Lazily composed twin of failureEmptyOperand(this.error + " and " + other.error).
			return new ComparisonResult(false, true, (Supplier<String>) () -> {
				String thisError = errorText();
				String otherError = other.errorText();
				return thisError + " and " + otherError;
			});
		}
		if(this.emptyOperand) {
			return other;
		}
		if(other.emptyOperand) {
			return this;
		}
		return combineFunc.apply(this, other);
	}

	@Override
	public List<Boolean> getMulti() {
		return Collections.singletonList(get());
	}

	@Override
	public Optional<?> getParent() {
		return Optional.empty();
	}

	@Override
	public List<?> getParentMulti() {
		return Collections.emptyList();
	}

	@Override
	public int resultCount() {
		return isEmptyOperand() ? 0 : 1;
	}

	public MapperS<Boolean> asMapper() {
		return isEmptyOperand() ? MapperS.ofNull() : MapperS.of(this.get());
	}
	
	@Override
	public List<Path> getPaths() {
		return Collections.emptyList();
	}

	@Override
	public List<Path> getErrorPaths() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getErrors() {
		return Collections.singletonList(getError()); 
	}
}