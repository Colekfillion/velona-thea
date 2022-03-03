from ca.quadrexium.velonathea.python import ImageResult, ResultContainer

author_keywords = ["member_name", "creator", "author_name", "company", "twitter_user_handle"]
title_keywords = ["title", "eng_name", "material", "source"]
supported_hosts = {
    # saucenao index id: hostname
    9: "danbooru.donmai.us",
    12: "yande.re",
    25: "gelbooru.com",
    34: "deviantart.com",
    40: "www.furaffinity.net"
}


def parse_results(results, response_header, minimum_similarity):
    image_results = []
    for result in results:
        data = result.get('data')
        header = result.get('header')

        similarity = float(header.get('similarity'))

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
        im = ImageResult(
            "" if title is None or title == "" else title,
            "unknown" if author is None or author == "" else author,
            similarity,
            sources,
            int(index_id)
        )
        image_results.append(im)

    scores = []
    # Sorts through results and reorders them based on a score
    for image_result in image_results:
        score = 0
        # Remove the result if it is not above the min similarity
        if image_result.getSimilarity() < minimum_similarity:
            image_results.remove(image_result)
            continue
        # The result with the most data is given the highest score
        if len(image_result.getSources()) != 0:
            score += 1
        if image_result.getTitle() != "":
            score += 1
        if image_result.getAuthor() != "":
            score += 1
        if image_result.getIndexId() in supported_hosts.keys():
            score += 1
        # Add tupule to score array that contains the score and the result
        scores.append((score, image_result))
    # Sorts the results in descending order based on its paired score
    scores.sort(reverse=True, key=get_score)
    output = []
    # Just get the results
    for score in scores:
        output.append(score[1])
    image_results = output
    return ResultContainer(image_results,
                           int(response_header.get('short_remaining')),
                           int(response_header.get('long_remaining')))


def get_score(element):
    return element[0]
