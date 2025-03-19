import ij.*;
import ij.plugin.PlugIn;
import ij.plugin.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.frame.RoiManager;

import java.util.List;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.io.*;

import java.awt.Color;

public class Roi2_ implements PlugIn {

  public void run(String arg) {

    GenericDialog gd = new GenericDialog("Choose output an input folders");
    gd.addDirectoryField("Input Folder", "imagensArvores/arvoresSrc");
    gd.addDirectoryField("Output Folder", "imagensArvores/arvoresOut");
    gd.showDialog();

    if (gd.wasCanceled()) {
      return;
    }

    String inputFolder = gd.getNextString();
    String outputFolder = gd.getNextString();

    int maxWidth = 0;
    int maxHeight = 0;

    System.out.println("Input Folder: " + inputFolder);

    for (File file : new File(inputFolder).listFiles()) {
      ImagePlus ip = new ImagePlus(file.getAbsolutePath());
      maxWidth = Math.max(maxWidth, ip.getWidth());
      maxHeight = Math.max(maxHeight, ip.getHeight());
    }

    ImagePlus stack = FolderOpener.open(inputFolder, maxWidth, maxHeight, "virtual");
    
    IJ.run(stack, "8-bit", "stack");
    IJ.setAutoThreshold(stack, "Default");
    // IJ.run(stack, "Enhance Contrast...", "saturated=0.35 normalize equalize stack");
    IJ.run(stack, "Convert to Mask", "black stack");
    IJ.run(stack, "Dilate", "stack");
    IJ.run(stack, "Dilate", "stack");
    IJ.run(stack, "Erode", "stack");
    IJ.run(stack, "Fill Holes", "stack");
    IJ.run(stack, "Analyze Particles...", "size=1000-Infinity add stack");


    RoiManager rm = RoiManager.getInstance();

    stack.close();
    
    Roi[] rois = rm.getRoisAsArray();

    ImagePlus originalStack =  FolderOpener.open(inputFolder, maxWidth, maxHeight, "virtual");
    originalStack.cropAndSave(rois, outputFolder, "png");
   
    for (Roi roi : rois) {
      roi.setStrokeColor(Color.BLUE); // Set the stroke color
    }

    originalStack.show();
  }
}
