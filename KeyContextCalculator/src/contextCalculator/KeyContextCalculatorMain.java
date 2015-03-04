package contextCalculator;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import data.DataManager;

public class KeyContextCalculatorMain {

	public static void main(String[] args) {		
		BufferedImage image = DataManager.getRawKeyTeethLeftImage();
		
		ArrayList<Point> edges = ImageProcessor.getKeyEdges(image);
		
		int[][][] shapeContext = ShapeContextCalculator.calcShapeContext(edges);
		
		//Change the first parameter to indicate the key index desired for the
		//newest key. Comment/Uncomment to determine whether the key is saved.
		//DataManager.saveShapeContextToFile(2, shapeContext);
		
		//Comment out following lines to remove matching functionality
		ArrayList<int[][][]> keyDatabase = DataManager.getKeyShapeContexts();
		
		int bestMatchIndex = KeyMatcher.matchKey(shapeContext, keyDatabase);
		
		System.out.println("INDEX OF MOST SIMILAR KEY: "+bestMatchIndex);
	}

}
