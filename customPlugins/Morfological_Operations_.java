import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.gui.*;
import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Morfological_Operations_ implements PlugIn {
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
    String[] items = { "Dilate", "Erode", "Opening", "Closing" ,"Border"};
    gd.addRadioButtonGroup("Test", items, 3, 1, "0");
    gd.addCheckbox("Create new image", true);
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

    int[][] dilateElement = { { 1, 1, 1 , 1},{ 1, 1, 1 , 1},{ 1, 1, 1 , 1},{ 1, 1, 1 , 1} };

    int[][] erodeElement = { { 0, 1, 0 }, { 1, 1, 1 }, { 0, 1, 0 } };

    switch (answer) {
      case "Dilate":
        newProcessor = Dilate(newProcessor, dilateElement);
        break;

      case "Erode":
        newProcessor = Erode(newProcessor, erodeElement);
        break;

      case "Opening":
        newProcessor = Erode(newProcessor, erodeElement);
        newProcessor = Dilate(newProcessor, dilateElement);
        break;

      case "Closing":
        newProcessor = Dilate(newProcessor, dilateElement);
        newProcessor = Erode(newProcessor, erodeElement);
        break;

      case "Border":
        newProcessor = Border(newProcessor, erodeElement);
        break;

      default:
        break;
    }

    if (createNewImage) {
      ImagePlus image_new = new ImagePlus(answer, newProcessor);
      new ImageWindow(image_new);
    } else {
      image.setProcessor(newProcessor);
      image.updateAndDraw();
    }

  }

  public ImageProcessor Dilate(ImageProcessor processor, int[][] structuringElement) {
    int structuringElementOffset = ((structuringElement.length - 1) / 2);
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);
    for (int y = structuringElementOffset; y < this.height - structuringElementOffset; y++) {
      for (int x = structuringElementOffset; x < this.width - structuringElementOffset; x++) {
        int pixel = processor.getPixel(x + structuringElementOffset, y + structuringElementOffset);
        if (pixel == 255) {
          for (int ky = -structuringElementOffset; ky <= structuringElementOffset; ky++) {
            for (int kx = -structuringElementOffset; kx <= structuringElementOffset; kx++) {
              newProcessor.putPixel(x + kx, y + ky,
                  (structuringElement[ky + structuringElementOffset][kx + structuringElementOffset]) * 255);
            }
          }
        }
      }
    }

    return newProcessor;
  }

  public int structuringElementSum(int[][] structuringElement) {
    int sum = 0;
    for (int i = 0; i < structuringElement.length; i++) {
      for (int j = 0; j < structuringElement[i].length; j++) {
        sum += structuringElement[i][j];
      }
    }
    return sum;
  }

  public ImageProcessor Erode(ImageProcessor processor, int[][] structuringElement) {
    int structuringElementOffset = ((structuringElement.length - 1) / 2);
    int structuringElementSum = structuringElementSum(structuringElement);
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);
    for (int y = structuringElementOffset; y < this.height - structuringElementOffset; y++) {
      for (int x = structuringElementOffset; x < this.width - structuringElementOffset; x++) {
        int sum = 0;
        for (int ky = -structuringElementOffset; ky <= structuringElementOffset; ky++) {
          for (int kx = -structuringElementOffset; kx <= structuringElementOffset; kx++) {
            int pixel = processor.getPixel(x + kx, y + ky);
            sum += (pixel * structuringElement[ky + structuringElementOffset][kx + structuringElementOffset]) /
                255;
          }
        }

        if (sum == structuringElementSum) {
          newProcessor.putPixel(x, y, 255);
        }
      }
    }

    return newProcessor;
  }

  public ImageProcessor Border(ImageProcessor processor, int[][] structuringElement) {
    ImageProcessor erodedProcessor = Erode(processor, structuringElement);
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);

    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        int originalValue = processor.getPixel(x, y);
        int erodedValue = erodedProcessor.getPixel(x, y);
        newProcessor.putPixel(x, y, originalValue - erodedValue);
      }
    }

    return newProcessor;
  }

}
