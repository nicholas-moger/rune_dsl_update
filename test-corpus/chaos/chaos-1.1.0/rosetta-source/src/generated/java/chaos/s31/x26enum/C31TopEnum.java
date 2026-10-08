package chaos.s31.x26enum;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.model.lib.annotations.RosettaSynonym;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Extends chain 3, with a synonym on the value.
 * @version 1.0.0
 */
@RosettaEnum("C31TopEnum")
public enum C31TopEnum {

	@RosettaEnumValue(value = "A") 
	A("A", null),
	
	@RosettaEnumValue(value = "B") 
	B("B", null),
	
	@RosettaSynonym(value = "cee", source = "C31Src")
	@RosettaEnumValue(value = "C") 
	C("C", null)
;
	private static Map<String, C31TopEnum> values;
	static {
        Map<String, C31TopEnum> map = new ConcurrentHashMap<>();
		for (C31TopEnum instance : C31TopEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C31TopEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C31TopEnum fromDisplayName(String name) {
		C31TopEnum value = values.get(name);
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
