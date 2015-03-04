package data;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class DataManager {
	
	
	
	public static final String rawKeyPath = "bin\\data\\imageData\\";
	public static final String rawDatabasePath = "bin\\data\\databases\\";
	
	
	
	public static BufferedImage getRawKeyTeethLeftImage(){	
		BufferedImage bufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		
		try {
			bufferedImage = ImageIO.read(new File(rawKeyPath+"rawTeethLeft.jpg"));
		} catch (IOException e) {
			System.out.println("unable to read raw teeth left image from file");
		}
		

		return bufferedImage;
	}
	
	
	
	public static BufferedImage getRawKeyTeethRightImage(){	
		BufferedImage bufferedImage = null;
		
		try {
			bufferedImage = ImageIO.read(new File(rawKeyPath+"rawKeyTeethRight.jpg"));
		} catch (IOException e) {
			System.out.println("unable to read raw teeth right image from file");
		}

		return bufferedImage;
	}



	public static void saveGrayscaleImageToFile(int width, int height, int[] pixels) {
		//DEBUG: Writes grayscale image to file.
		BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster rast = binaryImage.getRaster();
		rast.setPixels(0, 0, width, height, pixels);
		
		try {
			ImageIO.write(binaryImage, "jpg", new File(rawKeyPath+"grayscale.jpg"));
		} catch (IOException e) {
			System.out.println("unable to write grayscale image to file");
		}
	}
	
	
	
	public static void saveBinaryImageToFile(int width, int height, int[] pixels) {
		//DEBUG: Writes binary image to file.
		BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		WritableRaster rast = binaryImage.getRaster();
		rast.setPixels(0, 0, width, height, pixels);
		
		try {
			ImageIO.write(binaryImage, "jpg", new File(rawKeyPath+"binary.jpg"));
		} catch (IOException e) {
			System.out.println("unable to write binary image to file");
		}
	}
	
	
	
	public static void saveCleanedBinaryImageToFile(int width, int height, int[] pixels) {
		//DEBUG: Writes binary image to file.
		BufferedImage binaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		WritableRaster rast = binaryImage.getRaster();
		rast.setPixels(0, 0, width, height, pixels);
		
		try {
			ImageIO.write(binaryImage, "jpg", new File(rawKeyPath+"cleanedBinary.jpg"));
		} catch (IOException e) {
			System.out.println("unable to write binary image to file");
		}
	}



	public static ArrayList<int[][][]> getKeyShapeContexts() {
		ArrayList<int[][][]> keyShapeContexts = new ArrayList<int[][][]>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(rawDatabasePath+"keyShapeContextDatabase.csv"));
			String line = br.readLine();
			
			//Initial information from first line.
			int numKeys = 0;
			int numPoints = 0;
			int numRadialBins = 0;
			int numLogBins = 0;
			
			if(line != null){
				//Initial information, such as the number of keys, the number of points per key, etc.
				//line has form: "numKeys, pointsPerKey, numRadialBins, numLogBins" .
				String[] values = line.split(",");
				numKeys = Integer.parseInt(values[0]);
				numPoints = Integer.parseInt(values[1]);
				numRadialBins = Integer.parseInt(values[2]);
				numLogBins = Integer.parseInt(values[3]);
			}
			
			int lastNumKey = -1;
			int[][][] key = null;
			
			while((line = br.readLine()) != null){
				String[] values = line.split(",");
				int numKey = Integer.parseInt(values[0]);
				int point = Integer.parseInt(values[1]);
				int radialBin = Integer.parseInt(values[2]);
				int logBin = Integer.parseInt(values[3]);
				int frequency = Integer.parseInt(values[4]);
				
				//When there is a new key being traversed.
				if(numKey != lastNumKey){
					key = new int[numPoints][numRadialBins][numLogBins];
					keyShapeContexts.add(key);
					lastNumKey = numKey;
				}
				
				//Update the frequency info for the shape context of this key.
				key[point][radialBin][logBin] = frequency;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return keyShapeContexts;
	}



	/** Saves the shape context of a given key to the key context database,
	 * appending the information to the end.
	 * 
	 * @param shapeContext
	 */
	public static void saveShapeContextToFile(int keyIndex, int[][][] shapeContext) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(rawDatabasePath+"keyShapeContextDatabase.csv", true));
			
			for(int i=0; i<shapeContext.length; i++){
				for(int j=0; j<shapeContext[i].length; j++){
					for(int k=0; k<shapeContext[i][j].length; k++){
						//In order of: keyIndex, point, radialBin, logBin, frequency.
						bw.write(""+keyIndex+","+i+","+j+","+k+","+shapeContext[i][j][k]+"\n");
					}
				}
			}
			
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			System.out.println("Unable to save shape context to file.");
		}
		
	}
	
	
	
	
	
	
	
	
}
