package com.mattstults.track;

public interface Svgable {
	String toSvg(int indentation);
	
	public interface PathPart { }
	
	public static class Utilities {
		static void indent(StringBuilder builder, int nSpaces) {
			for (int i = 0; i < nSpaces; i++) {
				builder.append(" ");
			}
		}
	}
}
