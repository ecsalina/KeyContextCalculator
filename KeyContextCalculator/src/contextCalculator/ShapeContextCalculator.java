package contextCalculator;

import java.awt.Point;
import java.util.ArrayList;

public class ShapeContextCalculator {

	
	private static final int NUM_POINTS = 200;
	private static final int NUM_RADIAL_BINS = 12;
	private static final int NUM_LOG_BINS = 5;
	private static final int LOG_SCALE_FACTOR = 10;
	
	
	
	public static int[][][] calcShapeContext(ArrayList<Point> edges) {
		
		//1. Select NUM_POINTS points evenly from around edge of key.
		Point[] points = selectPoints(edges);
		
		//2. Calculate log-polar histogram for each point.
		int[][][] logPolarHistograms = calculateLogPolarHistograms(points);
		
		//3. Return histograms.
		return logPolarHistograms;
	}
	
	
	
	/** Selects a standard number of reference points from the edge.
	 */
	private static Point[] selectPoints(ArrayList<Point> edge){
		int stepSize = edge.size()/NUM_POINTS;
		
		Point[] selectedPoints = new Point[NUM_POINTS];
		
		for(int i=0; i<NUM_POINTS; i++){
			selectedPoints[i] = edge.get(i*stepSize);
		}
		
		return selectedPoints;
	}
	
	
	
	/** Calculates log-polar histogram of each point by comparing distance and angle from
	 * all other points on the shape.
	 */
	private static int[][][] calculateLogPolarHistograms(Point[] points){
		int[][][] histograms = new int[NUM_POINTS][NUM_RADIAL_BINS][NUM_LOG_BINS];
		
		//a. Calculate the distance between each point. This is then averaged to
		//determine the mean distance. The mean distance is then used to normalize
		//the distances.
		
		double distances[][] = new double[NUM_POINTS][NUM_POINTS];
		double sumDistances = 0.0;
		//i. Calculate raw distance and sum.
		for(int i=0; i<distances.length; i++){
			for(int j=0; j<distances.length; j++){
				int piX = points[i].x;
				int piY = points[i].y;
				int pjX = points[j].x;
				int pjY = points[j].y;
				int diffX = pjX-piX;
				int diffY = pjY-piY;
				double distance = Math.sqrt(diffX*diffX + diffY*diffY);
				
				distances[i][j] = distance;
				sumDistances += distance;
			}
		}
		double meanDistance = sumDistances / (NUM_POINTS*NUM_POINTS);
		System.out.println("mean: "+meanDistance);
		//ii. Normalize sum with mean distance. Then multiply by LOG_SCALE_FACTOR
		//to get some variation among log bins later.
		for(int i=0; i<distances.length; i++){
			for(int j=0; j<distances.length; j++){
				distances[i][j] /= meanDistance;
				distances[i][j] *= LOG_SCALE_FACTOR;
			}
		}
		
		
		for(int i=0; i<points.length; i++){
			//b. Calculate the tangent line between point i and previous point. This
			//is used as the baseline for the angle determination.
			int prevIndex = (i-1 < 0) ? points.length-1 : i-1;
			Point p0 = points[prevIndex];
			Point p1 = points[i];
			//Based on slope-intercept form y=mx+b.
			double m = (double)(p1.y-p0.y) / (double)(p1.x-p0.x);
			
			//Calculate the base angle in relation to the horizontal of
			//the tangent line from part a. This is needed for calculations 
			//in the loop below.
			double baseAngle = findAngle(m, 1);
			
			for(int j=0; j<points.length; j++){
				if(i != j){
					int piX = points[i].x;
					int piY = points[i].y;
					int pjX = points[j].x;
					int pjY = points[j].y;
					
					
					//c. Calculate the angle.
					//i. First transform point j coordinates to be in relation to those
					//of point i (i.e., point i is now the origin). Now i is the origin
					//so angles will be properly centered around i.
					pjX -= piX;
					pjY -= piY;
					double rawAngle = findAngle(pjX, pjY);
					//ii. Normalize the raw angle with the base angle. Then double check result
					//is positive (which may not always occur). If is not positive, convert angle
					//to a positive angle (within range of [0,2PI) ).
					double angle = rawAngle-baseAngle;
					angle = (angle < 0) ? angle += 2*Math.PI : angle;
					//iii. Convert to bins. If there is a floating point error and the bin comes
					//out as 12, it is rounded down to 11.
					int radialBin = (int)(angle * (NUM_RADIAL_BINS)/(2*Math.PI));
					radialBin = (radialBin == 12) ? 11 : radialBin;
					
					
					//d. Find log-bin of previously calculated and normalized distance. If the distance
					//is a decimal, then the log will be negative. Therefore, set negative logs to bin of
					//0.
					int logBin = (int)Math.log(distances[i][j]);	//Auto-rounds down.
					logBin = (logBin < 0) ? 0 : logBin;
					
					
					//e. Update the respective bin.
					histograms[i][radialBin][logBin]++;
				}
			}
		}
		
		
		return histograms;
	}
	
	
	
	/** Auxiliary method to get the angle from the horizontal based on cartesian
	 * coordinates x and y. Returns values between [0, 2PI). 
	 */
	private static double findAngle(double x, double y){
		//Maintain a record of what quadrant j is in, as tangent
		//inverse is used next, and will not necessarily return correct values.
		int quad = 0;
		if(x > 0 && y > 0){
			quad = 1;
		}else if(x < 0 && y > 0){
			quad = 2;
		}else if(x < 0 && y < 0){
			quad = 3;
		}else if(x > 0 && y < 0){
			quad = 4;
		}
		//Next find the raw angle in relation to the horizontal using
		//tangent inverse. This is in radians.
		double angle = Math.atan(y/x);
		
		//Normalize the angle to be within the range [0, 2PI).
		if(quad == 2){
			angle += Math.PI;
		}else if(quad == 3){
			angle += Math.PI;
		}else if(quad == 4){
			angle += 2*Math.PI;
		}
		
		
		return angle;
	}
	
	
	
	
	
	
	
	
	
	
	
}
