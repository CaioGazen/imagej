import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.gui.*;
import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Nonlinear_Filters_ implements PlugIn {
  private int height;
  private int width;

  public void run(String arg) {
    int[] ids = WindowManager.getIDList();
    if (ids == null || ids.length < 1) {
      IJ.error("Precisa ter 1 ou mais imagens abertas.");
      return;
    }

    String[] imageTitles = new String[ids.length];

    for (int i = 0; i < ids.length; i++) {
      ImagePlus imp = WindowManager.getImage(ids[i]);
      imageTitles[i] = imp.getTitle();
    }

    GenericDialog gd = new GenericDialog("Choose an Image");
    gd.addChoice("Image", imageTitles, imageTitles[0]);
    gd.showDialog();

    if (gd.wasCanceled()) {
      return;
    }

    String title = gd.getNextChoice();

    ImagePlus image = WindowManager.getImage(title);

    this.width = image.getWidth();
    this.height = image.getHeight();

    ImageProcessor processor = image.getProcessor();
    ImageProcessor VerticalProcessor = processor.duplicate();
    ImageProcessor HorizontalProcessor = processor.duplicate();

    int[][] kernelVertical = { { -1, 0,  1 },
                               { -2, 0,  2 },
                               { -1, 0,  1 } };

    int[][] kernelHorizontal = { {  1,  2,  1 },
                                 {  0,  0,  0 },
                                 { -1, -2, -1 } };

    
    VerticalProcessor = applyKernel(VerticalProcessor, kernelVertical, 1);
    HorizontalProcessor = applyKernel(HorizontalProcessor, kernelHorizontal, 1);

    

    ImagePlus SobelVerticalImage = new ImagePlus("Sobel Vertical", VerticalProcessor);
    ImagePlus SobelHorizontalImage = new ImagePlus("Sobel Horisontal", HorizontalProcessor);
    
    ImagePlus SobelImage = new ImagePlus("Sobel", JoinImages(VerticalProcessor, HorizontalProcessor));
    
    new ImageWindow(SobelVerticalImage);
    new ImageWindow(SobelHorizontalImage);

    new ImageWindow(SobelImage);

  }

  public ImageProcessor applyKernel(ImageProcessor processor, int[][] kernel, int kernelDivisor) {
    int kernelOffset = ((kernel.length - 1) / 2);
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);
    for (int y = kernelOffset; y < this.height - kernelOffset; y++) {
      for (int x = kernelOffset; x < this.width - kernelOffset; x++) {
        int sum = 0;
        for (int ky = -kernelOffset; ky <= kernelOffset; ky++) {
          for (int kx = -kernelOffset; kx <= kernelOffset; kx++) {
            int pixel = processor.getPixel(x + kx, y + ky);
            sum += pixel * kernel[ky + kernelOffset][kx + kernelOffset];
          }
        }
        newProcessor.putPixel(x, y, sum / kernelDivisor);
      }
    }

    return newProcessor;
  }
  
  public ImageProcessor JoinImages(ImageProcessor processor1, ImageProcessor processor2){
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);

    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        int value1 = processor1.getPixel(x, y);
        int value2 = processor2.getPixel(x, y);
        int newValue = (int) Math.round(Math.sqrt((value1*value1) + (value2*value2)));
        newProcessor.putPixel(x, y, newValue);
      }
    }

    return newProcessor;
  }
}
