import serenade.config


def intermediate_path(*args):
    return serenade.config.library_path("lm", "model-data", *args)


def output_path(*args):
    return serenade.config.library_path("lm", "model", *args)
