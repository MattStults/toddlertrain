package com.mattstults.track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mattstults.track.Svgable.PathPart;

public abstract class SvgContainer<T> implements Svgable{
	protected final List<T> contents;
	protected final Metadata metadata;
	
	protected SvgContainer(List<T> contents, Metadata metadata) {
		this.contents = Collections.unmodifiableList(contents);
		this.metadata = metadata;
	}
	
	public static class Path extends SvgContainer<PathPart> {
		protected Path(List<PathPart> contents, Metadata metadata) {
			super(contents, metadata);
		}

		@Override
		public String toSvg(int indentation) {
			StringBuilder ret = new StringBuilder();
			Svgable.Utilities.indent(ret, indentation);
			ret.append("<path d=\"");
			for (PathPart part : contents) {
				ret.append(part.toString()).append(" ");
			}
			ret.append("\"\n");
			Svgable.Utilities.indent(ret, indentation+4);
			ret.append(metadata.toString());
			ret.append("/>\n");
			return ret.toString();
		}
	}
	
	public static class Group extends SvgContainer<Svgable> {
		protected Group(List<Svgable> contents, Metadata metadata) {
			super(contents, metadata);
		}

		@Override
		public String toSvg(int indentation) {
			StringBuilder ret = new StringBuilder();
			Svgable.Utilities.indent(ret, indentation);
			ret.append("<g ").append(metadata.toString()).append(">\n");
			for (Svgable part : contents) {
				ret.append(part.toSvg(indentation+2));
			}
			Svgable.Utilities.indent(ret, indentation);
			ret.append("</g>\n");
			return ret.toString();
		}
	}
}
