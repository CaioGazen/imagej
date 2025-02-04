import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.gui.ImageWindow;
import java.util.List;
import java.util.ArrayList;

public class Combine_channels_ implements PlugIn {
    public void run(String arg) {
        int[] ids = WindowManager.getIDList();
        if (ids == null || ids.length < 3) {
            IJ.error("Precisam ter 3 imagens abertas.");
            return;
        }

        List<ImagePlus> images = new ArrayList();
        for (int id : ids) {
            ImagePlus img = WindowManager.getImage(id);
            if (img != null && img.getType() == ImagePlus.GRAY8) {
                images.add(img);
            }
        }

        if (images.size() != 3) {
            IJ.error("As imagens precisam ser em escala de cinza (8-bits)");
            return;
        }

        ImagePlus image_Red = images.get(0);
        ImagePlus image_Green = images.get(1);
        ImagePlus image_Blue = images.get(2);

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
