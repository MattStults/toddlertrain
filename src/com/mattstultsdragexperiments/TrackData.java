package com.mattstultsdragexperiments;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.larvalabs.svgandroid.SVGParser;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.view.Display;

public class TrackData {
	public static List<TrackData> readTracks(InputStream trackFile)
			throws SAXException, IOException, ParserConfigurationException {
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.parse(trackFile);
		NodeList nodes = doc.getElementsByTagName("path");
		List<TrackData> paths = new ArrayList<TrackData>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			Node path = node.getAttributes().getNamedItem("d");
			Node centerNode = node.getAttributes().getNamedItem("center");
			float center = centerNode != null
					? Float.parseFloat(centerNode.getTextContent())
					: 0;
			Node cyclesNode = node.getAttributes().getNamedItem("cycles");
			int cycles = cyclesNode != null
					? Integer.parseInt(cyclesNode.getTextContent())
					: 3;
			
			paths.add(new TrackData(path.getTextContent(), center, cycles));
		}
		return Collections.unmodifiableList(paths);
	}

	private final PathMeasure pathMeasure;
	private final Path path;
	private final float center;
	private final int cycles;
	private final RectF bounds;

	private TrackData(String pathDescription, float center, int cycles) {
		pathMeasure = new PathMeasure();
		bounds = new RectF();
		path = SVGParser.parsePath(pathDescription);
		pathMeasure.setPath(path, true);
		computeBoundsSlow(pathMeasure, bounds);
		this.center = center;
		this.cycles = cycles;
	}

	public float getCenter() {
		return center;
	}

	public int getCycles() {
		return cycles;
	}

	public void getPathAtSize(Path out, float width, float height, float buffer) {
		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setTranslate(width / 2 -bounds.centerX(), height / 2 - bounds.centerY());
		out.set(this.path);
		out.transform(scaleMatrix);
		scaleMatrix.reset();
		float scale = Math.min((width - buffer) / bounds.width(), (height - buffer) / bounds.height() );
		scaleMatrix.setScale(scale, scale, width / 2, height / 2);
		out.transform(scaleMatrix);
	}

	private static void computeBoundsSlow(PathMeasure measure, RectF bounds) {
		float pos[] = new float[2];
		float tan[] = new float[2];
		measure.getPosTan(0, pos, tan);
		bounds.left = bounds.right = pos[0];
		bounds.top = bounds.bottom = pos[1];
		float length = measure.getLength();
		int iteratons = 500;
		for (int i = 0; i < iteratons; i++) {
			measure.getPosTan((length * i) / iteratons, pos, tan);
			bounds.left = Math.min(bounds.left, pos[0]);
			bounds.right = Math.max(bounds.right, pos[0]);
			bounds.top = Math.min(bounds.top, pos[1]);
			bounds.bottom = Math.max(bounds.bottom, pos[1]);
		}
	}
}
