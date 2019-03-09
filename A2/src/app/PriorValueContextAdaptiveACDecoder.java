package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import ac.ArithmeticDecoder;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class PriorValueContextAdaptiveACDecoder {

	public static void main(String[] args) throws InsufficientBitsLeftException, IOException {
		String input_file_name = "data/PriorValueContextAdaptive-compressed.dat";
		String output_file_name = "data/PriorValueContextAdaptive-reuncompressed.dat";

		FileInputStream fis = new FileInputStream(input_file_name);

		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		Integer[] pixel_values = new Integer[256];
		
		for (int i=0; i<256; i++) {
			pixel_values[i] = i;
		}

		// Create 256 models. Model chosen depends on value of pixel prior to 
		// pixel being encoded.
		
		FreqCountIntegerSymbolModel[] models = new FreqCountIntegerSymbolModel[256];
		
		for (int i=0; i<256; i++) {
			// Create new model with default count of 1 for all symbols
			models[i] = new FreqCountIntegerSymbolModel(pixel_values);
		}
		
		// Read in number of pixel values encoded
		int total_pixels = bit_source.next(32);
		
		// Read in number of frames
		int total_frames = bit_source.next(16);

		// Read in range bit width and setup the decoder
		int range_bit_width = bit_source.next(8);
		
		ArithmeticDecoder<Integer> decoder = new ArithmeticDecoder<Integer>(range_bit_width);

		// Decode and produce output.
		System.out.println("Uncompressing file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		System.out.println("Number of encoded symbols: " + total_pixels);
		
		FileOutputStream fos = new FileOutputStream(output_file_name);
		
		int pix_in_each_frame = 4096;
		
		// Create prior_frame to keep track of pixel values in prior frame
		Integer[] prior_frame = new Integer[pix_in_each_frame];

		// Initialize all pixel values of prior_frame to 0
		for(int i=0; i<pix_in_each_frame; i++){
			prior_frame[i]= 0;
		}
		
		// Decoding (nested) loop that walks through each frame and within that, each pixel value
		
		FreqCountIntegerSymbolModel model;
		int pixel_value;
		
		for (int i=0; i<total_frames; i++)	
			for (int j=0; j<pix_in_each_frame; j++) {
				
				model = models[prior_frame[j]];
				pixel_value = decoder.decode(model, bit_source);
				fos.write(pixel_value);
				
				// Update model used
				model.addToCount(pixel_value);
				
				// Update prior frame
				prior_frame[j] = pixel_value;
			}

		System.out.println("Done.");
		fos.flush();
		fos.close();
		fis.close();
	}
}