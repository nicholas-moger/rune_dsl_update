package chaos.s02.a1o3;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Directions with displayName and docs.
 * @version 1.0.0
 */
@RosettaEnum("C2DirEnum")
public enum C2DirEnum {

	/**
	 * Buy side.
	 */
	@RosettaEnumValue(value = "Buy", displayName = "BUY") 
	BUY("Buy", "BUY"),
	
	@RosettaEnumValue(value = "Sell", displayName = "SELL") 
	SELL("Sell", "SELL"),
	
	/**
	 * No action.
	 */
	@RosettaEnumValue(value = "Hold") 
	HOLD("Hold", null)
;
	private static Map<String, C2DirEnum> values;
	static {
        Map<String, C2DirEnum> map = new ConcurrentHashMap<>();
		for (C2DirEnum instance : C2DirEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C2DirEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C2DirEnum fromDisplayName(String name) {
		C2DirEnum value = values.get(name);
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
