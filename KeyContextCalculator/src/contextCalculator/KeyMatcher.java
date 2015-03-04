package contextCalculator;

import java.util.ArrayList;

public class KeyMatcher {

	
	/** Finds the best most similar key from the database, based on calculations performed
	 * on the shape context of each key (implementing X^2 statistic and Hungarian Method).
	 * 
	 * @param shapeContext
	 * @param keyDatabase
	 * @return index within the supplied database array list of the key which is most similar.
	 */
	public static int matchKey(int[][][] shapeContextOriginal, ArrayList<int[][][]> keyDatabase){
		double[] costs = new double[keyDatabase.size()];
		
		for(int i=0; i<keyDatabase.size(); i++){
			//Calculates ChiSquared values of each key in Database, and then find optimal configuration
			//based on the Hungarian Method.
			double cost = calculateMinCostMatrix(shapeContextOriginal, keyDatabase.get(i));
			costs[i] = cost;
		}
		
		//find the smallest cost, and assign that key as the matching key.
		int minIndex = 0;
		double minValue = costs[minIndex];
		
		for(int i=0; i<keyDatabase.size(); i++){
			System.out.println("key: "+i+", cost: "+costs[i]);
			if(costs[i] < minValue){
				minValue = costs[i];
				minIndex = i;
			}
		}
		
		return minIndex;
	}

	
	
	/** Calculates the Cost Matrix for the original and test key. This is evaluated with the Chi Squared
	 * value of between the histograms of each point being compared in each key. The Hungarian Method is
	 * implemented to calculate the smallest Cost Matrix for the key comparison.
	 * 
	 * @param shapeContextOriginal
	 * @param keyDatabase
	 * @return
	 */
	private static double calculateMinCostMatrix(int[][][] shapeContextOriginal, int[][][] shapeContextTest){
		double[][] costMatrices = new double[shapeContextOriginal.length][shapeContextTest.length];
			
		//Traverses through each point on the original key.
		for(int i=0; i<shapeContextOriginal.length; i++){
			//traverses through each point on the test key.
			for(int j=0; j<shapeContextTest.length; j++){
				
				double sum = 0.0;	//Sums the chi values of each histogram.
				
				//For each shape context, must traverse through each bin.
				for(int k=0; k<shapeContextOriginal[i].length; k++){
					for(int h=0; h<shapeContextOriginal[i][k].length; h++){
						
						double numerator = (shapeContextOriginal[i][k][h] - shapeContextTest[j][k][h])
											*(shapeContextOriginal[i][k][h] - shapeContextTest[j][k][h]);
						double denominator = (shapeContextOriginal[i][k][h] + shapeContextTest[j][k][h]);
						double value = numerator / denominator;
						
						//when the two shape context bins have no frequency (freq=0) then the denominator
						//is equal to zero. A divide by zero error ensues. This statement corrects that issue.
						if(denominator == 0){	value = 0;	}
						
						sum += value;
					}
				}
				
				//Calculate cost matrix for this point comparison, and store for later
				//manipulate in an array of doubles.
				double costMatrix = (0.5)*(sum);
				costMatrices[i][j] = costMatrix;
			}
		}
		
		
		//Calculate the actual minimum cost efficiencies. 
		HungarianAlgorithm hungarian = new HungarianAlgorithm(costMatrices);
		int[] minCostMatrices = hungarian.execute();
		
		//Sum the values to arrive at the total minCostMatrix.
		double minCostMatrix = 0.0;
		
		for(int i=0; i<minCostMatrices.length; i++){
			int costIndex = minCostMatrices[i];
			double cost = costMatrices[i][costIndex];
			minCostMatrix += cost;
		}
		
		return minCostMatrix;
	}
	
	

}
