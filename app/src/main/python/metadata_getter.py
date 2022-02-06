import io
import requests
from PIL import Image

from exceptions import LongLimitException, RetryException, FatalException
from result_wrapper import parse_results

Image.MAX_IMAGE_PIXELS = None


def get_score(element):
    return element[0]


class MetadataGetter:

    def __init__(self, api_key, config, supported_hosts):
        self.api_key = api_key
        self.minimum_similarity = config.get("minimum_similarity")
        self.compress_checked = config.get("compress_checked")
        self.supported_hosts = supported_hosts

    def get_metadata(self, image):
        try:
            return self.saucenao_request(image)
        except Exception as e:
            raise e

    # Requests image data from saucenao
    def saucenao_request(self, image):
        params = dict()
        if self.api_key is not None:
            params['api_key'] = self.api_key
        params['output_type'] = 2  # json
        params['numres'] = 6  # number of results to return
        params['db'] = 999  # from all dbs

        image = image.convert('RGB')
        image_data = io.BytesIO()
        if self.compress_checked:
            image.thumbnail((250, 250), resample=Image.ANTIALIAS)
        image.save(image_data, format='PNG')
        files = {'file': ("image.png", image_data.getvalue())}
        image_data.close()

        resp = requests.post('https://saucenao.com/search.php', params=params, files=files)
        if resp.status_code == 200:
            resp = resp.json()

            status = resp.get('header').get('status')
            if status > 0:
                raise RetryException((60*10), "SauceNAO error: status " + str(status) + " (server error). ")
            elif status < 0:
                message = resp.get('header').get('message')
                message.replace("</a>", "")
                message.replace("<br />", " ")
                raise FatalException("SauceNAO error: status " + str(status) + " (client error) " + message)
            elif status == 0:
                results = resp.get('results')
                # Parse results into a ResultContainer that contains the image results
                parsed_results = parse_results(results, resp.get('header'))
                return parsed_results

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
                raise LongLimitException('Daily request limit reached. ')
            elif 'failed' in message:
                raise RetryException(5*60, message)
            else:
                raise RetryException(25, "Short limit reached. ")
        else:
            raise RetryException(30, "Cannot connect to SauceNAO - response " + str(resp.status_code))

    # Sorts image results in order of most data
    def sort_results(self, result):
        scores = []
        # Sorts through results and reorders them based on a score
        for image_result in result.image_results:
            score = 0
            # Remove the result if it is not above the min similarity
            if image_result.similarity < self.minimum_similarity:
                result.image_results.remove(image_result)
                continue
            # The result with the most data is given the highest score
            if len(image_result.sources) != 0:
                score += 1
            if image_result.title != "":
                score += 1
            if image_result.author != "":
                score += 1
            if image_result.index_id in self.supported_hosts.keys():
                score += 1
            # Add tupule to score array that contains the score and the result
            scores.append((score, image_result))
        # Sorts the results in descending order based on its paired score
        scores.sort(reverse=True, key=get_score)
        output = []
        # Just get the results
        for score in scores:
            output.append(score[1])
        result.image_results = output
        return result
