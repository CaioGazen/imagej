import ij.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.process.LUT;
import ij.gui.*;
import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.awt.Color;

import java.util.LinkedList;
import java.util.Queue;

public class Componentes_Conexos_ implements PlugIn {
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
    gd.addCheckbox("Create new image", true);
    gd.addCheckbox("Apply Custom Lut", true);
    gd.showDialog();

    if (gd.wasCanceled()) {
      return;
    }

    String title = gd.getNextChoice();
    Boolean createNewImage = gd.getNextBoolean();
    Boolean applyCustomLut = gd.getNextBoolean();

    ImagePlus image = WindowManager.getImage(title);

    this.width = image.getWidth();
    this.height = image.getHeight();

    ImageProcessor processor = image.getProcessor();
    ImageProcessor newProcessor = processor.duplicate();

    int[][] structuringElement = { { 0, 1, 0 }, { 1, 0, 1 }, { 0, 1, 0 } };

    newProcessor = LabelComponents(newProcessor, structuringElement);

    if (createNewImage) {
      ImagePlus image_new = new ImagePlus(title + "Componentes Conexos", newProcessor);
      new ImageWindow(image_new);
      if (applyCustomLut) {
        LUT customLut = createCustomLUT();
        image_new.setLut(customLut);
        image_new.updateAndDraw();
      }
    } else {
      image.setProcessor(newProcessor);
      if (applyCustomLut) {
        LUT customLut = createCustomLUT();
        image.setLut(customLut);
      }
      image.updateAndDraw();
    }

  }

  public ImageProcessor LabelComponents(ImageProcessor processor, int[][] structuringElement) {
    int structuringElementOffset = ((structuringElement.length - 1) / 2);
    ImageProcessor newProcessor = new ByteProcessor(this.width, this.height);
    int label = 1;

    Queue<Integer> fifo = new LinkedList<Integer>();

    for (int y = 0; y < this.height; y++) {
      for (int x = 0; x < this.width; x++) {
        int pixel = processor.getPixel(x, y);

        if (pixel == 0 | newProcessor.getPixel(x, y) != 0) {
          continue;
        }

        newProcessor.putPixel(x, y, label);

        fifo.add(x);
        fifo.add(y);

        while (!fifo.isEmpty()) {
          int x1 = fifo.remove();
          int y1 = fifo.remove();

          for (int ky = -structuringElementOffset; ky <= structuringElementOffset; ky++) {
            if (y < 0 | y > this.height) {
              continue;
            }

            for (int kx = -structuringElementOffset; kx <= structuringElementOffset; kx++) {
              if (x < 0 | x > this.width) {
                continue;
              }
              if (structuringElement[ky + structuringElementOffset][kx + structuringElementOffset] == 0) {
                continue;
              }

              int adjacentPixelLabel = newProcessor.getPixel(x1 + kx, y1 + ky);

              if (adjacentPixelLabel != 0) {
                continue;
              }

              int adjacentPixel = processor.getPixel(x1 + kx, y1 + ky);

              if (pixel == adjacentPixel) {
                newProcessor.putPixel(x1 + kx, y1 + ky, label);
                fifo.add(x1 + kx);
                fifo.add(y1 + ky);
              }
            }
          }
        }

        label += 10;
      }
    }

    return newProcessor;
  }

  private LUT createCustomLUT() {
    byte[] reds = new byte[256];
    byte[] greens = new byte[256];
    byte[] blues = new byte[256];

    // Set 0 to black
    reds[0] = 0;
    greens[0] = 0;
    blues[0] = 0;

    // Map values from 1 to 255 to colors
    for (int i = 1; i <= 255; i++) {
      // Normalize the grayscale value to the range [0, 1]
      float normalizedValue = (float) (i - 1) / 253.0f;

      // Map to a hue value (0 to 360)
      float hue = normalizedValue * 360.0f;

      // Set saturation and brightness (value)
      float saturation = 1.0f;
      float brightness = 1.0f;

      // Convert HSV to RGB
      Color rgbColor = Color.getHSBColor(hue / 360.0f, saturation, brightness);

      reds[i] = (byte) rgbColor.getRed();
      greens[i] = (byte) rgbColor.getGreen();
      blues[i] = (byte) rgbColor.getBlue();
    }

    return new LUT(reds, greens, blues);
  }

}
