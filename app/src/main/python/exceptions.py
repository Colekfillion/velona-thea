class ShortLimitException(Exception):
    pass


class LongLimitException(Exception):
    pass


class RetryException(Exception):
    def __init__(self, sleep_time, message):
        self.sleep_time = sleep_time
        self.message = message


class TagsNotFoundException(Exception):
    pass


class NoTagsException(Exception):
    pass


class UnsupportedHostException(Exception):
    def __init__(self, host):
        self.host = host


class FatalException(Exception):
    pass
