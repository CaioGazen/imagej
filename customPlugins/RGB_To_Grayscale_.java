import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.gui.*;
import java.util.List;
import java.util.ArrayList;

public class RGB_To_Grayscale_ implements PlugIn {
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
        String[] items = {"Average", "Weighted Average", "Luminance"};
        gd.addRadioButtonGroup("Test", items, 3, 1, "0");
        gd.addCheckbox("Create new image", false);
        gd.showDialog();

        if (gd.wasCanceled()) {
             return;
        }

        String title = gd.getNextChoice();
        String answer = gd.getNextRadioButton();
        Boolean createNewImage = gd.getNextBoolean();

        IJ.error(answer);

        ConversionMethods method = this::rgb_average;

        switch (answer) {
          case "Average":
            method = this::rgb_average;
            break;
          case "Weighted Average":
            method = this::rgb_luminance;
            break;
          case "Luminance":
            method = this::rgb_luminance_digital;
            break;
          default:
            break;
        }

        ImagePlus image = WindowManager.getImage(title);

        int width = image.getWidth();
        int height = image.getHeight();

        ImageProcessor processor = image.getProcessor();

        ImageProcessor processor_grayscale = method.conversion(image.getProcessor(), width, height);
        
        if(createNewImage){
            ImagePlus image_new = new ImagePlus("Grayscale", processor_grayscale);
            new ImageWindow(image_new);
        }
        else{
            image.setProcessor(processor_grayscale);
            image.updateAndDraw();
        }
      
    }

    public ImageProcessor rgb_average(ImageProcessor processor, int width, int height){

        ImageProcessor processor_grayscale = new ByteProcessor(width, height);

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                int gray = (rgb[0] + rgb[1] + rgb[2])/3;
                processor_grayscale.putPixel(x, y, gray);
            }
        }

      return processor_grayscale;
    }
    
    public int luminance(int[] rgb){
      double Yd = rgb[0] * 0.299 + rgb[1] * 0.587 + rgb[2] * 0.0114;
      int Y = (int)Math.round(Yd);
      return Y;
    }

    public int luminance_digital(int[] rgb){
      double Yd = rgb[0] * 0.2125 + rgb[1] * 0.7154 + rgb[2] * 0.0721;
      int Y = (int)Math.round(Yd);
      return Y;
    }

    public ImageProcessor rgb_luminance(ImageProcessor processor, int width, int height){

        ImageProcessor processor_grayscale = new ByteProcessor(width, height);

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                int Y = luminance(rgb);
                processor_grayscale.putPixel(x, y, Y);
            }
        }

      return processor_grayscale;
    }

    public ImageProcessor rgb_luminance_digital(ImageProcessor processor, int width, int height){

        ImageProcessor processor_grayscale = new ByteProcessor(width, height);

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int[] rgb = new int[3];
                processor.getPixel(x, y, rgb);
                int Y = luminance_digital(rgb);
                processor_grayscale.putPixel(x, y, Y);
            }
        }

      return processor_grayscale;
    }

    private interface ConversionMethods {
     
      ImageProcessor conversion(ImageProcessor processor, int width, int height);
    }
}
