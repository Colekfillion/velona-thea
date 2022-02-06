

author_keywords = ["member_name", "creator", "author_name", "company", "twitter_user_handle"]
title_keywords = ["title", "eng_name", "material", "source"]


class ResultContainer:
    def __init__(self, image_results, short_remaining, long_remaining):
        self.image_results = image_results
        self.short_remaining = float(short_remaining)
        self.long_remaining = float(long_remaining)


class ImageResult:
    def __init__(self, data):
        self.title = data.get('title')
        self.author = data.get('author')
        self.similarity = float(data.get('similarity'))
        self.sources = data.get('sources')
        self.index_id = data.get('index_id')


def parse_results(results, response_header):
    image_results = []
    for result in results:
        data = result.get('data')
        header = result.get('header')

        similarity = header.get('similarity')

        # Get author
        author = ""
        for author_keyword in author_keywords:
            if data.get(author_keyword) is not None:
                author = data.get(author_keyword)
                if isinstance(author, list):
                    author = author[0]
                if "banned artist, " in author:
                    author.replace("banned artist, ", "")
                break

        # Get title
        title = ""
        for title_keyword in title_keywords:
            if data.get(title_keyword) is not None:
                title = data.get(title_keyword)
                break

        # Get image sources
        sources = []
        if 'ext_urls' in data:
            for url in data.get('ext_urls'):
                sources.append(url)

        # Get index ID
        index_id = header.get('index_id')

        # Add image result to list
        im = ImageResult({
            'title': "" if title is None or title == "" else title,
            'author': "unknown" if author is None or author == "" else author,
            'similarity': similarity,
            'sources': sources,
            'index_id': index_id
        })
        image_results.append(im)
    return ResultContainer(image_results,
                           response_header.get('short_remaining'),
                           response_header.get('long_remaining'))
