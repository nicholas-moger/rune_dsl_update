package test.enumuniedge;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Display names that need Java escaping beyond non-ASCII.
 * @version 1.0.0
 */
@RosettaEnum("EscapeEnum")
public enum EscapeEnum {

	@RosettaEnumValue(value = "Quote", displayName = "say "hi"") 
	QUOTE("Quote", "say \"hi\""),
	
	@RosettaEnumValue(value = "Backslash", displayName = "back\slash") 
	BACKSLASH("Backslash", "back\\slash"),
	
	@RosettaEnumValue(value = "Tab", displayName = "tab	here") 
	TAB("Tab", "tab\there"),
	
	@RosettaEnumValue(value = "Dollar", displayName = "cost $5 {x}") 
	DOLLAR("Dollar", "cost $5 {x}")
;
	private static Map<String, EscapeEnum> values;
	static {
        Map<String, EscapeEnum> map = new ConcurrentHashMap<>();
		for (EscapeEnum instance : EscapeEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	EscapeEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static EscapeEnum fromDisplayName(String name) {
		EscapeEnum value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
		}
		return value;
	}

	@Override
	public String toString() {
		return toDisplayString();
	}

	public String toDisplayString() {
		return displayName != null ?  displayName : rosettaName;
	}
}
