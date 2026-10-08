package test.chswitchedge;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The chaos C18KindEnum.
 * @version 0.0.0
 */
@RosettaEnum("KindEnum")
public enum KindEnum {

	@RosettaEnumValue(value = "Red") 
	RED("Red", null),
	
	@RosettaEnumValue(value = "Green") 
	GREEN("Green", null),
	
	@RosettaEnumValue(value = "Blue") 
	BLUE("Blue", null)
;
	private static Map<String, KindEnum> values;
	static {
        Map<String, KindEnum> map = new ConcurrentHashMap<>();
		for (KindEnum instance : KindEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	KindEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static KindEnum fromDisplayName(String name) {
		KindEnum value = values.get(name);
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
