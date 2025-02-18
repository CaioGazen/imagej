import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.gui.*;
import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Histogram_equalization_ implements PlugIn {
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

    ImagePlus image = WindowManager.getImage(title);

    int width = image.getWidth();
    int height = image.getHeight();

    ImageProcessor processor = image.getProcessor();

    switch (answer) {
      case "Average":
        
        break;

      case "Weighted Average":
        
        break;

      case "Luminance":
        
        break;

      default:
        break;
    }

    if(createNewImage){
      ImagePlus image_new = new ImagePlus("Grayscale", processor_grayscale);
      new ImageWindow(image_new);
    }
    else{
        image.setProcessor(processor_grayscale);
        image.updateAndDraw();
    }

  }

  public Array 
}

