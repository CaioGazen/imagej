import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.gui.*;
import java.util.List;
import java.util.ArrayList;

public class Combine_channels_ implements PlugIn {
    public void run(String arg) {
        int[] ids = WindowManager.getIDList();
        if (ids == null || ids.length < 3) {
            IJ.error("Precisam ter 3 imagens abertas.");
            return;
        }

        String[] imageTitles = new String[ids.length];

        for (int i = 0; i < ids.length; i++) {
            ImagePlus imp = WindowManager.getImage(ids[i]);
            imageTitles[i] = imp.getTitle();
        }

        GenericDialog gd = new GenericDialog("Choose an Image");
        gd.addChoice("Image: Red:", imageTitles, imageTitles[0]); // Default selection
        gd.addChoice("Image: Green", imageTitles, imageTitles[0]); // Default selection
        gd.addChoice("Image: Blue", imageTitles, imageTitles[0]); // Default selection
        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        String title_Red = gd.getNextChoice();
        String title_Green = gd.getNextChoice();
        String title_Blue = gd.getNextChoice();

        ImagePlus image_Red = null;
        ImagePlus image_Green = null;
        ImagePlus image_Blue = null;

        for (int i = 0; i < ids.length; i++) {
            ImagePlus imp = WindowManager.getImage(ids[i]);
            if (imp.getTitle().equals(title_Red)) {
                image_Red = imp;
            }
            if (imp.getTitle().equals(title_Green)) {
                image_Green = imp;
            }
            if (imp.getTitle().equals(title_Blue)) {
                image_Blue = imp;
            }
        }

        

        int width = image_Red.getWidth();
        int height = image_Red.getHeight();

        if (width != image_Green.getWidth() || width != image_Blue.getWidth() ||
            height != image_Green.getHeight() || height != image_Blue.getHeight()) {
            IJ.error("As 3 imagens devem ter o mesmo tamanho");
            return;
        }

        ImageProcessor processor_Red = image_Red.getProcessor();
        ImageProcessor processor_Green = image_Green.getProcessor();
        ImageProcessor processor_Blue = image_Blue.getProcessor();

        ColorProcessor processor_RGB = new ColorProcessor(width, height);
    
        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int r = processor_Red.getPixel(x, y);
                int g = processor_Green.getPixel(x, y);
                int b = processor_Blue.getPixel(x, y);
                int[] rgb = {r,g,b};
                processor_RGB.putPixel(x, y, rgb);
            }
        }

        ImagePlus image_RGB = new ImagePlus("RGB Image", processor_RGB);

        new ImageWindow(image_RGB);
    }
}
