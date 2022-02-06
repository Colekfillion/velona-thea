import os
import threading
import time
from PIL import Image, ImageFile
from datetime import datetime
from shutil import copy

import metadata_getter
import web_scraper
from exceptions import UnsupportedHostException, RetryException, TagsNotFoundException, \
    NoTagsException, LongLimitException, FatalException

# By default PIL will not load images if they are missing some trailing bytes.
#  Explicitly tell PIL that this is OK.
ImageFile.LOAD_TRUNCATED_IMAGES = True

supported_hosts = {
    # saucenao index id: hostname
    9: "danbooru.donmai.us",
    12: "yande.re",
    25: "gelbooru.com",
    34: "deviantart.com",
    40: "www.furaffinity.net"
}

# Default values
running = False

extensions = [".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"]

# Handles starting and stopping of execute function
def execute_button(config):
    # Make sure execute function is not running
    global running
    if not running:
        running = True
        th = threading.Thread(target=lambda: execute(config))
        th.start()
    else:
        set_log("Stop requested...")
        running = False


# Changes the log text to the provided message
def set_log(message):
    print("[" + datetime.now().strftime("%H:%M:%S") + "]: " + message)


# Handles sleep time so user can quit when the program is waiting until it can make a new request
def retry_handler(e):
    # Formatting the message, if wait time is longer than 120 seconds display it in minutes
    time_key = "seconds"
    if e.sleep_time >= 120:
        time_key = "minutes"
    set_log(e.message + "Sleeping for " + (
        str(e.sleep_time / 60) if time_key == "minutes" else str(e.sleep_time)) +
            " " + time_key + "...")
    # Sleeps in increments of 5 seconds, after each increment check if a quit
    #  has been requested
    for num in range(round(e.sleep_time / 5)):
        time.sleep(5)
        if not running:
            break


change config to take its values.. minsim, input dir, etc
def execute(config):
    try:
        # Validating input of minimum_similarity
        config.update({
            "minimum_similarity": float(config.get("minimum_similarity"))
                       })
    except ValueError:
        # Quit
        return

    mdg = metadata_getter.MetadataGetter(config.get("api_key"), config, supported_hosts)
    ws = web_scraper.WebScraper(supported_hosts)

    files_processed = 0
    result = ""
    message = ""
    images_dir = config.get("images_directory")
    for entry in os.scandir(images_dir):
        path = entry.path
        file_name = path.split(images_dir + "\\", 1)[1]

        # If file is an image
        if entry.is_file() and path.endswith(tuple(extensions)):

            image = ""
            try:
                global running
                if not running:
                    break

                image = Image.open(path)
                image_name = file_name[:file_name.rindex('.')]

                # Requesting results
                result = ""
                message = ""
                while result == "" and running:
                    try:
                        result = mdg.get_metadata(image)
                        message += str(len(result.image_results)) + " results for " + file_name
                        break
                    except (LongLimitException, FatalException) as e:
                        message += str(e)
                        image.close()
                        running = False
                        break
                    except RetryException as e:
                        retry_handler(e)
                if result == "":
                    break
                if len(result.image_results) != 0:
                    num_results = len(result.image_results)
                    result = mdg.sort_results(result)
                    num_filtered = num_results - len(result.image_results)
                    if num_filtered > 0:
                        message += ", filtered " + str(num_filtered)
                    if len(result.image_results) == 0:
                        set_log(message + ", no more results")
                        copy(path, images_dir + "\\unknown\\" + file_name)
                        image.close()
                        os.remove(path)
                        files_processed += 1
                        continue

                # Choosing best result
                best = result.image_results[0]

                # Append metadata to file
                image_output_name = image_name if best.title is None or best.title == "" else best.title
                author_output = "unknown" if best.author is None or best.author == "" else best.author
                output_text = file_name + "\t" + image_output_name + "\t" + author_output + "\t"
                # Loops through each source to find tags.
                #  TODO: Loop through each image result's sources for tags, not just the best one.
                if len(best.sources) > 0:
                    # Write source to file
                    url = best.sources[0]
                    output_text += url + "\t"

                    # Creating iterator where each iteration can be repeated, since connections can fail
                    sentinel = object()
                    iterobj = iter(best.sources)
                    x = next(iterobj, sentinel)
                    while x is not sentinel:
                        try:
                            tags = ws.download_tags(x)
                            output_text += tags
                            message += ", got tags successfully"
                            # Found tags, break the loop
                            break
                        except RetryException as e:
                            # Tags not found because of error that may be fixed by
                            #  trying again after a given time period.
                            retry_handler(e)
                            # Continue with this iteration of the loop (redo)
                            continue
                        except (TagsNotFoundException, NoTagsException):
                            # Tags not found and cannot be found with a retry, skip to next iteration
                            message += ", could not find tags "
                            x = next(iterobj, sentinel)
                            continue
                        except UnsupportedHostException as e:
                            message += ", host " + e.host + " not supported"
                            x = next(iterobj, sentinel)
                            continue
                else:
                    output_text += "\t"
                output_text += "\n"
                if result != "":
                    output_file = open(images_dir + "\\" + "output.txt", 'a+', encoding="utf-8")
                    output_file.write(output_text)
                    output_file.close()

                    # Copy image to check directory and mark for deletion in input directory
                    copy(path, images_dir + "\\checked\\" + file_name)
                    image.close()
                    os.remove(path)

                image.close()
            except Exception as e:
                message = str(e)
                copy(path, images_dir + "\\failed\\" + file_name)
                if image != "":
                    image.close()
                os.remove(path)
            set_log(message)
        files_processed += 1
        # Wait if the short limit is reached, don't query and use up a search
        if result != "" and result.short_remaining == 0:
            e = RetryException(25, "Short limit reached. ")
            retry_handler(e)
    message += "Finished executing"
    set_log(message)

