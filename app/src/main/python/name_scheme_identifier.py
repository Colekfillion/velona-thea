import re


possible_schemes = {
    "imagesearch": "(.*?)_imgserch$",
    "pixiv": "[0-9]+_p[0-9]+",
    "deviantart": "(.*?)+_by_(.*?)+_(.*?)+"
}


def identify_naming_scheme(name):
    # loop through possible_schemes and compare them to the name
    for scheme in possible_schemes:
        pattern = re.compile(possible_schemes.get(scheme))
        if pattern.match(name):
            return scheme
    return "unknown"
