package test.enumuni;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A non-ASCII DEFINITION beside a non-ASCII display name — µ–„def“ 試験.
 * @version 1.0.0
 */
@RosettaEnum("DefinedEnum")
public enum DefinedEnum {

	/**
	 * Value definition with non-ASCII — ü.
	 */
	@RosettaEnumValue(value = "A", displayName = "Ä") 
	A("A", "\u00C4"),
	
	/**
	 * Plain value with a non-ASCII definition — ß.
	 */
	@RosettaEnumValue(value = "B") 
	B("B", null)
;
	private static Map<String, DefinedEnum> values;
	static {
        Map<String, DefinedEnum> map = new ConcurrentHashMap<>();
		for (DefinedEnum instance : DefinedEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	DefinedEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static DefinedEnum fromDisplayName(String name) {
		DefinedEnum value = values.get(name);
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
