package io.github.tt432.eyelib.common.bedrock.model.pojo;

import java.io.IOException;

/**
 * If not specifying vertex indices, arrays of data must be a list of tris or
 * quads, set by making this property either "tri_list" or "quad_list"
 */
public enum PolysEnum {
	QUAD_LIST, TRI_LIST;

	public String toValue() {
		return switch (this) {
			case QUAD_LIST -> "quad_list";
			case TRI_LIST -> "tri_list";
		};
	}

	public static PolysEnum forValue(String value) throws IOException {
		if (value.equals("quad_list"))
			return QUAD_LIST;
		if (value.equals("tri_list"))
			return TRI_LIST;
		throw new IOException("Cannot deserialize PolysEnum");
	}
}
