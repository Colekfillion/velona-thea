import requests
from urllib.parse import urlparse

from metadata_getter import metadata_request

supported_hosts = {
    # saucenao index id: hostname
    9: "danbooru.donmai.us",
    12: "yande.re",
    25: "gelbooru.com",
    34: "deviantart.com",
    40: "www.furaffinity.net"
}

def get_metadata(file, api_key, minimum_similarity):
    try:
        minimum_similarity = float(minimum_similarity)
    except ValueError:
        return -1, "Minsim must be number"

    return_value = metadata_request(file, api_key, minimum_similarity)
    if return_value[0] == 1:
        # make sure that the image has results before searching for tags
        if len(return_value[1].getImageResults()) > 0:
            print(type(return_value[1].getImageResults()[0]))
            tags = get_tags(return_value[1].getImageResults()[0].getSources()[0])
            if isinstance(tags, tuple):
                if tags[0] == -2:
                    return -2, tags[1], "Error fetching tags: " + tags[2]
                elif tags[0] == -3:
                    return return_value
            tags_list = tags.split(" ")
            for img in return_value[1].getImageResults():
                img.setTags(tags_list)

    return return_value


def get_tags(url):
    tags = ""
    source = urlparse(url).hostname
    if source not in supported_hosts.values():
        return -3, "Unsupported host " + source
    try:
        response = requests.get(url)
        if response.status_code == 403:
            return -2, 60*40, "Too many requests to " + source
    except requests.exceptions.ConnectionError:
        return -2, 30, "Connection refused"
    if response.status_code == 200:
        page = response.content.decode('utf-8')
        try:
            if source == 'danbooru.donmai.us':
                tag_head = "data-tags=\""
                if tag_head not in page:
                    return -3, "Could not find tags from " + url
                start_index = page.find(tag_head) + len(tag_head)
                tags = page[start_index:page.find("\"", start_index)]
            elif source == 'gelbooru.com':
                tag_head = "data-tags=\""
                if tag_head not in page:
                    return -3, "Could not find tags from " + url
                start_index = page.find(tag_head) + len(tag_head)+1
                tags = page[start_index:page.find("\"", start_index)-1]
            elif source == 'yande.re':
                page = page[page.find("<div id=\"note-container\">"):]
                tag_head = "<img alt=\""
                if tag_head not in page:
                    return -3, "Could not find tags from " + url
                start_index = page.find(tag_head) + len(tag_head)
                tags = page[start_index:page.find("\"", start_index)]
            elif source == 'deviantart.com':
                page = page[page.find("<div class=\"_3UK_f\">"):]
                tag_head = "<span class=\"_2ohCe\">"
                if tag_head not in page:
                    return -3, "Could not find tags from " + url
                tag_foot = "</span>"
                num_tags = page.count(tag_head)
                tags = ""
                for num in range(num_tags):
                    tag = page[page.find(tag_head)+len(tag_head):page.find(tag_foot)]
                    page = page.replace(tag_head + tag + tag_foot, "")
                    tags += tag + " "
                tags = tags[0:len(tags)-1]
            elif source == 'www.furaffinity.net':
                page = page[page.find("<section class=\"tags-row\">"):]
                page = page[:page.find("</section>")]
                page = page.replace("<span class=\"tags\">", "")
                page = page.replace("</span>", "")
                tag_head = "\">"
                tag_foot = "</a>"
                page = page[:page.rindex(tag_foot)+len(tag_foot)]
                num_tags = page.count(tag_foot)
                tags = ""
                for num in range(num_tags):
                    tag = page[page.rindex(tag_head) + len(tag_head):page.rindex(tag_foot)]
                    page = page.replace(tag_head + tag + tag_foot, "")
                    tags += tag + " "
                tags = tags[0:len(tags) - 1]
        except Exception:
            return -3, "Could not find tags from " + url
        if tags != "" and tags is not None:
            return tags
        return -3, "Page has no tags, " + url
    elif response.status_code == 404:
        return -3, "Could not find tags from " + url
    elif response.status_code == 502:
        return -2, 30, "502 Bad Gateway"
    elif response.status_code != 404:
        return -1, "Error, response " + str(response.status_code)