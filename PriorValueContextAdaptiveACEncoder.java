package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import ac.ArithmeticEncoder;
import io.OutputStreamBitSink;

public class PriorValueContextAdaptiveACEncoder{

	public static void main(String[] args) throws IOException {
		String input_file_name = "data/out.dat";
		String output_file_name = "data/PriorValueContextAdaptive-compressed.dat";

		int range_bit_width = 40;

		System.out.println("Encoding text file: " + input_file_name);
		System.out.println("Output file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		
		int total_pixels = (int) new File(input_file_name).length(); //total number of symbols in file
		
		Integer[] pixel_values = new Integer[256];
		
		for (int i=0; i<256; i++) {
			pixel_values[i] = i;
		}
		
		// Create 256 models. Model chosen depends on value of pixel prior to the 
		// pixel being encoded.
		
		FreqCountIntegerSymbolModel[] models = new FreqCountIntegerSymbolModel[256];
		
		for (int i=0; i<256; i++) {
			// Create new model with default count of 1 for all pixels
			models[i] = new FreqCountIntegerSymbolModel(pixel_values);
		}

		ArithmeticEncoder<Integer> encoder = new ArithmeticEncoder<Integer>(range_bit_width);

		FileOutputStream fos = new FileOutputStream(output_file_name);
		OutputStreamBitSink bit_sink = new OutputStreamBitSink(fos);

		int total_frames = 300; // 30 frames per second, 10 seconds = 300 frames
				
		// First 4 bytes are the number of symbols encoded
		bit_sink.write(total_pixels, 32);	
		
		// Next 2 bytes are the number of frames
		bit_sink.write(total_frames, 16);

		// Next byte is the width of the range registers
		bit_sink.write(range_bit_width, 8);
		
		// Now encode the input
		FileInputStream fis = new FileInputStream(input_file_name);
		
		int pix_in_each_frame = 4096; // 64 x 64 pixels in each frame = 4096 pixels
		
		// Create prior_frame to keep track of pixel values in prior frame
		Integer[] prior_frame = new Integer[pix_in_each_frame];

		// Initialize all pixel values of prior_frame to 0
		for(int i=0; i<pix_in_each_frame; i++){
			prior_frame[i]= 0;
		}
		
		// Encoding (nested) loop that walks through each frame and within that, each pixel value
		FreqCountIntegerSymbolModel model;
		int pixel_value;
		
		// Prior frame for very first frame will be nothing (all black/0s for pixel values)
		for (int i=0; i<total_frames; i++) {
			for(int j=0; j<pix_in_each_frame; j++ ){
				
				pixel_value = fis.read();
				
				// Set model based on value of the pixel 
				// in the same position of the prior frame
				model = models[prior_frame[j]];
				
				// Encode pixel_value based on model
				encoder.encode(pixel_value, model, bit_sink);
				
				// Update model used
				model.addToCount(pixel_value);
				
				// Now update prior_frame at this position to be this pixel value 
				prior_frame[j]= pixel_value;
			}
		}
		
		fis.close();
		
		// Finish off by emitting the middle pattern 
		// and padding to the next word
		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
		
		System.out.println("Done");
	}
		
}
