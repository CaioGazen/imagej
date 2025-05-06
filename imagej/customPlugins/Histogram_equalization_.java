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
    String[] items = {"Expand Histogram", "Equalize Histogram"};
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

    this.width = image.getWidth();
    this.height = image.getHeight();

    ImageProcessor processor = image.getProcessor();
    ImageProcessor newProcessor = processor.duplicate();

    int[] histogram = computeHistogram(newProcessor);
    int[] lowHigh = lowHigh(histogram);

    switch (answer) {
      case "Expand Histogram":
        expandHistogram(0, 255, lowHigh[0], lowHigh[1], histogram, newProcessor);
        break;

      case "Equalize Histogram":
        equalizeHistogram(255, histogram, newProcessor);
        break;

      default:
        break;
    }

    if(createNewImage){
      ImagePlus image_new = new ImagePlus(answer, newProcessor);
      new ImageWindow(image_new);
    }
    else{
        image.setProcessor(newProcessor);
        image.updateAndDraw();
    }

  }

  public int[] computeHistogram(ImageProcessor processor) {
    int[] histogram = new int[256];
    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        int value = processor.getPixel(x, y);
        histogram[value]++;
      }
    }
    return histogram;
  }

  public int[] lowHigh(int[] histogram) {
    int low = 0;
    int high = 255;
    for (int i = 0; i < 256; i++) {
      if (histogram[i] > 0) {
        low = i;
        break;
      }
    }
    for (int i = 255; i >= 0; i--) {
      if (histogram[i] > 0) {
        high = i;
        break;
      }
    }
    return new int[] {low, high};
  }

  public void expandHistogram(int min, int max, int low, int high, int[] histogram, ImageProcessor processor) {
    int range = max - min;
    int newRange = high - low;
    
    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        int value = processor.getPixel(x, y);
        int newValue = min + (value - low) * (range / newRange);
        processor.putPixel(x, y, newValue);
      }
    }
  }

  public void equalizeHistogram(int max, int[] histogram, ImageProcessor processor) {
    float MN = this.width * this.height;
    int[] newHistogram = new int[256];
    float probabilitySum = 0;

    for(int i = 0; i < 256; i++){
      probabilitySum += ((histogram[i]) / MN);
      newHistogram[i] = Math.round((max * probabilitySum));
    }
    
    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        int value = processor.getPixel(x, y);
        int newValue = newHistogram[value];
        processor.putPixel(x, y, newValue);
      }
    }
  }
}

