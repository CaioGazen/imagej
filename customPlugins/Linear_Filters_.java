import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.gui.*;
import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Linear_Filters_ implements PlugIn {
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
    String[] items = {"LowPass mean", "highPass","border south"};
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


    switch (answer) {
      case "LowPass mean":
        int[][] kernel = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
        newProcessor = applyKernel(newProcessor, kernel, 9);
        break;

      case "highPass":
        int[][] kernelhigh = {{0, -1, 0},
                             {-1, 5, -1},
                             {0, -1, 0}};
        newProcessor = applyKernel(newProcessor, kernelhigh, 1);
        break;

      case "border south":
        int[][] kernelSouth = {{-1, -1, -1},
                               {1, -2, 1},
                               {1, 1, 1}};
        newProcessor = applyKernel(newProcessor, kernelSouth, 1);
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

  public ImageProcessor applyKernel(ImageProcessor processor, int[][] kernel, int kernelDivisor){
    int kernelOffset = ((kernel.length - 1) / 2);
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);
    for(int y = kernelOffset; y < this.height - kernelOffset; y++){
      for(int x = kernelOffset; x < this.width - kernelOffset; x++){
        int sum = 0;
        for(int ky = -kernelOffset; ky <= kernelOffset; ky++){
          for(int kx = -kernelOffset; kx <= kernelOffset; kx++){
            int pixel = processor.getPixel(x + kx, y + ky);
            sum += pixel * kernel[ky + kernelOffset][kx + kernelOffset];
          }
        }
        newProcessor.putPixel(x, y, sum / kernelDivisor);
      }
    }

    return newProcessor;
  }

  }

