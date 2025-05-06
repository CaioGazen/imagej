import cv2
import numpy as np
import os
import time

input_folder = "src/"
output_folder = "output/"

target_max_dim = 300  # value for resizing the image to speed up GrabCut

# --- Create output folder if it doesn't exist ---
os.makedirs(output_folder, exist_ok=True)
print(f"Output folder '{output_folder}' ensured.")


# ---  list all images
# Defining image extensions to look for
image_extensions = [".jpg", ".jpeg", ".png", ".bmp", ".tiff"]
image_files = [
    f
    for f in os.listdir(input_folder)
    if os.path.splitext(f)[1].lower() in image_extensions
]

if not image_files:
    print(
        f"No image files found with extensions {image_extensions} in '{input_folder}'. Exiting."
    )
    exit()


print(f"Found {len(image_files)} images to process.")

n_images = len(image_files)
n_images_processed = 0
start_time = time.time()

# --- Process each image in the folder ---
for image_filename in image_files:
    full_image_path = os.path.join(input_folder, image_filename)
    print(f"\nProcessing image: {image_filename}")

    # --- Load image ---
    imagem_original = cv2.imread(full_image_path)
    if imagem_original is None:
        print(f"Warning: Could not read image '{image_filename}'. Skipping.")
        continue

    original_height, original_width = imagem_original.shape[:2]
    max_original_dim = max(original_height, original_width)

    # --- Determine Scaling Factor and Scale Down Image if needed ---
    scale_factor = 1.0  # Default is no scaling
    imagem_scaled = imagem_original  # Start assuming no scaling

    if max_original_dim > target_max_dim:
        scale_factor = target_max_dim / max_original_dim
        scaled_width = int(original_width * scale_factor)
        scaled_height = int(original_height * scale_factor)

        print(f"  Original size: ({original_width}x{original_height})")
        print(
            f"  Scaling down by factor {scale_factor:.2f} to ({scaled_width}x{scaled_height})"
        )
        if scaled_width <= 0 or scaled_height <= 0:
            print(
                f"Warning: Calculated scaled dimensions were zero or negative for '{image_filename}'. Skipping scaling."
            )
            # Keep scale_factor as 1.0 if scaling would result in invalid dimensions
            scale_factor = 1.0
            scaled_width = original_width
            scaled_height = original_height
        else:
            imagem_scaled = cv2.resize(
                imagem_original,
                (scaled_width, scaled_height),
                interpolation=cv2.INTER_AREA,
            )
            print(
                f"  Scaled down by factor {scale_factor:.2f} to ({scaled_width}x{scaled_height}) for GrabCut."
            )
    else:
        scaled_width = original_width
        scaled_height = original_height
        print(
            f"  Image size ({original_width}x{original_height}) is within target, no scaling for GrabCut."
        )

    # --- Run GrabCut on the potentially scaled-down image ---
    # Define the initial rectangle on the scaled image
    # Assuming the car is centered, define a rectangle covering the central area
    # Adjust these percentages (0.05 border) based on how much border is usually around the centered car
    border_percentage = 0.05
    rect_scaled = (
        int(scaled_width * border_percentage),  # x
        int(scaled_height * border_percentage),  # y
        int(scaled_width * (1 - 2 * border_percentage)),  # width of rect
        int(scaled_height * (1 - 2 * border_percentage)),  # height of rect
    )

    # Ensure the rectangle is valid (minimum 1x1 dimensions)
    if rect_scaled[2] <= 0 or rect_scaled[3] <= 0:
        print(
            f"  Warning: Calculated GrabCut rectangle is invalid ({rect_scaled}) for '{image_filename}'. Cannot run GrabCut."
        )
        continue
    else:
        # Initialize mask, background and foreground models for GrabCut
        mask_scaled = np.zeros(
            imagem_scaled.shape[:2], np.uint8
        )  # Mask initialized to zeros
        bgdModel = np.zeros((1, 65), np.float64)  # Background model
        fgdModel = np.zeros((1, 65), np.float64)  # Foreground model

        # Run GrabCut with the initial rectangle on the scaled image
        try:
            # You might need more iterations depending on the image complexity
            cv2.grabCut(
                imagem_scaled,
                mask_scaled,
                rect_scaled,
                bgdModel,
                fgdModel,
                5,
                cv2.GC_INIT_WITH_RECT,
            )

            # --- Process the GrabCut mask ---
            # Create a binary mask where 255 represents the foreground (GC_FGD or GC_PR_FGD)
            # GrabCut labels: 0=GC_BGD, 1=GC_FGD, 2=GC_PR_BGD, 3=GC_PR_FGD
            # We want labels 1 (definite foreground) and 3 (probable foreground)
            grabcut_mask_scaled_binary = np.where(
                (mask_scaled == cv2.GC_FGD) | (mask_scaled == cv2.GC_PR_FGD), 255, 0
            ).astype("uint8")

            # --- Resize the mask back to the original image size ---
            # Use nearest neighbor interpolation to keep the mask binary (0 or 255)
            grabcut_mask_original_size = cv2.resize(
                grabcut_mask_scaled_binary,
                (original_width, original_height),
                interpolation=cv2.INTER_NEAREST,
            )

        except cv2.error as e:
            print(
                f"  CV2 Error running GrabCut on scaled image for '{image_filename}': {e}"
            )
            continue

        except Exception as e:  # Catch other potential errors
            print(
                f"  An unexpected error occurred during GrabCut for '{image_filename}': {e}"
            )

            continue

    # 1. Create an all-white image of the same size as the original
    white_background = np.full(imagem_original.shape, 255, dtype=imagem_original.dtype)

    output_image = white_background  # Start with the white background
    cv2.copyTo(imagem_original, grabcut_mask_original_size, output_image)

    # --- Save the resulting image ---
    # Get the base name of the image file without extension
    base_filename = os.path.splitext(image_filename)[0]
    # Save the masked image. Using a descriptive suffix.
    output_masked_filename = f"{base_filename}_masked.png"
    output_masked_path = os.path.join(output_folder, output_masked_filename)

    # Using imwrite with a 3-channel image and a mask is possible for saving with alpha channel (for transparency)
    cv2.imwrite(output_masked_path, output_image)
    print(f"  Saved masked image to '{output_masked_path}'")

    n_images_processed += 1

    now = time.time()

    print(
        f"Processed {n_images_processed}/{n_images} images. {n_images_processed/n_images}% Elapsed time: {now - start_time:.2f} seconds  ETA: {(now - start_time) / n_images_processed * (n_images - n_images_processed):.2f} seconds"
    )
