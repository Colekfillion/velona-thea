import requests
from urllib.parse import urlparse

from exceptions import RetryException, TagsNotFoundException, NoTagsException, \
    UnsupportedHostException


class WebScraper:
    def __init__(self, supported_hosts):
        self.supported_hosts = supported_hosts

    def download_tags(self, url):
        source = urlparse(url).hostname
        if source not in self.supported_hosts.values():
            raise UnsupportedHostException(source)
        try:
            response = requests.get(url)
            if response.status_code == 403:
                raise RetryException((60*4), "Too many requests to " + source)
        except requests.exceptions.ConnectionError:
            raise RetryException(30, "Connection refused")
        if response.status_code == 200:
            page = response.content.decode('utf-8')
            tags = ""
            try:
                if source == 'danbooru.donmai.us':
                    tag_head = "data-tags=\""
                    if tag_head not in page:
                        raise TagsNotFoundException("Could not find tags from " + url)
                    start_index = page.find(tag_head) + len(tag_head)
                    tags = page[start_index:page.find("\"", start_index)]
                elif source == 'gelbooru.com':
                    tag_head = "data-tags=\""
                    if tag_head not in page:
                        raise TagsNotFoundException("Could not find tags from " + url)
                    start_index = page.find(tag_head) + len(tag_head)+1
                    tags = page[start_index:page.find("\"", start_index)-1]
                elif source == 'yande.re':
                    page = page[page.find("<div id=\"note-container\">"):]
                    tag_head = "<img alt=\""
                    if tag_head not in page:
                        raise TagsNotFoundException("Could not find tags from " + url)
                    start_index = page.find(tag_head) + len(tag_head)
                    tags = page[start_index:page.find("\"", start_index)]
                elif source == 'deviantart.com':
                    page = page[page.find("<div class=\"_3UK_f\">"):]
                    tag_head = "<span class=\"_2ohCe\">"
                    if tag_head not in page:
                        raise TagsNotFoundException("Could not find tags from " + url)
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
                raise TagsNotFoundException("Could not find tags from " + url)
            if tags != "" and tags is not None:
                return tags
            raise NoTagsException("Page has no tags " + url)
        elif response.status_code == 404:
            raise TagsNotFoundException("Could not find tags from " + url)
        elif response.status_code == 502:
            raise RetryException(30, "502 Bad Gateway")
        elif response.status_code != 404:
            raise Exception("Error, response " + str(response.status_code))
