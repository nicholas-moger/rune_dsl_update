package holdout.tostringoverdefaultenum;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The side (the chaos s31 keyword-named values).
 * @version 0.0.0
 */
@RosettaEnum("SideEnum")
public enum SideEnum {

	@RosettaEnumValue(value = "Long") 
	LONG("Long", null),
	
	@RosettaEnumValue(value = "Short") 
	SHORT("Short", null)
;
	private static Map<String, SideEnum> values;
	static {
        Map<String, SideEnum> map = new ConcurrentHashMap<>();
		for (SideEnum instance : SideEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	SideEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static SideEnum fromDisplayName(String name) {
		SideEnum value = values.get(name);
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
