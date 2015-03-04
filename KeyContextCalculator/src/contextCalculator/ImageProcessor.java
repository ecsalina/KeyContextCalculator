package contextCalculator;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import data.DataManager;

public class ImageProcessor {
	
	
	private static final int RANGE_VALUES = 255;
	private static final int BIN_WIDTH = 5;
	private static final int MAX_CLEANING_DISTANCE = 5;
	private static final int MAX_BOX_WIDTH = 10;
	private static final int MAX_BOX_HEIGHT = 10;
	private static final int BLACK = 0;
	private static final int WHITE = 1;
	private static final int BLADE_DISTANCE_THRESHOLD = 5;
	private static final int BLADE_LENGTH_PROJECTION = 200;
	private static final double ANGLE_OFFSET_THRESHOLD = 0.001;
	
	
	
	public static ArrayList<Point> getKeyEdges(BufferedImage image){
		
		//1. Convert image to grayscale.
		int[] grayscalePixels = convertToGrayscale(image);
		//1.b. DEBUG: Save grayscale image to file.
		DataManager.saveGrayscaleImageToFile(image.getWidth(), image.getHeight(), grayscalePixels);
		
		//2. Implement Otsu's Method to form a binary image from the grayscale image.
		int[] binaryPixels = convertToBinary(grayscalePixels);
		//2.b. DEBUG: save binary image to file.
		DataManager.saveBinaryImageToFile(image.getWidth(), image.getHeight(), binaryPixels);
		
		//3. Clean binary image to remove small black or white blobs
		int[] cleanedBinaryPixels = cleanBinaryPixels(image.getWidth(), image.getHeight(), binaryPixels);
		//3.b. DEBUG: save cleaned binary image to file.
		DataManager.saveCleanedBinaryImageToFile(image.getWidth(), image.getHeight(), cleanedBinaryPixels);
				
		//4. Find all points which constitute an edge. These are where a black pixel is
		//directly next to a white one.
		ArrayList<Point> edges = findEdges(image.getWidth(), cleanedBinaryPixels);
		
		//5. Find only the right edge of the key.
		ArrayList<Point> rightEdge = selectRightEdge(edges);
		
		//6. Calculate the key's overall slant (as an angle in radians) from the vertical.
		double keyAngleOffset = findKeyAngleOffset(rightEdge);
		
		//7. Find the coordinates for the beginning of the blade of the key.
		Point beginningBlade = findBladeBeginning(rightEdge, keyAngleOffset);
		
		
		//**
		//DEBUG: Draw in y line of start of blade on grayscale in black.
		for(int i=0; i<image.getHeight(); i++){
			grayscalePixels[i*image.getWidth() + beginningBlade.x] = 0;
		}
		//DEBUG: Draw in start x line of blade on grayscale in black.
		for(int i=0; i<image.getHeight(); i++){
			grayscalePixels[beginningBlade.y*image.getWidth() + i] = 0;
		}
		//**
		
		
		
		//8. Find the center point of the key based on the horizontal line formed by the x value of
		//the coordinate above.
		Point keyCenter = findKeyCenter(beginningBlade, rightEdge);
		
		
		//**
		//DEBUG: Draw in y line of start of blade on grayscale in black.
		for(int i=0; i<image.getHeight(); i++){
			grayscalePixels[i*image.getWidth() + keyCenter.x] = 0;
		}
		DataManager.saveGrayscaleImageToFile(image.getWidth(), image.getHeight(), grayscalePixels);
		//**
		
		
		
		//9. Remove all edge points which are in the lower left quadrant in comparison to the center point.
		ArrayList<Point> cleanedEdges = cleanEdges(keyCenter, edges);
		
		
		//DEBUG: draw in all other edges.
		for(int i=0; i<cleanedEdges.size(); i++){
			grayscalePixels[cleanedEdges.get(i).y*image.getWidth() + cleanedEdges.get(i).x] = 0;
		}
		DataManager.saveGrayscaleImageToFile(image.getWidth(), image.getHeight(), grayscalePixels);
		
		
		
		return cleanedEdges;
	}
	
	
	
	/** Processes image from RGB BufferedImage into grayscale double[] of pixels for ease
	 * of blob detection later.
	 */
	private static int[] convertToGrayscale(BufferedImage image){
		//Creates grayscale BufferedImage based on initial RGB BufferedImage.
		BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);  
		Graphics g = grayImage.getGraphics();  
		g.drawImage(image, 0, 0, null);  
		g.dispose(); 
		
		int[] pixelsInit = new int[image.getWidth()*image.getHeight()];
		int[] pixels = grayImage.getRaster().getPixels(0, 0, grayImage.getWidth(), grayImage.getHeight(), pixelsInit);
				
		return pixels;
	}
	
	
	
	/** Implements Otsu's Method to convert the grayscale image to binary. May wish to use a more
	 * efficient method for this later.
	 */
	private static int[] convertToBinary(int[] grayscalePixels){
		//Creates histogram of luminosity values with bin widths of BIN_WIDTH. +1 is added so that
		//the bin indexes are truly between (0, max]. The luminosity value for each pixel is rounded up,
		//so that pure white (i.e. 255) is not in a bin by itself. This is because the background
		//will be white, so ideally similar white shades would count as the background. Because each
		//value is rounded up, the max bin index is inclusive, while the lowest (i.e. 0) is exclusive.
		//The bin at index=0 will never be filled.
		int[] histogram = new int[(RANGE_VALUES / BIN_WIDTH)+1];
		
		//Fills histogram based on sample.
		for(int i=0; i<grayscalePixels.length; i++){
			int luminosity = grayscalePixels[i];
			int bin = luminosity / BIN_WIDTH;
			//Below is equivalent to rounding up, as ints are always rounded down. This should be faster
			//than converting to double and using Math.round. Highest brightness value (i.e. 255) is excluded
			//so as to staying within range.
			bin = (luminosity == RANGE_VALUES) ? bin : bin+1;
			
			//Increase frequency.
			histogram[bin]++;
		}
		
		int thresholdValue = otsusMethod(histogram);
		
		int[] binaryPixels = new int[grayscalePixels.length];
		
		//Sets any value above the threshold to 1, and any value equal or below to 0.
		for(int i=0; i<binaryPixels.length; i++){
			if(grayscalePixels[i] > thresholdValue){
				binaryPixels[i] = 1;
			}else{
				binaryPixels[i] = 0;
			}
		}
		
		
		return binaryPixels;
	}
	
	
	
	/** Implementation of actual Otsu's Method. This is a helper method to the binary
	 * converter method.
	 */
	private static int otsusMethod(int[] histogram){
		//This holds values for the Between Class Variance value which dictates
		//the optimal threshold value
		double[] variances = new double[histogram.length];
		
		
		//Finds the Between Class Variance for each potential threshold value.
		for(int thresholdIndex=0; thresholdIndex<histogram.length; thresholdIndex++){
			//Background (though this designation is arbitrary):
			
			//1. Calculates Weight of the background. This is the sum of the number of background
			// pixels, divided by the total number of pixels
			double sumb = 0.0;
			for(int i=0; i<thresholdIndex; i++){
				sumb += histogram[i];
			}
			double Wb = sumb / histogram.length;
			
			//2. Calculates the Mean of the background.
			double Mub = 0.0;
			for(int i=0; i<thresholdIndex; i++){
				Mub += (i*histogram[i]);
			}
			Mub /= sumb;
			
			
			//Foreground:
			
			//1. Calculates Weight of the background. This is the sum of the number of background
			// pixels, divided by the total number of pixels
			double sumf = 0.0;
			for(int i=thresholdIndex; i<histogram.length; i++){
				sumf += histogram[i];
			}
			double Wf = sumf / histogram.length;
			
			//2. Calculates the Mean of the background.
			double Muf = 0.0;
			for(int i=thresholdIndex; i<histogram.length; i++){
				Muf += (i*histogram[i]);
			}
			Muf /= sumf;
			
			
			double betweenClassVariance = Wb*Wf*(Mub-Muf)*(Mub-Muf);
			variances[thresholdIndex] = betweenClassVariance;
		}
		
		
		//Finds the largest variance.
		int maxIndex = -1;
		double maxVariance = -1.0;
		
		for(int i=0; i<variances.length; i++){
			if(variances[i] > maxVariance){
				maxVariance = variances[i];
				maxIndex = i;
			}
		}
		
		//Calculates the actual gray value, as maxIndex is simply the bin number.
		int threshold = maxIndex * BIN_WIDTH;
		
		
		return threshold;
	}
	
	
	
	/** Cleans the binary pixels by removing small black or white blobs
	 */
	private static int[] cleanBinaryPixels(int width, int height, int[] binaryPixels){
		int[] cleanedPixels = binaryPixels.clone();
		
		//This loops through each point in the array. If a point is:
		//	1. bounded on four sides by the opposite color
		//	2. bounded on three sides by the opposite color
		//	3. bounded on opposite sides by the opposite color
		//then its color is changed to that of the opposite color. In
		//implementation really only the last condition needs be checked,
		//as the first and second conditions are only true if the last is
		//true. One iteration should remove small blobs. Keeps iterating
		//until there are no more blobs that can be removed. After this,
		//each point is compared in a similar method above, except that
		//each point is compared to a point x units away from it. This
		//distance was initially 1, now it will be increased iteratively
		//up to 3. This removes large lines and other artefacts that may
		//inadvertently occur due to the prior cleaning processes. Afterwards.
		//only small squares and rectangles are left. The mass of these
		//can be easily calculated, and if the are sufficiently small, they
		//are filled.
		for(int dist=1; dist<MAX_CLEANING_DISTANCE; dist++){
		
			boolean wasAlteration = true;
			while(wasAlteration){
				wasAlteration = false;
				//Traverses through each pixel, checking the surrounding conditions.
				for(int i=0; i<cleanedPixels.length; i++){
					int center = cleanedPixels[i];
					
					//Checking above and below.
					if(i+(width*dist) < cleanedPixels.length && i-(width*dist) >= 0){
						int above = cleanedPixels[i-(width*dist)];
						int below = cleanedPixels[i+(width*dist)];
					
						if(center != above && above == below){
							cleanedPixels[i] = above;
							wasAlteration = true;
						}
					}
					
					//Checking left and right. Makes sure the terms are within bounds of array
					//Also checks that they are on the same line (the /width expressions).
					else if(i+dist < cleanedPixels.length && i-dist >=0		&&
							(i+dist)/width == (i-dist)/width){
						int right = cleanedPixels[i+dist];
						int left = cleanedPixels[i-dist];
						
						if(center != left && left == right){
							cleanedPixels[i] = left;
							wasAlteration = true;
						}
					}
				}
			}
		
		}
		
		//Checks for rectangles and squares. If the width or height of the rectangle is smaller than
		//a mimimum threshold, the whole area is changed color to the surrounding color. This is done
		//first by checking the horizontal lines, and filling each line if smaller than threshold, and
		//then by checking vertical lines, and doing the same.
		
		//For the y direction.
		int y0 = 0;
		for(int i=width; i<cleanedPixels.length; i+=width){
			
			if(cleanedPixels[i] != cleanedPixels[i-width]){
				int heightBox = (i-y0)/width;
				
				//If the height is below the minimum threshold, then fill the line with the prev color.
				if(heightBox < MAX_BOX_HEIGHT){
					fillY(y0, i, y0-width, cleanedPixels, width);
				}
		
				y0 = i;
		
			}
		}
		
		//For the x direction.
		int x0 = 0;
		for(int i=1; i<cleanedPixels.length; i++){
			
			if(cleanedPixels[i] != cleanedPixels[i-1]){
				int widthBox = i-x0;
				
				//If the width is below the minimum threshold, then fill the line with the prev color.
				if(widthBox < MAX_BOX_WIDTH){
					fillX(x0, i, cleanedPixels[x0-1], cleanedPixels);
				}
				
				x0 = i;
			}
			
		}
		
		
		return cleanedPixels;
	}
	
	
	
	/** Helper method to the above cleaning method. Fills in the small rectangles and squares.
	 * Does so at one layer.
	 */
	private static void fillX(int x0, int x1, int color, int[] pixels){
		for(int i=x0; i<x1; i++){
			pixels[i] = color;
		}
	}
	
	/** Helper method to the above cleaning method. Fills in the small rectangles and squares.
	 * Does so for a whole rectangular area.
	 */
	private static void fillY(int y0, int y1, int color, int[] pixels, int width){
		for(int i=y0; i<=y1; i+=width){
			pixels[i] = color;
		}
	}
	
	
	
	/** Finds the edges of the key silhouette by determining every points where a black pixel
	 * is directly next to a white pixel on from either above, below, right, or left.
	 */
	private static ArrayList<Point> findEdges(int width, int[] pixels){
		ArrayList<Point> edges = new ArrayList<Point>();
		
		for(int i=0; i<pixels.length; i++){
			if(pixels[i] == BLACK){	//Only if this pixel is black.
				//Makes sure all indexes are within bounds.
				if(i-width >= 0 && i+width < pixels.length		&& i-1 >= 0 && i+1 < pixels.length){
					
					int above = pixels[i-width];
					int below = pixels[i+width];
					int right = pixels[i+1];
					int left = pixels[i-1];
					
					if(above == WHITE || below == WHITE || right == WHITE || left == WHITE){
						Point point = new Point(i%width, i/width);	//Must contain x and y coordinates.
						edges.add(point);
					}
				}
			}
		}
		
		
		return edges;
	}
	
	
	
	/** Returns an ArrayList with only the right edge of the total edge points entered.
	 * 
	 */
	private static ArrayList<Point> selectRightEdge(ArrayList<Point> totalEdges){
		ArrayList<Point> edges = new ArrayList<Point>();
		
		//Packs the data into a new ArrayList that can be manipulated without worry.
		for(Point p : totalEdges){
			edges.add((Point)p.clone());
		}
		
		//Compares each point in the ArrayList to every other point. If two points have
		//the same y value, then the point with the greatest x value is retained. This
		//maintains only the right side of the key, which has the straight part of the
		//blade that is desired.
		for(int i=0; i<edges.size(); i++){
			boolean iRemoved = false;
			
			for(int j=i+1; j<edges.size(); j++){
				
				if(edges.get(i).y == edges.get(j).y){
					//When j is the rightmost i will never be rightmost, as points were added to the edges Arraylist
					//left to right, top to bottom. Therefore, j, which is after i, will always be more right than i.
					if(edges.get(j).x > edges.get(i).x){
						edges.remove(i);
						iRemoved = true;
						j--;
					}
				}
				
			}
			if(iRemoved){
				i--;
			}
		}
		
		return edges;
	}
	
	
	
	/** Determines the overall key slant from the horizontal. This value is recorded as an
	 * angle from the horizontal in radians. It could be determined from the vertical, which 
	 * is more intuitive, however it is faster to determine this value from the horizontal.
	 * It is determined by finding the highest and lowest points on the edge, and then by
	 * calculating the line which contains both points. This should be close to the actual
	 * vertical center-line of the key (assuming the key is roughly symmetrical). The slope of
	 * this line is used in the inverse tangent function to determine the angle offset in radians.
	 * This offset would ideally be PI/2 (or exactly vertical).
	 */
	private static double findKeyAngleOffset(ArrayList<Point> edges){
		//Since the edge will always be in order from top to bottom, the min and max will always
		//be the first and last points in the ArrayList.
		Point max = edges.get(0);
		Point min = edges.get(edges.size()-1);
		
		double deltaX =  (double)(max.x-min.x);
		double deltaY = (double)(max.y-min.y);
		
		double m;
		double offset;
		
		//Makes sure that NaN will never enter into atan. When deltaX is 0, then there is a vertical
		//line, and the offset will have an angle of PI/2.
		if(deltaX != 0){
			m = deltaY / deltaX;
			offset = Math.atan(m);
		}else{
			offset = Math.PI/2.0;
		}
				
		return offset;
	}
	
	
	
	/** Finds beginning of the blade by finding the long, straight edge which corresponds
	 * to the right side of the blade, since the teeth are on the left. This is the point
	 * where the blade meets the head of the key.
	 */
	private static Point findBladeBeginning(ArrayList<Point> edges, double angleOffset){
		Point bladeBeginning = edges.get(0);
		
		//Finds the slope between two consecutive points and projects it further down
		//the side of the key. If all the points down the key are within a certain tolerance
		//of this projected line, then this must be the key blade. By starting from the top
		//down, the first point to achieve this must be the start of the blade. This method
		//also accounts for slight slanting of the key in the image, though it assumes the
		//head is roughly up while the blade is roughly down.
		for(int i=1; i<edges.size(); i++){
			Point p0 = edges.get(i-1);
			Point p1 = edges.get(i);
			
			
			//Calculates the slope of the line in Slope-intercept form y=mx+b.
			//In the case that p1 is directly above p0, and therefore the slope is Infinity/Does Not Exist,
			//the projected line is vertical. This is not a function, therefore the calculations below are
			//skipped, and the mTest value is set to -1.
			double m;
			double deltaX = (double)(p1.x-p0.x);
			double deltaY = (double)(p1.y-p0.y);
			boolean isVertical = false;
			if(deltaX != 0){
				m = deltaY / deltaX;
			}else{
				m = -1;
				isVertical = true;
			}
			
			//Solving y=mx+b for b yields: b=y-mx. When the line is vertical, the b value is simply a constant
			//which equals any x value.
			double b;
			if(!isVertical){
				b = (double)p0.y-(m*(double)p0.x);
			}else{
				b = (double)p0.x;
			}
			
			//Determines the angle offset from the horizontal (in radians) of the projected line. If
			//this angle is not similar enough to the overall angle offset of the key (as determined
			//by the difference of the projected angle and overall angle being less then a maximum
			//threshold), then the below steps with the line projection are not taken, saving resources
			//and time.
			if(!isVertical){
				double angleOffsetProjected = Math.atan(m);
								
				if(angleOffset-angleOffsetProjected <= ANGLE_OFFSET_THRESHOLD){
					//Continue on with the below projections.
				}else{
					//Move onto the next point.
					continue;
				}
			}else{
				double angleOffsetProjected = Math.PI/2.0;
				
				if(angleOffset-angleOffsetProjected <= ANGLE_OFFSET_THRESHOLD){
					//Continue on with the below projections.
				}else{
					//Move onto the next point.
					continue;
				}
			}
			
			
			
			//Calculates distance from projected line for each point down the key side. This is done
			//by calculating another line, which intersects the projected line at a right angle and
			//contains the test point.
			boolean withinBounds = true;
			for(int j=i+1; j<i+BLADE_LENGTH_PROJECTION && j<edges.size(); j++){
				Point test = edges.get(j);
				//The distance between the point and the projected line must be perpendicular to the
				//projected line, so the slope for the test line equation must be -1/m of the projected line.
				//If the projected line is vertical, then the perpendicular slope must be 0.
				double mTest;
				if(!isVertical){
					mTest = -1/m;
				}else{
					mTest = 0;
				}
				double bTest = (double)test.y-(mTest*(double)test.x);
				//The intersection of the two lines is where y=mx+b == y=mTestx+bTest Solving mx+b=mTestx+bTest
				//for x yields: x=(bTest-b)/(m-mTest). Again, when the original projected line is vertical, it will
				//have no true slope m but will instead contain a constant as the b value, which corresponds to any
				//x value of the vertical line. The perpendicular line will have a slope of 0, and a constant bTest
				//value as well, which corresponds to any y value. Therefore, the point (b, bTest) will be the intersect
				//point.
				double intersectX;
				double intersectY;
				if(!isVertical){
					intersectX = (bTest-b)/(m-mTest);
					//Plugging x into either equation yields the y value.
					intersectY = m*intersectX + b;
				}else{
					intersectX = b;
					intersectY = bTest;
				}
				
				
				//Finally, the Pythagorean Theorem is employed to find the total distance.
				double distanceX = intersectX-test.x;
				double distanceY = intersectY-test.y;
				double distanceTot = Math.sqrt(distanceX*distanceX + distanceY*distanceY);
				
				//This is done this way so that if an error ensues and distanceTot comes out to not be
				//a true number (such as when divide by zero error occurs), it will still be considered
				//out of bounds.
				if(distanceTot <= BLADE_DISTANCE_THRESHOLD){
					continue;
				}else{
					withinBounds = false;
					break;
				}
			}
			
			if(withinBounds){
				bladeBeginning = p0;
				break;
			}
		}
		
		System.out.println("BEGIN BLADE: "+bladeBeginning);
		
		
		return bladeBeginning;
	}
	
	
	
	/** Finds the center, vertical dividing line of the key. This is done by finding and returning
	 *  the lowest point on the blade, which should be the tip.
	 */
	private static Point findKeyCenter(Point bladeBeginning, ArrayList<Point> edges){
		Point min = edges.get(0);
		
		for(Point p : edges){
			if(p.y > min.y){
				min = p;
			}
		}
		
		Point keyCenter = new Point(min.x, bladeBeginning.y);
		//DEBUG:
		System.out.println("KEY CENTER: "+min);
		
		return keyCenter;
	}
	
	
	
	/** Returns an ArrayList of Points containing all the edges of the key except for those which are
	 * in the lower left quadrant in comparison to the key center. This essentially removes the part
	 * of the blade containing the teeth, which are not useful in comparing keys.
	 */
	private static ArrayList<Point> cleanEdges(Point keyCenter, ArrayList<Point> edges){
		ArrayList<Point> cleanedEdges = new ArrayList<Point>();
		
		for(Point p : edges){
			if(p.x > keyCenter.x || p.y < keyCenter.y){
				cleanedEdges.add(p);
			}
		}
		
		return cleanedEdges;
	}
	
	
	
	
	
	
	
	
	
	
}
