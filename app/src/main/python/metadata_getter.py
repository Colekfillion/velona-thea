import io
import requests
from PIL import Image, ImageFile, UnidentifiedImageError

from wrappers import parse_results

ImageFile.LOAD_TRUNCATED_IMAGES = True
Image.MAX_IMAGE_PIXELS = None


def metadata_request(file, api_key, minimum_similarity):
    file = io.BytesIO(file)
    params = dict()
    params['api_key'] = api_key
    params['output_type'] = 2  # json
    params['numres'] = 6  # number of results to return
    params['db'] = 999  # from all dbs

    try:
        image = Image.open(file)
    except UnidentifiedImageError as e:
        return -3, "Unknown image format - " + str(e)
    image = image.convert('RGB')
    image_data = io.BytesIO()
    image.thumbnail((250, 250), resample=Image.ANTIALIAS)
    image.save(image_data, format='PNG')
    files = {'file': ("image.png", image_data.getvalue())}
    image_data.close()

    resp = requests.post('https://saucenao.com/search.php', params=params, files=files)
    if resp.status_code == 200:
        resp = resp.json()

        status = resp.get('header').get('status')
        if status > 0:
            return -2, 60*10, "SauceNAO error: status " + str(status) + " (server error). ", 60*10
        elif status < 0:
            message = resp.get('header').get('message')
            message.replace("</a>", "")
            message.replace("<br />", " ")
            return -1, "SauceNAO error: status " + str(status) + " (client error) " + message
        elif status == 0:
            results = resp.get('results')
            # Parse results into a ResultContainer that contains the image results
            parsed_results = parse_results(results, resp.get('header'), minimum_similarity)
            return 1, parsed_results

    elif resp.status_code == 429:
        message = resp.json()['header']['message']
        message = message.replace("<strong>", "").replace("</strong>", "")
        message = message.replace("<br />", " ")
        link_start = "<a href="
        link_end = ">"
        for num in range(message.count(link_start)):
            substring = message[message.find(link_start):message.find(link_end)+len(link_end)]
            message = message.replace(substring, "")
            message = message.replace("</a>", "")
        if 'Daily' in message:
            return -1, 'Daily request limit reached. '
        elif 'failed' in message:
            return -2, 5*60, message
        else:
            return -2, 25, "Short limit reached. Waiting 25 seconds..."
    else:
        return -2, 30, "Cannot connect to SauceNAO - waiting 30 seconds, response " + str(resp.status_code)
