import ij.*;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.gui.ImageWindow;

public class Split_channels_ implements PlugIn {
	public void run(String arg) {
		ImagePlus image = IJ.getImage();
		split_image(image, image.getWidth(), image.getHeight());
	}
	

	public void split_image(ImagePlus image, int width, int height){
		ImageProcessor processor = image.getProcessor();
		int valorPixel[] = {0,0,0};

		//IJ.error("inside split_image");
		
		//IJ.error("inside for loop");
		
    ImageProcessor channel_Red = new ByteProcessor(width, height);
    ImageProcessor channel_Green = new ByteProcessor(width, height);
    ImageProcessor channel_Blue = new ByteProcessor(width, height);

    System.err.println(width);
    System.err.println(height);
    //IJ.error("after processor");

    for(int x = 0; x < width; x++){
      for(int y = 0; y < height; y++){
        processor.getPixel(x, y, valorPixel);
        channel_Red.putPixel(x, y, valorPixel[0]);
        channel_Green.putPixel(x, y, valorPixel[1]);
        channel_Blue.putPixel(x, y, valorPixel[2]);
      }
    }

    ImagePlus image_Red = new ImagePlus("Red channel", channel_Red);
    ImagePlus image_Green = new ImagePlus("Green channel", channel_Green);
    ImagePlus image_Blue = new ImagePlus("Blue channel", channel_Blue);
    
    new ImageWindow(image_Red);
    new ImageWindow(image_Green);
    new ImageWindow(image_Blue);


	}
}
