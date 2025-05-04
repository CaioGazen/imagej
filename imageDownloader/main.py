import requests
from bs4 import BeautifulSoup
import os
from urllib.parse import urljoin, urlparse
import re  # For potential filename sanitization

# --- Configuration ---
target_urls = [
    "https://hotwheels.fandom.com/wiki/List_of_" + str(i) + "_Hot_Wheels"
    for i in range(1968, 2026)
]
download_folder = "downloaded_relevant_images"
target_attribute = "data-relevant"
target_values = ["0", "1"]  # <-- UPDATED: List of allowed values
# Use a User-Agent header to mimic a browser, some sites block simple scripts
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
}

# --- End Configuration ---

print(target_urls)


def sanitize_filename(filename):
    """Removes characters potentially invalid for filenames."""
    sanitized = re.sub(r"[^\w\.\-]", "_", filename)
    sanitized = re.sub(r"_+", "_", sanitized)
    sanitized = sanitized.strip("_.")
    if not sanitized:
        # Use hash of original filename for uniqueness if sanitization removes everything
        # Convert to string first in case filename is None or unexpected type
        return f"image_{hash(str(filename))}"
    return sanitized


def download_images_with_tag(url, folder, attribute_name, allowed_attribute_values):
    """
    Downloads images from a SINGLE URL that have a specific data attribute
    with one of the allowed values.

    Args:
        url (str): The URL of the webpage to scrape.
        folder (str): The directory to save downloaded images.
        attribute_name (str): The name of the HTML attribute to filter by.
        allowed_attribute_values (list): A list of strings representing the
                                         required values of the HTML attribute.
    """
    print("-" * 60)  # Separator for different URLs
    print(f"Processing URL: {url}")
    print("-" * 60)

    if not url or not url.startswith(("http://", "https://")):
        print(f"Skipping invalid URL format: {url}")
        return  # Stop processing this specific URL

    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        print(f"Successfully fetched URL: {url}")
    except requests.exceptions.Timeout:
        print(f"Error: Request timed out for {url}")
        return  # Stop processing this specific URL
    except requests.exceptions.RequestException as e:
        print(f"Error fetching URL {url}: {e}")
        return  # Stop processing this specific URL

    soup = BeautifulSoup(response.text, "html.parser")

    # Create the download directory if it doesn't exist (safe to call multiple times)
    if not os.path.exists(folder):
        try:
            os.makedirs(folder)
            print(f"Created directory: {folder}")
        except OSError as e:
            # Handle potential race condition if another process creates it
            if not os.path.isdir(folder):
                print(f"Error creating directory {folder}: {e}")
                return  # Stop processing this specific URL if dir can't be made

    image_tags = soup.find_all("img")
    print(f"Found {len(image_tags)} total image tags on the page.")

    download_count = 0
    skipped_count = 0
    failed_count = 0

    for img in image_tags:
        if (
            img.has_attr(attribute_name)
            and img.get(attribute_name) in allowed_attribute_values
        ):
            img_url_relative = None
            if img.has_attr("data-src"):
                img_url_relative = img.get("data-src")
            elif img.has_attr("src"):
                img_url_relative = img.get("src")

            if not img_url_relative:
                # print(f"Skipping relevant image tag without 'src' or 'data-src': {img}") # Less verbose
                skipped_count += 1
                continue

            if img_url_relative.startswith("data:"):
                # print(f"Skipping data URL: {img_url_relative[:60]}...") # Less verbose
                skipped_count += 1
                continue

            try:
                img_url_absolute = urljoin(url, img_url_relative)

                # Ensure the joined URL is also valid before proceeding
                if not urlparse(img_url_absolute).scheme in ["http", "https"]:
                    print(f"Skipping invalid absolute URL scheme: {img_url_absolute}")
                    skipped_count += 1
                    continue

                filename = img.get("data-image-name")
                if not filename:
                    parsed_url = urlparse(img_url_absolute)
                    # Decode URL-encoded characters in path for filename
                    filename_path = requests.utils.unquote(parsed_url.path)
                    filename = os.path.basename(filename_path)
                if not filename:
                    filename = (
                        f"image_{download_count + failed_count + skipped_count + 1}"
                    )
                    if "." in img_url_absolute:
                        # Extract extension more robustly
                        path_part = urlparse(img_url_absolute).path
                        if "." in path_part:
                            ext = path_part.rsplit(".", 1)[-1]
                            # Basic check for common image extensions (case-insensitive)
                            if ext.lower() in [
                                "jpg",
                                "jpeg",
                                "png",
                                "gif",
                                "webp",
                                "svg",
                                "bmp",
                                "tiff",
                            ]:
                                filename += f".{ext}"

                safe_filename = sanitize_filename(filename)
                # Add check for empty filename AFTER sanitization
                if not safe_filename:
                    safe_filename = f"image_{hash(filename or img_url_absolute)}"

                filepath = os.path.join(folder, safe_filename)

                if os.path.exists(filepath):
                    # print(f"Skipping (already exists): {safe_filename}") # Less verbose
                    skipped_count += 1
                    continue

                # print(f"Downloading '{safe_filename}' from {img_url_absolute}...") # Less verbose
                img_response = requests.get(
                    img_url_absolute, headers=headers, stream=True, timeout=30
                )
                img_response.raise_for_status()

                # Optional: Check content type if needed, though raise_for_status often suffices
                # content_type = img_response.headers.get('content-type')
                # if content_type and not content_type.startswith('image'):
                #     print(f"Skipping non-image content at {img_url_absolute} (type: {content_type})")
                #     img_response.close() # Close the connection
                #     failed_count += 1
                #     continue

                with open(filepath, "wb") as f:
                    for chunk in img_response.iter_content(chunk_size=8192):
                        f.write(chunk)
                download_count += 1

            except requests.exceptions.MissingSchema:
                # This case might be caught earlier by the urljoin/scheme check now
                print(
                    f"Error: Invalid URL scheme for image '{img_url_absolute}'. Skipping."
                )
                failed_count += 1
            except requests.exceptions.Timeout:
                print(f"Error: Request timed out for image {img_url_absolute}")
                failed_count += 1
            except requests.exceptions.RequestException as e:
                if "No connection adapters were found for" in str(e):
                    print(
                        f"Error: Cannot handle URL scheme for image '{img_url_absolute}'. Skipping."
                    )
                else:
                    print(f"Error downloading image {img_url_absolute}: {e}")
                failed_count += 1
            except IOError as e:
                print(f"Error saving file {safe_filename}: {e}")
                failed_count += 1
            except Exception as e:
                print(
                    f"An unexpected error occurred for image source '{img_url_relative}': {e}"
                )
                failed_count += 1

    # Summary for the current URL
    print(f"\n--- Summary for {url} ---")
    print(f"Successfully downloaded: {download_count} images.")
    print(
        f"Skipped (e.g., data URL, already exists, no src, wrong attr value): {skipped_count} images."
    )
    print(f"Failed to download/save: {failed_count} images.")
    print(f"Images saved to folder: '{folder}'")
    print("-" * 60 + "\n")


# --- Main Execution ---
if __name__ == "__main__":
    # Check if the list is empty or still contains placeholders
    if not target_urls or all("YOUR_" in url for url in target_urls):
        print(
            "Please update the 'target_urls' list in the script with valid web page URLs."
        )
    else:
        print(f"Starting image download process for {len(target_urls)} URL(s)...")
        print(f"Images will be saved to: {download_folder}")

        # Loop through each URL in the list
        for current_url in target_urls:
            # Clean up potential whitespace around URL
            url_to_process = current_url.strip()
            if url_to_process:  # Process only if not empty after stripping
                download_images_with_tag(
                    url_to_process, download_folder, target_attribute, target_values
                )
            else:
                print("Skipping empty URL entry in the list.")

        print("Finished processing all URLs.")
