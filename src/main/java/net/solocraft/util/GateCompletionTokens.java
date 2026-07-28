package net.solocraft.util;

/**
 * Exact operations for the legacy comma-terminated gate completion list.
 */
public final class GateCompletionTokens {
	private GateCompletionTokens() {
	}

	public static boolean contains(String encodedTokens, String gateId) {
		String token = tokenFor(gateId);
		if (encodedTokens == null || encodedTokens.isEmpty() || token.isEmpty())
			return false;

		int searchFrom = 0;
		while (searchFrom < encodedTokens.length()) {
			int match = encodedTokens.indexOf(token, searchFrom);
			if (match < 0)
				return false;
			if (match == 0 || encodedTokens.charAt(match - 1) == ',')
				return true;
			searchFrom = match + 1;
		}
		return false;
	}

	public static String remove(String encodedTokens, String gateId) {
		if (encodedTokens == null || encodedTokens.isEmpty())
			return encodedTokens == null ? "" : encodedTokens;

		String token = tokenFor(gateId);
		if (token.isEmpty())
			return encodedTokens;

		StringBuilder result = null;
		int copyFrom = 0;
		int searchFrom = 0;
		while (searchFrom < encodedTokens.length()) {
			int match = encodedTokens.indexOf(token, searchFrom);
			if (match < 0)
				break;
			if (match == 0 || encodedTokens.charAt(match - 1) == ',') {
				if (result == null)
					result = new StringBuilder(encodedTokens.length());
				result.append(encodedTokens, copyFrom, match);
				copyFrom = match + token.length();
				searchFrom = copyFrom;
			} else {
				searchFrom = match + 1;
			}
		}

		if (result == null)
			return encodedTokens;
		return result.append(encodedTokens, copyFrom, encodedTokens.length()).toString();
	}

	private static String tokenFor(String gateId) {
		if (gateId == null || gateId.isEmpty() || gateId.indexOf(',') >= 0)
			return "";
		return gateId + ",";
	}
}
