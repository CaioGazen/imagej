import requests
from bs4 import BeautifulSoup
import os
from urllib.parse import urljoin, urlparse
import re  # For potential filename sanitization
import threading
import queue
import time  # For potential delays and timing


# --- Configuration ---
target_urls = [
    "https://hotwheels.fandom.com/wiki/List_of_" + str(i) + "_Hot_Wheels"
    for i in range(1968, 2026)
]
download_folder = "downloaded_relevant_images"
target_attribute = "data-relevant"
target_values = ["0", "1"]  # List of allowed values
required_parent_classes = ["mw-file-description", "image"]
num_worker_threads = 500  # Adjust based on your network and CPU

# Use a User-Agent header to mimic a browser, some sites block simple scripts
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
}

# --- End Configuration ---

# --- Shared Resources ---
# Queue to hold download tasks: (img_url_absolute, filepath, safe_filename)
download_queue = queue.Queue()
# Lock for safely updating shared counters
counter_lock = threading.Lock()
# Shared counters for the final summary
total_processed_count = 0
total_download_count = 0
total_skipped_count = 0  # Includes already exists, data URLs, invalid schemes
total_failed_count = 0
# --- End Shared Resources ---


def sanitize_filename(filename):
    """Removes characters potentially invalid for filenames."""
    if not filename:  # Handle None or empty string input
        return f"image_{hash(time.time())}"  # Use time hash if filename is None/empty
    sanitized = re.sub(
        r"[^\w\.\-]", "_", str(filename)
    )  # Ensure filename is treated as string
    sanitized = re.sub(r"_+", "_", sanitized)
    sanitized = sanitized.strip("_.")
    if not sanitized:
        # Use hash of original filename if sanitization removes everything
        return f"image_{hash(str(filename))}"
    return sanitized


def find_images_on_page(url, page_num, total_pages):
    """
    Fetches a single page, finds images matching criteria, and queues them
    for download. Updates shared counters for skipped items found here.
    """
    global total_skipped_count  # Allow modification of global counter

    print(f"[Finder {page_num}/{total_pages}] Processing URL: {url}")

    if not url or not url.startswith(("http://", "https://")):
        print(f"[Finder {page_num}/{total_pages}] Skipping invalid URL format: {url}")
        # Increment failed count here? Or maybe a separate "page failed" counter?
        # Let's stick to image-level counts for now.
        return

    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        # print(f"[Finder {page_num}/{total_pages}] Successfully fetched URL: {url}") # Can be verbose
    except requests.exceptions.Timeout:
        print(
            f"[Finder {page_num}/{total_pages}] Error: Request timed out for page {url}"
        )
        return
    except requests.exceptions.RequestException as e:
        print(f"[Finder {page_num}/{total_pages}] Error fetching page {url}: {e}")
        return

    soup = BeautifulSoup(response.text, "html.parser")
    image_tags = soup.find_all("img")
    page_processed_count = 0

    for img in image_tags:
        # 1. Check data-relevant attribute
        if not (
            img.has_attr(target_attribute)
            and img.get(target_attribute) in target_values
        ):
            continue

        # 2. Check parent anchor structure
        parent_anchor = img.find_parent("a")
        parent_classes_ok = False
        if parent_anchor and parent_anchor.has_attr("class"):
            actual_classes = parent_anchor.get("class", [])
            if all(
                req_class in actual_classes for req_class in required_parent_classes
            ):
                parent_classes_ok = True

        # 3. Check if parent anchor exists, has correct classes, and has href
        if not (parent_anchor and parent_classes_ok and parent_anchor.has_attr("href")):
            continue

        # --- Conditions met, prepare download task ---
        page_processed_count += 1
        img_url_relative = parent_anchor.get("href")

        # Skip data: URLs or empty hrefs
        if not img_url_relative or img_url_relative.startswith("data:"):
            # print(f"[Finder {page_num}/{total_pages}] Skipping data URL/empty href.") # Verbose
            with counter_lock:
                total_skipped_count += 1
            continue

        try:
            img_url_absolute = urljoin(url, img_url_relative)

            # Validate scheme
            parsed_absolute_url = urlparse(img_url_absolute)
            if not parsed_absolute_url.scheme in ["http", "https"]:
                print(
                    f"[Finder {page_num}/{total_pages}] Skipping invalid absolute URL scheme: {img_url_absolute}"
                )
                with counter_lock:
                    total_skipped_count += 1
                continue

            # Generate filename
            filename = img.get("data-image-name")
            if not filename:
                filename_path = requests.utils.unquote(parsed_absolute_url.path)
                filename = os.path.basename(filename_path)
            # Add extension guess if filename is still missing/basic
            if not filename or "." not in filename:
                base_name = filename or f"image_{hash(img_url_absolute)}"
                path_part = parsed_absolute_url.path
                ext = ".jpg"  # Default extension
                if "." in path_part:
                    potential_ext = path_part.rsplit(".", 1)[-1]
                    if potential_ext.lower() in [
                        "jpg",
                        "jpeg",
                        "png",
                        "gif",
                        "webp",
                        "svg",
                        "bmp",
                        "tiff",
                    ]:
                        ext = f".{potential_ext}"
                filename = base_name + (ext if not base_name.endswith(ext) else "")

            safe_filename = sanitize_filename(filename)
            if (
                not safe_filename
            ):  # Handle case where sanitization results in empty string
                safe_filename = f"image_{hash(filename or img_url_absolute)}"

            filepath = os.path.join(download_folder, safe_filename)

            # *** Check if file exists BEFORE queuing ***
            if os.path.exists(filepath):
                # print(f"[Finder {page_num}/{total_pages}] Skipping (already exists): {safe_filename}") # Verbose
                with counter_lock:
                    total_skipped_count += 1
                continue

            # --- Queue the download task ---
            download_queue.put((img_url_absolute, filepath, safe_filename))

        except Exception as e:
            # Catch potential errors during URL joining, parsing, filename generation
            print(
                f"[Finder {page_num}/{total_pages}] Error preparing download task for {img_url_relative} on page {url}: {e}"
            )
            with counter_lock:
                total_failed_count += 1  # Count as failed if preparation fails

    print(
        f"[Finder {page_num}/{total_pages}] Found {page_processed_count} potential images matching criteria on {url}."
    )
    # Update total processed count (images matching criteria on page)
    with counter_lock:
        global total_processed_count
        total_processed_count += page_processed_count


def worker():
    """Worker thread function to download images from the queue."""
    global total_download_count, total_failed_count  # Allow modification

    while True:
        try:
            # Get task from queue, wait if necessary
            task = download_queue.get()
            if task is None:
                # Sentinel value received, indicating no more tasks
                break

            img_url_absolute, filepath, safe_filename = task
            # print(f"[Worker {threading.current_thread().name}] Downloading '{safe_filename}' from {img_url_absolute}...") # Verbose

            try:
                img_response = requests.get(
                    img_url_absolute, headers=headers, stream=True, timeout=45
                )  # Increased timeout
                img_response.raise_for_status()

                # Double check folder exists (might be needed if finder runs fast)
                os.makedirs(os.path.dirname(filepath), exist_ok=True)

                with open(filepath, "wb") as f:
                    for chunk in img_response.iter_content(chunk_size=8192):
                        f.write(chunk)

                # --- Success ---
                # print(f"[Worker {threading.current_thread().name}] Successfully downloaded {safe_filename}") # Verbose
                with counter_lock:
                    total_download_count += 1

            # --- Handle Download/Save Errors ---
            except requests.exceptions.Timeout:
                print(
                    f"[Worker {threading.current_thread().name}] Error: Timeout downloading {safe_filename} from {img_url_absolute}"
                )
                with counter_lock:
                    total_failed_count += 1
            except requests.exceptions.RequestException as e:
                print(
                    f"[Worker {threading.current_thread().name}] Error downloading {safe_filename}: {e}"
                )
                with counter_lock:
                    total_failed_count += 1
            except IOError as e:
                print(
                    f"[Worker {threading.current_thread().name}] Error saving file {safe_filename}: {e}"
                )
                with counter_lock:
                    total_failed_count += 1
            except Exception as e:
                print(
                    f"[Worker {threading.current_thread().name}] Unexpected error for {safe_filename}: {e}"
                )
                with counter_lock:
                    total_failed_count += 1
            finally:
                # Mark task as done regardless of success or failure
                download_queue.task_done()

        except queue.Empty:
            # This shouldn't happen with task is None check, but as safeguard
            continue
        except Exception as e:
            # Catch broader errors in worker loop itself
            print(
                f"[Worker {threading.current_thread().name}] Critical worker error: {e}"
            )
            # Ensure task_done is called if possible, though state might be inconsistent
            if "task" in locals() and task is not None:
                download_queue.task_done()


# --- Main Execution ---
if __name__ == "__main__":
    start_time = time.time()

    # --- Preparations ---
    # Validate URLs slightly
    valid_urls = [
        url.strip()
        for url in target_urls
        if url.strip() and url.strip().startswith(("http://", "https://"))
    ]
    invalid_urls_count = len(target_urls) - len(valid_urls)

    if not valid_urls:
        print(
            "Error: No valid URLs provided in 'target_urls' list. Please check the configuration."
        )
        exit()

    if invalid_urls_count > 0:
        print(
            f"Warning: {invalid_urls_count} invalid or empty URL entries were ignored."
        )

    # Create download folder if it doesn't exist
    if not os.path.exists(download_folder):
        try:
            os.makedirs(download_folder)
            print(f"Created download directory: {download_folder}")
        except OSError as e:
            print(f"Error creating directory {download_folder}: {e}. Exiting.")
            exit()
    else:
        print(f"Download directory already exists: {download_folder}")

    print(f"\nStarting image scraping and download process...")
    print(f"Processing {len(valid_urls)} URLs.")
    print(f"Using {num_worker_threads} download worker threads.")
    print("-" * 60)

    # --- Start Worker Threads ---
    threads = []
    for i in range(num_worker_threads):
        thread = threading.Thread(
            target=worker, name=f"Worker-{i+1}", daemon=True
        )  # Daemon threads exit if main exits
        thread.start()
        threads.append(thread)

    # --- Find Images and Queue Tasks (Main Thread) ---
    # This part remains sequential per page to avoid overwhelming servers
    # with page requests, but image downloads happen concurrently.
    for i, url in enumerate(valid_urls):
        find_images_on_page(url, i + 1, len(valid_urls))
        # Optional: Add a small delay between page requests if needed
        # time.sleep(0.1)

    print("-" * 60)
    print("Finished finding images. Waiting for downloads to complete...")

    # --- Signal Workers to Stop ---
    # Block until all tasks are processed
    download_queue.join()
    print("All queued download tasks have been processed.")

    # Stop worker threads by sending sentinel values
    for _ in range(num_worker_threads):
        download_queue.put(None)

    # Wait for all worker threads to finish
    print("Waiting for worker threads to terminate...")
    for thread in threads:
        thread.join()

    print("All worker threads finished.")
    end_time = time.time()
    print("-" * 60)

    # --- Final Summary ---
    print("\n--- Overall Summary ---")
    print(f"Processed {len(valid_urls)} URLs.")
    print(f"Total images matching criteria found: {total_processed_count}")
    print(f"Successfully downloaded: {total_download_count} new images.")
    print(f"Skipped (already exists, data URL, etc.): {total_skipped_count} images.")
    print(f"Failed to download/save: {total_failed_count} images.")
    print(f"Total execution time: {end_time - start_time:.2f} seconds.")
    print(f"Images saved in folder: '{download_folder}'")
    print("-" * 60)
